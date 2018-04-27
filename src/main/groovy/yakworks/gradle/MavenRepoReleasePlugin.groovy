package yakworks.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.gradle.notes.UpdateReleaseNotesTask
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.notes.ReleaseNotesPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.StringUtil

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
        ReleasePlugin releasePlugin = project.plugins.apply(ReleasePlugin) //should have already been done by now
        ShipkitConfiguration conf = project.plugins.apply(ShipkitConfigurationPlugin).configuration

        Task gitPush = (Task)project.property(GIT_PUSH_TASK)
        Task performRelease = project.tasks.getByName(PERFORM_RELEASE_TASK)

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
                    performRelease.dependsOn(MAVEN_PUBLISH_REPO_TASK)
                }

                //TODO FIXME See the UpdateReleaseNotesTask.setPublicationRepository in BintrayRelease plugin.
                //UpdateReleaseNotesTask needs to be modified as its hard coded to use bintray when writing out the release-notes
                def updateNotes = (UpdateReleaseNotesTask) project[UPDATE_NOTES_TASK]
                updateNotes.publicationRepository = conf.releaseNotes.publicationRepository

                //Making git push run as late as possible because it is an operation that is hard to reverse.
                gitPush.mustRunAfter(mavenLocalTask)
            }

        }
    }
}
