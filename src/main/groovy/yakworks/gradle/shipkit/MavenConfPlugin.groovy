/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package yakworks.gradle.shipkit

import com.jfrog.bintray.gradle.BintrayExtension
import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.commons.Pogo

/**
 * Setup Bintray and/or Maven
 * Should be applied after JavaPublishPlugin so that can do the maven setups first
 * should only be put on rootprojects
 */
@CompileDynamic
class MavenConfPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(MavenConfPlugin)

    ConfigMap config

    void apply(Project rootProject) {
        ProjectUtil.requireRootProject(rootProject, this.getClass())
        config = rootProject.config

        rootProject.allprojects { Project project ->
            project.plugins.withType(ShippablePlugin) {
                boolean isBintray = config['bintray.enabled']

                if (isBintray) {
                    project.plugins.apply(JavaBintrayPlugin)
                    configBintray(project, config)

                    project.plugins.withId("yakworks.grails-plugin") {
                        configGrailsBintray(project)
                    }
                }

                if (project['isSnapshot'] || !isBintray) {
                    LOG.lifecycle("calling setupMavenPublishRepo because either isSnapshot is true: ${project['isSnapshot']} , or bintray.enabled is false?: $isBintray ")
                    setupMavenPublishRepo(project)
                }

                cleanDepsInPom(project)
                //wireUpDocPublishing(project)
            }
        }
    }

    @CompileDynamic
    void setupMavenPublishRepo(Project project){
        LOG.lifecycle("Set PublishingExtension with URL: ${config['maven.publishUrl']}")
        project.extensions.configure PublishingExtension, new ClosureBackedAction( {
            repositories {
                maven {
                    url config.maven.publishUrl
                    credentials {
                        username config.maven.user
                        password config.maven.key
                    }
                }
            }
        })
    }

    /**
     * Taken from GrailsCentralPublishGradlePlugin in grails-core. its the 'org.grails.grails-plugin-publish'
     * Cleans up dependencies without versions and removes the bom dependencyManagement stuff and adds the grails-plugin.xml artefact
     */
    //FIXME I don't think this is how it should be done as it doesnt include the BOM deps
    @CompileDynamic
    private void cleanDepsInPom(Project project) {
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction( {
                publications {
                    javaLibrary(MavenPublication) {
                        project.plugins.withId("yakworks.grails-plugin") {
                            artifact getGrailsPluginArtifact(project)
                        }
                        pom.withXml {
                            Node pomNode = asNode()
                            if (pomNode.dependencyManagement) {
                                pomNode.dependencyManagement[0].replaceNode {}
                            }
                            pomNode.dependencies.dependency.findAll {
                                it.version.text().isEmpty()
                            }.each {
                                it.replaceNode {}
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * sets up the bintray task defaults
     */
    @CompileDynamic
    private void configBintray(Project project, ConfigMap config) {
        project.afterEvaluate { prj ->
            println "configBintray for ${prj.name}"
            final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension.class)
            config.evalAll() //make sure StringTemplates are evaluated
            bintray.user = config['bintray.user']
            bintray.key = config['bintray.key']

            final BintrayExtension.PackageConfig pkg = bintray.getPkg()
            Pogo.merge(pkg,config['bintray.pkg'])
            Pogo.merge(pkg.version.gpg,config['bintray.pkg.version.gpg'])
            Pogo.merge(pkg.version.mavenCentralSync,config['bintray.pkg.version.mavenCentralSync'])

            pkg.name = prj.name
            pkg.version.name = prj.version
        }
    }

    private void configGrailsBintray(Project project) {
        project.afterEvaluate { prj ->
            final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension.class)
            bintray.pkg.version.attributes = ["grails-plugin": "${prj['group']}:${prj['name']}".toString()]
        }
    }

    @CompileDynamic
    protected Map<String, String> getGrailsPluginArtifact(Project project) {
        def directory
        try {
            directory = project.sourceSets.main.groovy.outputDir
        } catch (Exception e) {
            directory = project.sourceSets.main.output.classesDir
        }
        [source: "${directory}/META-INF/grails-plugin.xml".toString(),
         classifier: "plugin",
         extension: 'xml']
    }
}
