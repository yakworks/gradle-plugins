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
package yakworks.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.snapshot.LocalSnapshotPlugin

//@CompileStatic
class GrailsPluginPublishPlugin implements Plugin<Project> {

    void apply(Project project) {
        //minor hack here, load JavaLibraryPlugin first for jar and sources tasks that will then get used in
        //the grails-plugin task. then when JavaBintrayPlugin applies it again its fine.
        project.getPlugins().apply(JavaLibraryPlugin)
        project.getPlugins().apply("org.grails.grails-plugin")
        project.getPlugins().apply(JavaBintrayPlugin)
        cleanDepsInPom(project)
        configBintray(project)

        //setup repo so `gradle publish` works
        if (project.findProperty('isSnapshot') || !project.findProperty('bintrayRepo')) {
            //for snapshots setup the normal repo way, this will enable the 'publish' task
            project.publishing.repositories {
                maven {
                    url project.artifactoryUrl
                    credentials {
                        username project.findProperty("artifactoryUser")
                        password project.findProperty("artifactoryPassword")
                    }
                }
            }
        }
    }

    /**
     * Taken from GrailsCentralPublishGradlePlugin in grails-core. its the 'org.grails.grails-plugin-publish'
     * Cleans up dependencies without versions and removes the bom and adds the grails-plugin.xml artefact
     */
    private void cleanDepsInPom(Project project) {
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction( {
                publications {
                    javaLibrary(MavenPublication) {
                        artifact getDefaultExtraArtifact(project)
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
     * sets up the bintray task defualts
     */
    private void configBintray(Project project) {
        project.afterEvaluate { prj ->
            prj.bintray {
                user = System.getenv("BINTRAY_USER") ?: prj.findProperty("bintrayUser") ?: ''
                key = System.getenv("BINTRAY_KEY") ?: prj.findProperty("bintrayKey") ?: ''
                pkg {
                    repo = prj.bintrayRepo
                    userOrg = prj.bintrayOrg
                    name = prj.name
                    websiteUrl = prj.websiteUrl
                    issueTrackerUrl = prj.githubIssues
                    vcsUrl = prj.githubUrl
                    licenses = prj.hasProperty('license') ? [prj.license] : []
                    publicDownloadNumbers = true
                    version {
                        def artifactType = getDefaultArtifactType()
                        attributes = [(artifactType): "$prj.group:$prj.name"]
                        name = prj.version
                        gpg {
                            sign = false
                            //passphrase = signingPassphrase
                        }
                        mavenCentralSync {
                            sync = false
                        }
                    }
                }
            }
        }
    }

    protected String getDefaultArtifactType() {
        "grails-plugin"
    }

    protected Map<String, String> getDefaultExtraArtifact(Project project) {
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
