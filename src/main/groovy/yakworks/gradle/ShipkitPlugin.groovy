package yakworks.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action;
import org.gradle.api.Plugin;
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
import org.shipkit.gradle.git.GitCommitTask
import org.shipkit.gradle.version.BumpVersionFileTask
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.bintray.ShipkitBintrayPlugin
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.ComparePublicationsPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin
import org.shipkit.internal.gradle.release.CiReleasePlugin
import org.shipkit.internal.gradle.release.ReleasePlugin;
import org.shipkit.internal.gradle.release.ShipkitBasePlugin
import org.shipkit.internal.gradle.release.TravisPlugin
import org.shipkit.internal.gradle.snapshot.LocalSnapshotPlugin
import org.shipkit.internal.gradle.util.GradleDSLHelper
import org.shipkit.internal.gradle.util.PomCustomizer
import org.shipkit.internal.gradle.util.ProjectUtil
import org.shipkit.internal.gradle.util.TaskMaker
import org.shipkit.internal.gradle.version.VersionInfoFactory
import org.shipkit.internal.gradle.version.VersioningPlugin
import org.shipkit.internal.util.PropertiesUtil
import org.shipkit.version.VersionInfo

import static java.util.Collections.singletonList;

/**
 * Continuous delivery for Java/Groovy/Grails with CirclePlugin and Bintray.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java/Groovy/Grails multi-project build.
 */
//@CompileStatic
public class ShipkitPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShipkitPlugin)

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        setupSnaphotTaskFromVersionProp(project)
        //project.getPlugins().apply(ShipkitBasePlugin.class); see below for what this did
        //we don't apply the ShipkitBasePlugin because we don't wan't the TravisPlugin applied and instead want circle ci
        project.getPlugins().apply(CirclePlugin)
        project.getPlugins().apply(BintrayReleasePlugin)
        project.getPlugins().apply(PomContributorsPlugin)
        project.getPlugins().apply(DefaultsPlugin)

        wireUpDocs(project)

        project.allprojects { Project subproject ->
            subproject.getPlugins().withId("yakworks.grails-plugin") {
                subproject.getPlugins().apply(GrailsPluginPublishPlugin)
                //setup publishVersion task that depends on publish if its a snapshot
                setupPublishVersionTask(subproject)
            }
        }
        //setup publishVersion that depends on performRelease for root when its not a snapshot
        if(!project.snapshotVersion) {
            project.task('publishVersion', dependsOn: ReleasePlugin.PERFORM_RELEASE_TASK)
            project.task('ciPublishVersion', dependsOn: CiReleasePlugin.CI_PERFORM_RELEASE_TASK)
        }

    }

    //Sets dependendsOn and wires up so gitPush will take into account the README updates and the Mkdocs will get run after a release
    void wireUpDocs(Project project){
        final Task updateReadme = project.tasks.getByName(DocmarkPlugin.UPDATE_README_TASK)
        final File rmeFile = project.file('README.md')
        GitPlugin.registerChangesForCommitIfApplied([rmeFile], 'README.md versions', updateReadme)

        final Task performRelease = project.getTasks().getByName(ReleasePlugin.PERFORM_RELEASE_TASK);
        final Task gitPublishPush = project.getTasks().getByName('gitPublishPush')
        gitPublishPush.mustRunAfter(GitPlugin.GIT_PUSH_TASK)
        performRelease.dependsOn(gitPublishPush)
    }

    void setupSnaphotTaskFromVersionProp(Project project) {
        final File versionFile = project.file(VersioningPlugin.VERSION_FILE_NAME);
        String snapshot = PropertiesUtil.readProperties(versionFile).getProperty("snapshot")
        def bSnapshot = Boolean.parseBoolean(snapshot ?: 'false')
        List startTasks = project.gradle.startParameter.taskNames
        project.ext.snapshotVersion = startTasks.contains('snapshot') || bSnapshot

        if(project.snapshotVersion && !startTasks.contains('snapshot')) {
            startTasks.add(0, 'snapshot')
            project.gradle.startParameter.taskNames = startTasks
            LOG.lifecycle("  Snapshot set in versions file. Added snapshot task.")
            //println project.gradle.startParameter.taskNames
        }
    }

    void setupPublishVersionTask(Project project) {
        //Task publishVersion = project.task('publishVersion')
        if(project.snapshotVersion) {
            Task publishTask = project.tasks.findByPath('publish')
            if(publishTask){
                project.task('publishVersion') {
                    dependsOn(publishTask)
                    dependsOn(':gitPublishPush')
                }
                //only do this if it has $CIRCLE_COMPARE_URL
                //check if has changes that are not docs. if so then dependOn publishTask
                project.task('ciPublishVersion'){
                    dependsOn(publishTask)
                    dependsOn(':gitPublishPush')
                }
                //check if has changes that are docs, if so then dependOn gitPublishPush

            }
        }
    }
}
