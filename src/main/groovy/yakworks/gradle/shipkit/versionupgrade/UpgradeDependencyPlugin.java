package yakworks.gradle.shipkit.versionupgrade;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Spec;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.gradle.exec.ShipkitExecTask;
import org.shipkit.gradle.git.GitPushTask;
import org.shipkit.internal.gradle.configuration.LazyConfiguration;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.git.GitConfigPlugin;
import org.shipkit.internal.gradle.git.GitOriginPlugin;
import org.shipkit.internal.gradle.git.GitUrlInfo;
import org.shipkit.internal.gradle.git.domain.PullRequest;
import org.shipkit.internal.gradle.git.tasks.GitCheckOutTask;
import org.shipkit.internal.gradle.git.tasks.GitPullTask;
import org.shipkit.internal.gradle.util.GitUtil;
import org.shipkit.internal.gradle.util.TaskMaker;
import org.shipkit.internal.gradle.versionupgrade.CreatePullRequestTask;
import org.shipkit.internal.gradle.versionupgrade.FindOpenPullRequestTask;
import org.shipkit.internal.gradle.versionupgrade.MergePullRequestTask;
import org.shipkit.internal.gradle.versionupgrade.UpgradeDependencyExtension;
import org.shipkit.internal.util.IncubatingWarning;

import java.util.Optional;

import static org.shipkit.internal.gradle.configuration.DeferredConfiguration.deferredConfiguration;
import static org.shipkit.internal.gradle.exec.ExecCommandFactory.execCommand;

/**
 * BEWARE! This plugin is in incubating state, so its API may change in the future!
 * The plugin applies following plugins:
 *
 * <ul>
 *     <li>{@link ShipkitConfigurationPlugin}</li>
 *     <li>{@link GitOriginPlugin}</li>
 *     <li>{@link GitConfigPlugin}</li>
 * </ul>
 *
 * and adds following tasks:
 *
 * <ul>
 *     <li>checkoutBaseBranch - checkouts base branch - the branch to which version upgrade should be applied through pull request</li>
 *     <li>pullUpstream - syncs the fork on which we perform version upgrade with the upstream repo</li>
 *     <li>findOpenPullRequest - finds an open pull request with version upgrade if it exists</li>
 *     <li>checkoutVersionBranch - checkouts version branch where version will be upgraded. A new branch or the head branch for open pull request</li>
 *     <li>replaceVersion - replaces version in build file, using dependency pattern</li>
 *     <li>commitVersionUpgrade - commits replaced version</li>
 *     <li>pushVersionUpgrade - pushes the commit to the version branch</li>
 *     <li>createPullRequest - creates a pull request between base and version branches if there is no open pull request for this dependency already</li>
 *     <li>mergePullRequest - wait for status checks defined for pull request and in case of success merge it to base branch. Task is executed only if there was no previously opened pull request. If createPullRequest task is skipped, mergePullRequest is also skipped and pull request needs to be merged manually. If no checks defined, pull request also needs to be merged manually</li>
 *     <li>performVersionUpgrade - task aggregating all of the above</li>
 * </ul>
 *
 * Plugin should be used in client projects that want to have automated version upgrades of some other dependency, that use the producer version of this plugin.
 * Project with the producer plugin applied would then clone a fork of client project and run './gradlew performVersionUpgrade -Pdependency=${group:name:version}' on it.
 *
 * Example of plugin usage:
 *
 * Configure your 'shipkit.gradle' file like here:
 *
 *      apply plugin: 'org.shipkit.upgrade-dependency'
 *
 *      upgradeDependency {
 *          baseBranch = 'release/2.x'
 *          buildFile = file('build.gradle')
 *      }
 *
 * and then call it:
 *
 * ./gradlew performVersionUpgrade -Pdependency=org.shipkit:shipkit:1.2.3
 */

