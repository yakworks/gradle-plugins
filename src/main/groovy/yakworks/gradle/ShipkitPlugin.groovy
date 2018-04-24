package yakworks.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil

/**
 * Continuous delivery for Java/Groovy/Grails with CirclePlugin and Bintray.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java/Groovy/Grails multi-project build.
 */
//@CompileStatic
class ShipkitPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShipkitPlugin)

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //apply CircleReleasePlugin plugin, has be done early, before ShipkitConfiguration, so it can add the snapshot task to the startParams
        project.plugins.apply(CircleReleasePlugin)
        //modified ShipkitBasePlugin
        project.plugins.apply(BintrayReleasePlugin)
        project.plugins.apply(PomContributorsPlugin)
        project.plugins.apply(DefaultsPlugin)

        wireUpDocPublishing(project)

        project.allprojects { Project subproject ->
            subproject.getPlugins().withId("yakworks.grails-plugin") {
                subproject.getPlugins().apply(GrailsPluginPublishPlugin)
            }
        }
    }

    //Sets dependendsOn and wires up so gitPush will take into account the README updates and the Mkdocs will get run after a release
    void wireUpDocPublishing(Project project){
        final Task updateReadme = project.tasks.getByName(DocmarkPlugin.UPDATE_README_TASK)
        final File rmeFile = project.file('README.md')
        GitPlugin.registerChangesForCommitIfApplied([rmeFile], 'README.md versions', updateReadme)

        final Task performRelease = project.getTasks().getByName(ReleasePlugin.PERFORM_RELEASE_TASK);
        final Task gitPublishPush = project.getTasks().getByName('gitPublishPush')
        gitPublishPush.mustRunAfter(GitPlugin.GIT_PUSH_TASK)
        performRelease.dependsOn(gitPublishPush)
    }

}
