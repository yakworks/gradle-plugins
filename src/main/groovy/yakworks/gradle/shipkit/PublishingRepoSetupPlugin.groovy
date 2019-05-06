/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.commons.Pogo
import yakworks.gradle.ShippablePlugin

/**
 * Finalize setup for Bintray and/or Maven (artifactory) based on isSnapshot and bintray.enabled
 * Should be applied after JavaPublishPlugin so that can do the maven setups first
 * only apply on rootprojects
 */
@CompileDynamic
class PublishingRepoSetupPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(PublishingRepoSetupPlugin)

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
                }

                if (project['isSnapshot'] || !isBintray) {
                    LOG.lifecycle("calling setupMavenPublishRepo because one of the following is true\n" +
                        "isSnapshot = true: ${project['isSnapshot']} , " +
                        "bintray.enabled = false: $isBintray ")
                    setupMavenPublishRepo(project)
                }

            }
        }
    }

    @CompileDynamic
    void setupMavenPublishRepo(Project project){
        LOG.lifecycle("Set PublishingExtension with URL: ${config['maven.publishUrl']}")
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction({
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
    }

    /**
     * sets up the bintray task defaults
     */
    @CompileDynamic
    private void configBintray(Project project, ConfigMap config) {
        project.afterEvaluate { prj ->
            //println "configBintray for ${prj.name}"
            final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension)
            config.evalAll() //make sure StringTemplates are evaluated
            bintray.user = config['bintray.user']
            bintray.key = config['bintray.key']

            final BintrayExtension.PackageConfig pkg = bintray.getPkg()
            Pogo.merge(pkg, config['bintray.pkg'])
            Pogo.merge(pkg.version.gpg, config['bintray.pkg.version.gpg'])
            Pogo.merge(pkg.version.mavenCentralSync, config['bintray.pkg.version.mavenCentralSync'])

            pkg.name = prj.name
            pkg.version.name = prj.version
        }
    }
}
