/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.gradle.notes.UpdateReleaseNotesOnGitHubTask
import org.shipkit.gradle.notes.UpdateReleaseNotesTask
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.notes.ReleaseNotesPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin

import yakworks.commons.ConfigMap

import static org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin.DRY_RUN_PROPERTY
import static org.shipkit.internal.gradle.git.GitPlugin.GIT_PUSH_TASK
import static org.shipkit.internal.gradle.java.JavaPublishPlugin.MAVEN_LOCAL_TASK
import static org.shipkit.internal.gradle.java.JavaPublishPlugin.PUBLICATION_NAME
import static org.shipkit.internal.gradle.notes.ReleaseNotesPlugin.UPDATE_NOTES_TASK
import static org.shipkit.internal.gradle.release.ReleasePlugin.PERFORM_RELEASE_TASK

/**
 * Configures Java project for automated releases with to a maven repo such as artifactory or nexus.
 * Modified from shipkits original BintrayReleasePlugin
 */
@CompileStatic
class MavenRepoReleasePlugin implements Plugin<Project> {
    public final static String MAVEN_PUBLISH_REPO_TASK = "publish${PUBLICATION_NAME.capitalize()}PublicationToMavenRepository"

    void apply(final Project project) {
        ReleasePlugin releasePlugin = project.plugins.apply(ReleasePlugin) as ReleasePlugin //should have already been done by now

        def skplugin = project.plugins.apply(ShipkitConfigurationPlugin) as ShipkitConfigurationPlugin
        ShipkitConfiguration shipkitConf = skplugin.configuration

        Task gitPush = (Task)project.property(GIT_PUSH_TASK)
        Task performRelease = project.tasks.getByName(PERFORM_RELEASE_TASK)

        ConfigMap config = (ConfigMap)project.property('config')

        project.allprojects { Project subproject ->

            subproject.getPlugins().withType(MavenPublishPlugin) {
                String mavenLocalTask = "${subproject.getPath()}:$MAVEN_LOCAL_TASK"

                if (subproject.hasProperty(DRY_RUN_PROPERTY)) {
                    //if its a dryrun test then we are just publish to local
                    performRelease.dependsOn(mavenLocalTask)
                } else {
                    //publish after git push so that when git push fails we don't publish jars to bintray git push is easier to undo than
                    // deleting published jars (not possible with Central)
                    //the MAVEN_PUBLISH_REPO_TASK gets added so late that we need to listen for its creation. the normal build life cycle wont work
                    subproject.tasks.whenTaskAdded { Task task ->
                        if(task.name == MAVEN_PUBLISH_REPO_TASK){
                            task.mustRunAfter(gitPush)
                        }
                    }
                    performRelease.dependsOn("${subproject.getPath()}:$MAVEN_PUBLISH_REPO_TASK")
                }

                subproject.afterEvaluate {
                    //UpdateReleaseNotesTask needs to be modified as its hard coded to use bintray when writing out the release-notes
                    UpdateReleaseNotesTask updateNotes = (UpdateReleaseNotesTask) project[UPDATE_NOTES_TASK]
                    UpdateReleaseNotesOnGitHubTask updateNotesOnGitHub = (UpdateReleaseNotesOnGitHubTask) project.getTasks().getByName(
                        ReleaseNotesPlugin.UPDATE_NOTES_ON_GITHUB_TASK)

                    String userSpecifiedRepo = shipkitConf.lenient.releaseNotes.publicationRepository
                    //println "userSpecifiedRepo : $userSpecifiedRepo"
                    ///println "updateNotes.publicationRepository : ${updateNotes.publicationRepository}"
                    if (userSpecifiedRepo != null) {
                        updateNotes.publicationRepository = userSpecifiedRepo
                        updateNotesOnGitHub.publicationRepository = userSpecifiedRepo
                    } else {
                        String groupPath = subproject.group.toString().replace('.', '/')
                        //Otherwise build it by hand
                        updateNotes.publicationRepository = "${config['maven.publishUrl']}/${groupPath}/${subproject.name}/"
                        updateNotesOnGitHub.publicationRepository = updateNotes.publicationRepository
                    }
                    //println "updateNotes.publicationRepository : ${updateNotes.publicationRepository}"
                }

                //Making git push run as late as possible because it is an operation that is hard to reverse.
                gitPush.mustRunAfter(mavenLocalTask)
            }

        }
    }
}
