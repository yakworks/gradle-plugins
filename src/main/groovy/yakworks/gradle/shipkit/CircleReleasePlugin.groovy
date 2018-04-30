package yakworks.gradle.shipkit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.gradle.exec.ShipkitExecTask
import org.shipkit.gradle.release.ReleaseNeededTask
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.release.CiReleasePlugin
import org.shipkit.internal.gradle.release.ReleaseNeededPlugin
import org.shipkit.internal.gradle.release.tasks.ReleaseNeeded
import org.shipkit.internal.gradle.util.ProjectUtil
import org.shipkit.internal.gradle.version.VersioningPlugin
import org.shipkit.internal.util.PropertiesUtil

import static org.shipkit.internal.gradle.exec.ExecCommandFactory.execCommand

/**
 * Does special Snapshot wiring and config with CircleCI.
 * For snapshot this will read the snapshot property in the version.propeties and if true
 * it sets up the ciPublish to use
 */
//@CompileStatic
public class CircleReleasePlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(CircleReleasePlugin)
    public static final String CI_PUBLISH_TASK = "ciPublish"
    public static final String CI_CHECK_TASK = "ciCheck"

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //addSnaphotTaskFromVersionProp has to be done before ShipkitConfiguration, so it can add the snapshot task to the startParams
        addSnaphotTaskFromVersionProp(project)

        ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration()

        project.plugins.apply(CiReleasePlugin)
        project.plugins.apply(CirclePlugin)

        if(System.env['CI']) {
            def ciPubTask = project.task(CI_PUBLISH_TASK)

            //add a check shell command. simply depending on it does not seem to fire it so we hard wire it this way
            //down the line, during snapshot, we check for code changes that are not just docs changes
            //and have the root ciPublish depend on this if there are.
            //NOT USED, Works locally but not on circle
            ShipkitExecTask ciCheckTask = project.task(CI_CHECK_TASK, type:ShipkitExecTask){
                description = "Runs the `gradle check` in a sep command process"
                execCommands.add(execCommand("check tests", ["./gradlew", 'check', '--no-daemon']))
                //execCommands.add(execCommand("check tests", ["./gradlew", 'check', '--no-daemon'], ExecCommandFactory.stopExecution()))
            }

            if (project.snapshotVersion) {
                project.allprojects { Project subproject ->
                    subproject.getPlugins().withType(JavaPublishPlugin) {
                        setupCiPublishForSnapshots(subproject, ciPubTask)
                    }
                }
            } else {
                //runs the normal ciPerformRelease after it runs a check
                //ciPubTask.dependsOn(ciCheckTask)
                ciPubTask.dependsOn(CiReleasePlugin.CI_PERFORM_RELEASE_TASK)
            }

            addGitConfigUser(conf)
        }

    }

    /**
     * Look into version file for a snapshot property. Add the snapshot task to startParameter.taskNames
     * if its true.
     */
    void addSnaphotTaskFromVersionProp(Project project) {
        final File versionFile = project.file(VersioningPlugin.VERSION_FILE_NAME);
        String snapshot = PropertiesUtil.readProperties(versionFile).getProperty("snapshot")
        def bSnapshot = Boolean.parseBoolean(snapshot ?: 'false')
        List startTasks = project.gradle.startParameter.taskNames
        project.ext.snapshotVersion = startTasks.contains('snapshot') || bSnapshot

        boolean excludedTasks = startTasks.any { ['resolveConfigurations', 'clean'].contains(it)}

        if(project.snapshotVersion && !excludedTasks) {
            startTasks.add(0, 'snapshot')
            project.gradle.startParameter.taskNames = startTasks
            LOG.lifecycle("  Snapshot set in versions file. Added snapshot task.")
            //println project.gradle.startParameter.taskNames
        }
    }

    /**
     * Checks if snapshot release is needed for the current branch.
     * checks for changes to docs and only publishes them if they are the only thing that changes
     */
    void setupCiPublishForSnapshots(Project project, Task ciPublishTask) {

        //def ciPublishTask = project.task(CI_PUBLISH_TASK)

        ReleaseNeededTask rtask = project.rootProject.tasks.getByName(ReleaseNeededPlugin.ASSERT_RELEASE_NEEDED_TASK)
        String branch = System.getenv("CIRCLE_BRANCH")
        boolean releasableBranch = branch?.matches(rtask.releasableBranchRegex)
        boolean skipEnvVariable = Boolean.parseBoolean(System.env['SKIP_RELEASE'])
        boolean skippedByCommitMessage = rtask.commitMessage?.contains(ReleaseNeeded.SKIP_RELEASE_KEYWORD)

        LOG.lifecycle("Should Release SNAPSHOT on branch [${rtask.branch}] :\n" +
            " - releasableBranch: " + releasableBranch + ", $branch matches (${rtask.releasableBranchRegex}) \n" +
            " - isPullRequest: " + rtask.isPullRequest() + "\n" +
            " - skipEnvVariable: " + skipEnvVariable + "\n" +
            " - skippedByCommitMessage: " + skippedByCommitMessage + "\n"
        )

        if(releasableBranch && !rtask.isPullRequest() && !skipEnvVariable && !skippedByCommitMessage) {
            def commitRange = ['sh', '-c', 'echo "$CIRCLE_COMPARE_URL" | rev | cut -d/ -f1 | rev'].execute().text.trim()
            def gitDiff = "git diff --name-only $commitRange"
            def grepReg = '"(README\\.md|mkdocs\\.yml|docs/)"'

            boolean hasAppChanges = ['sh', '-c', gitDiff + ' | grep --invert-match -E ' + grepReg].execute().text.trim().length() > 0
            boolean hasDocChanges = ['sh', '-c', gitDiff + ' | grep -E ' + grepReg].execute().text.trim().length() > 0
            LOG.lifecycle(" - Has application changes and will run publish: " + hasAppChanges + "\n" +
                " - Docs have changed will run `:gitPublishPush` : " + hasDocChanges)
            if(hasAppChanges){
                //rootPubTask.dependsOn(CI_CHECK_TASK)
                ciPublishTask.dependsOn('publish')
            }
            if(hasDocChanges) ciPublishTask.dependsOn(':gitPublishPush')
        } else {
            LOG.lifecycle("SNAPSHOT publish will be skipped. See Logs above")
        }

    }

    //some of the grgit doesnt seem to add the config for user info.
    void addGitConfigUser(ShipkitConfiguration conf){
        ['git', 'config', '--global', 'user.name', conf.git.user].execute()
        ['git', 'config', '--global', 'user.email', conf.git.email].execute()
        ['git', 'config', '--global', 'credential.helper', "store --file=~/.git-credentials"].execute()
    }
}