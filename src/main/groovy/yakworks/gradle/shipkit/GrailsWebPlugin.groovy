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

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.snapshot.LocalSnapshotPlugin
import org.shipkit.internal.gradle.util.GradleDSLHelper
import org.shipkit.internal.gradle.util.PomCustomizer
import org.shipkit.internal.gradle.util.StringUtil

/**
 * A marker for a grails plugin, "yakworks.grails-plugin", will apply GrailsPluginPublishPlugin later after config
 */
@CompileStatic
class GrailsWebPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(GrailsWebPlugin.class);

    void apply(Project project) {
        project.plugins.apply(ShippablePlugin)

        project.plugins.apply('war')
        project.plugins.apply('groovy')
        project.plugins.apply("org.grails.grails-web")

        //setup deploy
        setupMavenWarPublish(project)
        project.rootProject.plugins.apply(MavenConfPlugin) //should come last after setupMavenWarPublish as it needs to have MavenPublishPlugin
    }

    @CompileDynamic
    void setupMavenWarPublish(final Project project) {
        project.getPlugins().apply(LocalSnapshotPlugin.class);
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
