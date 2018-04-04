package yakworks.gradle

import com.jfrog.bintray.gradle.BintrayExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.gradle.notes.UpdateReleaseNotesTask
import org.shipkit.internal.gradle.bintray.ShipkitBintrayPlugin
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.notes.ReleaseNotesPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.BintrayUtil

import static org.shipkit.internal.gradle.configuration.DeferredConfiguration.deferredConfiguration

/**
 * Configures Java project for automated releases with Bintray.
 * <p>
 * Applies following plugins:
 *
 * <ul>
 *     <li>{@link ReleasePlugin}</li>
 * </ul>
 *
 * Adds following behavior to all submodules that have {@link JavaBintrayPlugin}:
 *
 * <ul>
 *     <li>Hooks up bintray upload tasks to the release tasks</li>
 *     <li>Configures Bintray publication repository on the release notes tasks</li>
 * </ul>
 */
//@CompileStatic
class BintrayReleasePlugin implements Plugin<Project> {

    void apply(final Project project) {
        project.getPlugins().apply(ReleasePlugin.class)
        final ShipkitConfiguration conf = project.plugins.apply(ShipkitConfigurationPlugin.class).configuration
        project.allprojects { subproject ->
            subproject.plugins.withType(ShipkitBintrayPlugin.class){ plugin ->
                Task bintrayUpload = subproject.getTasks().getByName(ShipkitBintrayPlugin.BINTRAY_UPLOAD_TASK)
                Task performRelease = project.getTasks().getByName(ReleasePlugin.PERFORM_RELEASE_TASK)
                performRelease.dependsOn(bintrayUpload)

                //Making git push run as late as possible because it is an operation that is hard to reverse.
                //Git push will be executed after all tasks needed by bintrayUpload
                // but before bintrayUpload.
                Task gitPush = project.getTasks().getByName(GitPlugin.GIT_PUSH_TASK)
                gitPush.mustRunAfter(bintrayUpload.getTaskDependencies())

                //bintray upload after git push so that when git push fails we don't publish jars to bintray
                //git push is easier to undo than deleting published jars (not possible with Central)
                bintrayUpload.mustRunAfter(gitPush)

                final BintrayExtension bintray = subproject.getExtensions().getByType(BintrayExtension.class)
                deferredConfiguration(subproject) {
                    UpdateReleaseNotesTask updateNotes = (UpdateReleaseNotesTask) project.getTasks().getByName(ReleaseNotesPlugin.UPDATE_NOTES_TASK)
                    String userSpecifiedRepo = conf.getLenient().getReleaseNotes().getPublicationRepository()
                    if (userSpecifiedRepo != null) {
                        updateNotes.setPublicationRepository(userSpecifiedRepo)
                    } else {
                        updateNotes.setPublicationRepository(BintrayUtil.getRepoLink(bintray))
                    }
                }
            }
        }
    }
}