// THIS IS COPIED FROM SHIPKIT TO REPLACE THE REGEX ON VERSION TO BE MORE FLEXIBLE
// It replaces ReplaceVersionTask and DependencyNewVersionParser to have a more flexible version regex pattern
// disables the MERGE_PULL_REQUEST as well so a user is forced to merge
public class UpgradeDependencyPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(UpgradeDependencyPlugin.class);

    public static final String CHECKOUT_BASE_BRANCH = "checkoutBaseBranch";
    public static final String PULL_UPSTREAM = "pullUpstream";
    public static final String FIND_OPEN_PULL_REQUEST = "findOpenPullRequest";
    public static final String CHECKOUT_VERSION_BRANCH = "checkoutVersionBranch";
    public static final String REPLACE_VERSION = "replaceVersion";
    public static final String COMMIT_VERSION_UPGRADE = "commitVersionUpgrade";
    public static final String PUSH_VERSION_UPGRADE = "pushVersionUpgrade";
    public static final String CREATE_PULL_REQUEST = "createPullRequest";
    public static final String PERFORM_VERSION_UPGRADE = "performVersionUpgrade";
    public static final String MERGE_PULL_REQUEST = "mergePullRequest";

    public static final String DEPENDENCY_PROJECT_PROPERTY = "dependency";

    private UpgradeDependencyExtension upgradeDependencyExtension;

    @Override
    public void apply(final Project project) {
        //IncubatingWarning.warn("upgrade-dependency plugin");
        final ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration();
        final GitOriginPlugin gitOriginPlugin = project.getRootProject().getPlugins().apply(GitOriginPlugin.class);
        project.getPlugins().apply(GitConfigPlugin.class);

        upgradeDependencyExtension = project.getExtensions().create("upgradeDependency", UpgradeDependencyExtension.class);

        // set defaults
        upgradeDependencyExtension.setBuildFile(project.file("build.gradle"));
        upgradeDependencyExtension.setBaseBranch("master");

        String dependency = (String) project.findProperty(DEPENDENCY_PROJECT_PROPERTY);

        new DependencyNewVersionParser(dependency).fillVersionUpgradeExtension(upgradeDependencyExtension);

        TaskMaker.task(project, CHECKOUT_BASE_BRANCH, GitCheckOutTask.class, new Action<GitCheckOutTask>() {
            @Override
            public void execute(final GitCheckOutTask task) {
                task.setDescription("Checks out the base branch.");

                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        task.setRev(upgradeDependencyExtension.getBaseBranch());
                    }
                });
            }
        });

        TaskMaker.task(project, PULL_UPSTREAM, GitPullTask.class, new Action<GitPullTask>() {
            @Override
            public void execute(final GitPullTask task) {
                task.setDescription("Performs git pull from upstream repository.");
                task.mustRunAfter(CHECKOUT_BASE_BRANCH);
                task.setDryRun(conf.isDryRun());

                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        task.setRev(upgradeDependencyExtension.getBaseBranch());
                    }
                });

                gitOriginPlugin.provideOriginRepo(task, new Action<String>() {
                    public void execute(String originRepo) {
                        GitUrlInfo info = new GitUrlInfo(conf, conf.getGitHub().getRepository());
                        task.setUrl(info.getGitUrl());
                        task.setSecretValue(info.getWriteToken());
                    }
                });
            }
        });

        final FindOpenPullRequestTask findOpenPullRequestTask = TaskMaker.task(project,
            FIND_OPEN_PULL_REQUEST, FindOpenPullRequestTask.class, new Action<FindOpenPullRequestTask>() {

            @Override
            public void execute(final FindOpenPullRequestTask task) {
                task.setDescription("Find an open pull request with version upgrade, if such exists.");
                task.mustRunAfter(PULL_UPSTREAM);

                task.setGitHubApiUrl(conf.getGitHub().getApiUrl());
                task.setAuthToken(conf.getLenient().getGitHub().getReadOnlyAuthToken());
                task.setUpstreamRepositoryName(conf.getGitHub().getRepository());
                task.setVersionBranchRegex(getVersionBranchName(
                    upgradeDependencyExtension.getDependencyName(), ReplaceVersionTask.VERSION_REGEX));
            }
        });

        TaskMaker.task(project, CHECKOUT_VERSION_BRANCH, GitCheckOutTask.class, new Action<GitCheckOutTask>() {
            public void execute(final GitCheckOutTask task) {
                task.setDescription("Creates a new version branch and checks it out.");
                task.mustRunAfter(FIND_OPEN_PULL_REQUEST);

                findOpenPullRequestTask.provideOpenPullRequest(task, new Action<Optional<PullRequest>>() {
                    @Override
                    public void execute(Optional<PullRequest> pullRequest) {
                        if (pullRequest.isPresent()) {
                            // don't create a new branch if there is already a branch with open pull request with version upgrade
                            task.setNewBranch(false);
                        } else {
                            task.setNewBranch(true);
                        }
                        task.setRev(getCurrentVersionBranchName(upgradeDependencyExtension.getDependencyName(),
                            upgradeDependencyExtension.getNewVersion(), pullRequest));

                    }
                });
            }
        });

        final ReplaceVersionTask replaceVersionTask = TaskMaker.task(project, REPLACE_VERSION, ReplaceVersionTask.class, new Action<ReplaceVersionTask>() {
            @Override
            public void execute(final ReplaceVersionTask task) {
                task.setDescription("Replaces dependency version in build file.");
                task.mustRunAfter(CHECKOUT_VERSION_BRANCH);
                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        task.setDependencyGroup(upgradeDependencyExtension.getDependencyGroup());
                        task.setNewVersion(upgradeDependencyExtension.getNewVersion());
                        task.setDependencyName(upgradeDependencyExtension.getDependencyName());
                        task.setBuildFile(upgradeDependencyExtension.getBuildFile());
                    }
                });
            }
        });

        final ShipkitExecTask shipkitExecTask = TaskMaker.task(project, COMMIT_VERSION_UPGRADE, ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            @Override
            public void execute(final ShipkitExecTask exec) {
                exec.setDescription("Commits updated build file.");
                exec.mustRunAfter(REPLACE_VERSION);
                exec.dependsOn(GitConfigPlugin.SET_EMAIL_TASK, GitConfigPlugin.SET_USER_TASK);

                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        String message = String.format("%s version upgraded to %s", upgradeDependencyExtension.getDependencyName(), upgradeDependencyExtension.getNewVersion());
                        exec.execCommand(execCommand("Committing build file",
                            "git", "commit", "--author", GitUtil.getGitGenericUserNotation(conf.getGit().getUser(), conf.getGit().getEmail()),
                            "-m", message, upgradeDependencyExtension.getBuildFile().getAbsolutePath()));
                    }
                });
                exec.onlyIf(wasBuildFileUpdatedSpec(replaceVersionTask));
            }
        });

        final GitPushTask gitPushTask = TaskMaker.task(project, PUSH_VERSION_UPGRADE, GitPushTask.class, new Action<GitPushTask>() {
            @Override
            public void execute(final GitPushTask task) {
                task.setDescription("Pushes updated config file to an update branch.");
                task.mustRunAfter(COMMIT_VERSION_UPGRADE);

                task.setDryRun(conf.isDryRun());

                gitOriginPlugin.provideOriginRepo(task, new Action<String>() {
                    public void execute(String originRepo) {
                        GitUrlInfo info = new GitUrlInfo(conf, conf.getGitHub().getRepository());
                        task.setUrl(info.getGitUrl());
                        task.setSecretValue(info.getWriteToken());
                    }
                });

                findOpenPullRequestTask.provideOpenPullRequest(task, new Action<Optional<PullRequest>>() {
                    @Override
                    public void execute(Optional<PullRequest> pullRequest) {
                        task.getTargets().add(getCurrentVersionBranchName(upgradeDependencyExtension.getDependencyName(),
                            upgradeDependencyExtension.getNewVersion(), pullRequest));
                    }
                });

                task.onlyIf(wasBuildFileUpdatedSpec(replaceVersionTask));
            }
        });

        final CreatePullRequestTask createPullRequestTask = TaskMaker.task(project, CREATE_PULL_REQUEST, CreatePullRequestTask.class, new Action<CreatePullRequestTask>() {
            @Override
            public void execute(final CreatePullRequestTask task) {
                task.setDescription("Creates a pull request from branch with version upgraded to master");
                task.mustRunAfter(PUSH_VERSION_UPGRADE);
                task.setGitHubApiUrl(conf.getGitHub().getApiUrl());
                task.setDryRun(conf.isDryRun());
                task.setAuthToken(conf.getLenient().getGitHub().getWriteAuthToken());
                task.setPullRequestTitle(getPullRequestTitle(upgradeDependencyExtension));
                task.setPullRequestDescription(getPullRequestDescription(upgradeDependencyExtension));
                task.setUpstreamRepositoryName(conf.getGitHub().getRepository());

                gitOriginPlugin.provideOriginRepo(task, new Action<String>() {
                    @Override
                    public void execute(String originRepoName) {
                        task.setForkRepositoryName(originRepoName);
                    }
                });

                findOpenPullRequestTask.provideOpenPullRequest(task, new Action<Optional<PullRequest>>() {
                    @Override
                    public void execute(Optional<PullRequest> pullRequest) {
                        task.setVersionBranch(getCurrentVersionBranchName(upgradeDependencyExtension.getDependencyName(),
                            upgradeDependencyExtension.getNewVersion(), pullRequest));
                    }
                });

                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        task.setBaseBranch(upgradeDependencyExtension.getBaseBranch());
                    }
                });

                task.onlyIf(wasOpenPullRequestNotFound(findOpenPullRequestTask));
                task.onlyIf(wasBuildFileUpdatedSpec(replaceVersionTask));
            }
        });

        TaskMaker.task(project, MERGE_PULL_REQUEST, MergePullRequestTask.class, new Action<MergePullRequestTask>() {
            @Override
            public void execute(final MergePullRequestTask task) {
                task.setDescription("Merge pull request when all checks will be passed");
                task.mustRunAfter(CREATE_PULL_REQUEST);
                task.setGitHubApiUrl(conf.getGitHub().getApiUrl());
                task.setDryRun(conf.isDryRun());
                task.setAuthToken(conf.getLenient().getGitHub().getWriteAuthToken());
                task.setBaseBranch(upgradeDependencyExtension.getBaseBranch());
                task.setUpstreamRepositoryName(conf.getGitHub().getRepository());

                gitOriginPlugin.provideOriginRepo(task, new Action<String>() {
                    @Override
                    public void execute(String originRepoName) {
                        task.setForkRepositoryName(originRepoName);
                    }
                });

                createPullRequestTask.provideCreatedPullRequest(task, new Action<PullRequest>() {
                    @Override
                    public void execute(PullRequest pullRequest) {
                        setPullRequestDataToTask(Optional.ofNullable(pullRequest), task);
                    }
                });

                deferredConfiguration(project, new Runnable() {
                    @Override
                    public void run() {
                        task.setBaseBranch(upgradeDependencyExtension.getBaseBranch());
                    }
                });

                task.onlyIf(wasOpenPullRequestNotFound(findOpenPullRequestTask));
                task.onlyIf(wasBuildFileUpdatedSpec(replaceVersionTask));
            }
        });

        TaskMaker.task(project, PERFORM_VERSION_UPGRADE, new Action<Task>() {
            @Override
            public void execute(Task task) {
                task.setDescription("Checkouts new version branch, updates Shipkit dependency in config file, commits and pushes.");
                task.dependsOn(CHECKOUT_BASE_BRANCH);
                task.dependsOn(FIND_OPEN_PULL_REQUEST);
                task.dependsOn(PULL_UPSTREAM);
                task.dependsOn(CHECKOUT_VERSION_BRANCH);
                task.dependsOn(REPLACE_VERSION);
                task.dependsOn(COMMIT_VERSION_UPGRADE);
                task.dependsOn(PUSH_VERSION_UPGRADE);
                task.dependsOn(CREATE_PULL_REQUEST);
                //task.dependsOn(MERGE_PULL_REQUEST);
            }
        });

        addLazyDependencyValidation(dependency, createPullRequestTask, gitPushTask, findOpenPullRequestTask,
            replaceVersionTask, shipkitExecTask);
    }

    private void setPullRequestDataToTask(Optional<PullRequest> pullRequest, MergePullRequestTask task) {
        if (pullRequest.isPresent()) {
            String branch = getCurrentVersionBranchName(upgradeDependencyExtension.getDependencyName(),
                upgradeDependencyExtension.getNewVersion(), pullRequest);
            task.setVersionBranch(branch);
            task.setPullRequestSha(pullRequest.get().getSha());
            task.setPullRequestUrl(pullRequest.get().getUrl());
            task.setPullRequestNumber(pullRequest.get().getNumber());
        } else {
            LOG.info("Because pull request was not created, we were not able to fully configure task {}.", task.getPath());
        }
    }

    static String getCurrentVersionBranchName(String dependencyName, String version, Optional<PullRequest> pr) {
        if (pr.isPresent()) {
            return pr.get().getRef();
        }
        return getVersionBranchName(dependencyName, version);
    }

    private String getPullRequestDescription(UpgradeDependencyExtension versionUpgrade) {
        return String.format("This pull request was automatically created by Shipkit's" +
                " 'org.shipkit.upgrade-downstream' Gradle plugin (http://shipkit.org)." +
                " Please merge it so that you are using fresh version of '%s' dependency.",
            versionUpgrade.getDependencyName());
    }

    private String getPullRequestTitle(UpgradeDependencyExtension versionUpgrade) {
        return String.format("Version of %s upgraded to %s", versionUpgrade.getDependencyName(), versionUpgrade.getNewVersion());
    }

    private Spec<Task> wasBuildFileUpdatedSpec(final ReplaceVersionTask replaceVersionTask) {
        return new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task element) {
                return replaceVersionTask.isBuildFileUpdated();
            }
        };
    }

    private Spec<Task> wasOpenPullRequestNotFound(final FindOpenPullRequestTask findOpenPullRequestTask) {
        return new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return findOpenPullRequestTask.getPullRequest() == null;
            }
        };
    }

    private static String getVersionBranchName(String dependencyName, String newVersion) {
        return "upgrade-" + dependencyName + "-to-" + newVersion;
    }

    public UpgradeDependencyExtension getUpgradeDependencyExtension() {
        return upgradeDependencyExtension;
    }

    private void addLazyDependencyValidation(final String dependency, Task... tasks) {
        if (dependency == null) {
            for (final Task task : tasks) {
                LazyConfiguration.lazyConfiguration(task, new Runnable() {
                    @Override
                    public void run() {
                        throw new GradleException("Dependency project property not set. It is required for task '" + task.getPath() + "'.\n" +
                            "You can pass project property via command line: -Pdependency=\"org.shipkit:shipkit:1.2.3\"");
                    }
                });
            }
        }
    }
}
