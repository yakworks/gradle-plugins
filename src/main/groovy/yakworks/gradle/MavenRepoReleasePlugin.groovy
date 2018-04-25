package yakworks.gradle;

import com.jfrog.bintray.gradle.BintrayExtension
import groovy.transform.CompileStatic;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.gradle.notes.UpdateReleaseNotesTask;
import org.shipkit.internal.gradle.bintray.ShipkitBintrayPlugin;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.git.GitPlugin;
import org.shipkit.internal.gradle.java.JavaBintrayPlugin;
import org.shipkit.internal.gradle.java.JavaPublishPlugin;
import org.shipkit.internal.gradle.notes.ReleaseNotesPlugin;
import org.shipkit.internal.gradle.release.ReleasePlugin;
import org.shipkit.internal.gradle.util.BintrayUtil;

import static org.shipkit.internal.gradle.configuration.DeferredConfiguration.deferredConfiguration;
import static org.shipkit.internal.gradle.java.JavaPublishPlugin.MAVEN_LOCAL_TASK;

/**
 * Configures Java project for automated releases with to a maven repo such as artifactory or nexus.
 * Modified from shipkits original BintrayReleasePlugin
 */
@CompileStatic
class MavenRepoReleasePlugin implements Plugin<Project> {

    void apply(final Project project) {
        ReleasePlugin releasePlugin = project.getPlugins().apply(ReleasePlugin.class) //should have already been done by now

        final Task gitPush = project.getTasks().getByName(GitPlugin.GIT_PUSH_TASK)
        final Task performRelease = project.getTasks().getByName(ReleasePlugin.PERFORM_RELEASE_TASK)

        project.allprojects { Project subproject ->

            subproject.getPlugins().withType(JavaPublishPlugin) {
                Task publish = subproject.getTasks().getByName('publish')
                String mavenLocalTask = "${subproject.getPath()}:$MAVEN_LOCAL_TASK"

                if (subproject.hasProperty(ShipkitConfigurationPlugin.DRY_RUN_PROPERTY)) {
                    //if its a dryrun test then just publish to local
                    publish = subproject.getTasks().getByName(mavenLocalTask)
                } else {
                    //publish after git push so that when git push fails we don't publish jars to bintray
                    //git push is easier to undo than deleting published jars (not possible with Central)
                    publish.mustRunAfter(gitPush)
                }
                performRelease.dependsOn(publish)

                //TODO FIXME See the UpdateReleaseNotesTask.setPublicationRepository in BintrayRelease plugin.
                //UpdateReleaseNotesTask needs to be modified as its hard coded to use bintray when writing out the release-notes

                //Making git push run as late as possible because it is an operation that is hard to reverse.
                gitPush.mustRunAfter(mavenLocalTask)
            }

        }
    }
}
