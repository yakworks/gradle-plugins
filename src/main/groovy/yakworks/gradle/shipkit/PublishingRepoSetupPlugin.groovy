/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileDynamic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.util.ClosureBackedAction

import yakworks.commons.ConfigMap
import yakworks.commons.Pogo

/**
 * Finalize setup for Maven (artifactory) based on isSnapshot
 * Should be applied after JavaPublishPlugin so that can do the maven setups first
 * only apply on rootprojects
 */
@CompileDynamic
class PublishingRepoSetupPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(PublishingRepoSetupPlugin)

    ConfigMap config

    void apply(Project project) {
        // ProjectUtil.requireRootProject(rootProject, this.getClass())
        config = project.rootProject.config

        setupMavenPublishRepo(project)
    }

    @CompileDynamic
    void setupMavenPublishRepo(Project project){
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

}
