package yakworks.gradle.shipkit.versionupgrade;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.gradle.exec.ShipkitExecTask;
import org.shipkit.internal.gradle.configuration.DeferredConfiguration;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.exec.ExecCommandFactory;
import org.shipkit.internal.gradle.git.CloneGitRepositoryTaskFactory;
import org.shipkit.internal.gradle.util.GradleWrapper;
import org.shipkit.internal.gradle.util.TaskMaker;
import org.shipkit.internal.gradle.versionupgrade.UpgradeDownstreamExtension;
import org.shipkit.internal.util.ExposedForTesting;
import org.shipkit.version.VersionInfo;

import java.text.MessageFormat;

import static org.shipkit.internal.util.ArgumentValidation.notNull;
import static org.shipkit.internal.util.RepositoryNameUtil.repositoryNameToCapitalizedCamelCase;

/**
 * BEWARE! This plugin is in incubating state, so its API may change in the future!
 * The plugin applies following plugins:
 *
 * <ul>
 *     <li>{@link ShipkitConfigurationPlugin}</li>
 * </ul>
 *
 * and adds following tasks:
 *
 * <ul>
 *     <li>clone{consumerRepository} - clones consumer repository into temporary directory</li>
 *     <li>upgrade{consumerRepository} - runs task performVersionUpgrade on consumerRepository</li>
 *     <li>upgradeDownstream - task aggregating all of the upgrade{consumerRepository} tasks</li>
 * </ul>
 *
 * Plugin performs a version upgrade of the project that it's applied in, for all consumer repositories defined.
 * Example of plugin usage:
 *
 * Configure your 'shipkit.gradle' file like here:
 *
 *      apply plugin: 'org.shipkit.upgrade-downstream'
 *
 *      upgradeDownstream {
 *          repositories = ['wwilk/shipkit', 'wwilk/mockito']
 *      }
 *
 * and then call:
 *
 * ./gradlew upgradeDownstream
 *
 */
//COPIED FROM SHIPKIT SO IT ADDS THE TOKEN TO THE CLONE
public class UpgradeDownstreamPlugin implements Plugin<Project> {

    public static final String UPGRADE_DOWNSTREAM_TASK = "upgradeDownstream";
    UpgradeDownstreamExtension upgradeDownstreamExtension;

    @Override
    public void apply(final Project project) {
        final ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration();

        upgradeDownstreamExtension = project.getExtensions().create("upgradeDownstream", UpgradeDownstreamExtension.class);

        final Task performAllUpdates = TaskMaker.task(project, UPGRADE_DOWNSTREAM_TASK, new Action<Task>() {
            @Override
            public void execute(final Task task) {
                task.setDescription("Performs dependency upgrade in all downstream repositories.");
            }
        });

        DeferredConfiguration.deferredConfiguration(project, new Runnable() {
            @Override
            public void run() {
                // notNull(upgradeDownstreamExtension.getRepositories(),
                //     "'upgradeDownstream.repositories'");
                if(upgradeDownstreamExtension.getRepositories() == null) return;
                for (String consumerRepositoryName : upgradeDownstreamExtension.getRepositories()) {
                    //This is the fix, its not quiet and logs out the token. XXX flaw
                    String token = conf.getLenient().getGitHub().getWriteAuthToken();
                    String githubUrl = "https://" + token + ":@github.com";
                    Task cloneTask = CloneGitRepositoryTaskFactory.createCloneTask(project, githubUrl, consumerRepositoryName);
                    Task performUpdate = createProduceUpgradeTask(project, consumerRepositoryName);
                    performUpdate.dependsOn(cloneTask);
                    performAllUpdates.dependsOn(performUpdate);
                }
            }
        });
    }

    private Task createProduceUpgradeTask(final Project project, final String consumerRepository) {
        return TaskMaker.task(project, "upgrade" + repositoryNameToCapitalizedCamelCase(consumerRepository), ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            @Override
            public void execute(final ShipkitExecTask task) {
                task.setDescription("Performs dependency upgrade in " + consumerRepository);
                task.execCommand(ExecCommandFactory.execCommand("Upgrading dependency",
                    CloneGitRepositoryTaskFactory.getConsumerRepoCloneDir(project, consumerRepository),
                    GradleWrapper.getWrapperCommand(), "performVersionUpgrade", getDependencyProperty(project)));
            }
        });
    }

    private String getDependencyProperty(Project project) {
        VersionInfo info = project.getRootProject().getExtensions().getByType(VersionInfo.class);
        return String.format("-Pdependency=%s:%s:%s", project.getGroup().toString(), project.getName(), info.getPreviousVersion());
    }

    @ExposedForTesting
    protected UpgradeDownstreamExtension getUpgradeDownstreamExtension() {
        return upgradeDownstreamExtension;
    }
}
