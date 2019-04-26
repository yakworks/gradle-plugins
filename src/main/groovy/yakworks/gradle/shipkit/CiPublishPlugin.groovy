package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.gradle.release.ReleaseNeededTask
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.release.CiReleasePlugin
import org.shipkit.internal.gradle.release.ReleaseNeededPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.release.tasks.ReleaseNeeded
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.Shell

/**
 * Why?: Shipkit has CiReleasePlugin. This does special snapshot wiring and sets up detection
 * for commits that only changed things like docs and should not perform a full release.
 * All is configurable in config.yml so that it can work with both circle and travis.
 */
@CompileStatic
public class CiPublishPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(CiPublishPlugin)
    public static final String CI_PUBLISH_TASK = "ciPublish"
    public static final String PUBLISH_RELEASE_TASK = "publishRelease"
    //public static final String CI_CHECK_TASK = "ciCheck"

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration()

        project.plugins.apply(CiReleasePlugin)
        project.plugins.apply(CirclePlugin)

        //add a check shell command. simply depending on it does not seem to fire it so we hard wire it this way
        //down the line, during snapshot, we check for code changes that are not just docs changes
        //and have the root ciPublish depend on this if there are.
        //NOT USED, Works locally but not on circle
        //            ShipkitExecTask ciCheckTask = project.task(CI_CHECK_TASK, type:ShipkitExecTask){
        //                description = "Runs the `gradle check` in a sep command process"
        //                execCommands.add(execCommand("check tests", ["./gradlew", 'check', '--no-daemon']))
        //                //execCommands.add(execCommand("check tests", ["./gradlew", 'check', '--no-daemon'], ExecCommandFactory.stopExecution()))
        //            }

        if(System.getenv('CI')) {
            def ciPubTask = project.task(CI_PUBLISH_TASK)

            if (project['isSnapshot']) {
                project.allprojects { Project subproject ->
                    subproject.plugins.withType(JavaPublishPlugin) {
                        setupCiPublishForSnapshots(subproject, ciPubTask)
                    }
                }
            } else {
                ciPubTask.dependsOn(CiReleasePlugin.CI_PERFORM_RELEASE_TASK)
            }

            addGitConfigUser(conf)
        }
        else { //
            def pubTask = project.task(PUBLISH_RELEASE_TASK)

            if (project['isSnapshot']) {
                project.allprojects { Project subproject ->
                    subproject.plugins.withType(JavaPublishPlugin) {
                        pubTask.dependsOn("${subproject.getPath()}:$MavenRepoReleasePlugin.MAVEN_PUBLISH_REPO_TASK")
                    }
                }
            } else {
                pubTask.dependsOn(ReleasePlugin.PERFORM_RELEASE_TASK)
            }
        }

    }

    /**
     * Checks if snapshot release is needed for the current branch.
     * checks for changes to docs and only publishes them if they are the only thing that changes
     */
    void setupCiPublishForSnapshots(Project project, Task ciPublishTask) {

        //def ciPublishTask = project.task(CI_PUBLISH_TASK)

        ReleaseNeededTask rtask = (ReleaseNeededTask)project.rootProject.tasks.getByName(ReleaseNeededPlugin.ASSERT_RELEASE_NEEDED_TASK)
        String branch = System.getenv("CIRCLE_BRANCH")
        boolean releasableBranch = branch?.matches(rtask.releasableBranchRegex)
        boolean skipEnvVariable = System.getenv('SKIP_RELEASE').toBoolean()
        boolean skippedByCommitMessage = rtask.commitMessage?.contains("[ci skip-release]")

        LOG.lifecycle("Should Release SNAPSHOT on branch [${rtask.branch}] :\n" +
            " - releasableBranch: " + releasableBranch + ", $branch matches (${rtask.releasableBranchRegex}) \n" +
            " - isPullRequest: " + rtask.isPullRequest() + "\n" +
            " - skipEnvVariable: " + skipEnvVariable + "\n" +
            " - skippedByCommitMessage: " + skippedByCommitMessage + "\n"
        )

        if(releasableBranch && !rtask.isPullRequest() && !skipEnvVariable && !skippedByCommitMessage) {
            String commitRange = Shell.exec('echo "$CIRCLE_COMPARE_URL" | rev | cut -d/ -f1 | rev')
            def gitDiff = "git diff --name-only $commitRange"
            def grepReg = $/"(README\.md|mkdocs\.yml|docs/)"/$

            boolean hasAppChanges = ['sh', '-c', gitDiff + ' | grep --invert-match -E ' + grepReg].execute().text.trim().length() > 0
            boolean hasDocChanges = ['sh', '-c', gitDiff + ' | grep -E ' + grepReg].execute().text.trim().length() > 0
            LOG.lifecycle(" - Has application changes and will run publish: " + hasAppChanges + "\n" +
                " - Docs have changed will run `:gitPublishPush` : " + hasDocChanges)
            if(hasAppChanges){
                String publishMavSnap = "${project.getPath()}:$MavenRepoReleasePlugin.MAVEN_PUBLISH_REPO_TASK"
                ciPublishTask.dependsOn(publishMavSnap)
            }
            if(hasDocChanges) ciPublishTask.dependsOn(':gitPublishPush')
        } else {
            LOG.lifecycle("SNAPSHOT publish will be skipped. See Logs above")
        }

    }

    //some of the grgit doesnt seem to add the config for user info.
    /**
     * add the github user info so commits are more readable, GRGIT doesn't seem to pick up the way shipkit does it.
     */
    void addGitConfigUser(ShipkitConfiguration conf){
        ['git', 'config', '--global', 'user.name', conf.git.user].execute()
        ['git', 'config', '--global', 'user.email', conf.git.email].execute()
        ['git', 'config', '--global', 'credential.helper', "store --file=~/.git-credentials"].execute()
    }
}
