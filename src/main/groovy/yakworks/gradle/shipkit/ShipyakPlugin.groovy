package yakworks.gradle.shipkit


import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.gradle.DefaultsPlugin
import yakworks.gradle.DocmarkPlugin

/**
 * Continuous delivery for Java/Groovy/Grails with CirclePlugin and Bintray.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java/Groovy/Grails multi-project build.
 */
@CompileStatic
class ShipyakPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShipyakPlugin)

    ConfigMap config

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())

        config = project.plugins.apply(ConfigYakPlugin).config

        project.plugins.apply(CiPublishPlugin)
        project.plugins.apply(DefaultsPlugin)

        boolean isBintray = config['bintray.enabled']
        if(isBintray){
            println "applying BintrayReleasePlugin isBintray=true"
            project.plugins.apply(BintrayReleasePlugin)
        } else{
            //we are publishing lib to artifactory
            project.plugins.apply(MavenRepoReleasePlugin)
        }
        project.plugins.apply(PomContributorsPlugin)

        wireUpDocPublishing(project)
    }

    //Sets dependendsOn and wires up so gitPush will take into account the README updates and the Mkdocs will get run after a release
    void wireUpDocPublishing(Project project){
        final Task updateReadme = project.tasks.getByName(DocmarkPlugin.UPDATE_README_TASK)
        final File rmeFile = project.file('README.md')
        GitPlugin.registerChangesForCommitIfApplied([rmeFile], 'README.md versions', updateReadme)

        final Task performRelease = project.tasks.getByName(ReleasePlugin.PERFORM_RELEASE_TASK)
        boolean enableDocsPublish = config['docs.enabled']
        if(enableDocsPublish) {
            String gitPublishDocsTaskName = 'gitPublishPush'
            if (project.hasProperty(ShipkitConfigurationPlugin.DRY_RUN_PROPERTY)) {
                gitPublishDocsTaskName = 'gitPublishCopy'
            }
            //LOG.lifecycle("gitPublishDocsTaskName $gitPublishDocsTaskName" )
            final Task gitPublishDocsTask = project.tasks.getByName(gitPublishDocsTaskName)
            gitPublishDocsTask.mustRunAfter(GitPlugin.GIT_PUSH_TASK)
            performRelease.dependsOn(gitPublishDocsTask)
        } else {
            //LOG.lifecycle("Doc will not be published as enableDocsPublish is false" )
        }
    }

}
