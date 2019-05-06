/*
* Copyright 2019. Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.snapshot.LocalSnapshotPlugin
import yakworks.gradle.ShippablePlugin

/**
 * A marker for a grails plugin, "yakworks.grails-plugin", will apply GrailsPluginPublishPlugin later after config
 */
@CompileStatic
class GrailsWebPlugin implements Plugin<Project> {
    private final static Logger log = Logging.getLogger(GrailsWebPlugin)

    void apply(Project project) {
        project.rootProject.plugins.apply(ShipYakRootPlugin)
        project.plugins.apply(ShippablePlugin)

        project.plugins.apply('war')
        project.plugins.apply('groovy')
        project.plugins.apply("org.grails.grails-web")

        //setup deploy
        setupMavenWarPublish(project)
        //should come last after setupMavenWarPublish as it needs to have MavenPublishPlugin
        project.rootProject.plugins.apply(PublishingRepoSetupPlugin)

        GrailsPlugin.addGrailsRepos(project)
    }

    @CompileDynamic
    void setupMavenWarPublish(final Project project) {
        project.getPlugins().apply(LocalSnapshotPlugin);
        Task snapshotTask = project.getTasks().getByName(LocalSnapshotPlugin.SNAPSHOT_TASK);
        snapshotTask.dependsOn(JavaPublishPlugin.MAVEN_LOCAL_TASK);
        project.getPlugins().apply("maven-publish");

        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction( {
                publications {
                    javaLibrary(MavenPublication) {
                        from project.components.web
                    }
                }
            })
        }
        //so that we flesh out problems with maven publication during the build process
        project.getTasks().getByName("build").dependsOn(JavaPublishPlugin.MAVEN_LOCAL_TASK);
    }
}
