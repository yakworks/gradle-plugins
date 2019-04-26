package yakworks.gradle.shipkit;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.shipkit.gradle.git.IdentifyGitBranchTask;
import org.shipkit.gradle.release.ReleaseNeededTask;
import org.shipkit.internal.gradle.configuration.BasicValidator;
import org.shipkit.internal.gradle.configuration.LazyConfiguration;
import org.shipkit.internal.gradle.git.GitBranchPlugin;
import org.shipkit.internal.gradle.git.GitSetupPlugin;
import org.shipkit.internal.gradle.git.tasks.GitCheckOutTask;
import org.shipkit.internal.gradle.util.StringUtil;
import yakworks.commons.Shell;

/**
 * Configures the release automation to be used with Circle CI.
 * Intended for root project.
 * Copied the core logic from the TravisPlugin class.
 */
public class CirclePlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(CirclePlugin.class);

    @Override
    public void apply(final Project project) {
        //project.getPlugins().apply(CiReleasePlugin.class);

        final String branch = System.getenv("CIRCLE_BRANCH");
        LOG.info("Branch from 'CIRCLE_BRANCH' env variable: {}", branch);

        //configure branch based on env variable
        IdentifyGitBranchTask identifyBranch = (IdentifyGitBranchTask) project.getTasks().getByName(GitBranchPlugin.IDENTIFY_GIT_BRANCH);
        if (!StringUtil.isEmpty(branch)) {
            identifyBranch.setBranch(branch);
        }

        //set the branch to be checked out on ci build
        final GitCheckOutTask checkout = (GitCheckOutTask) project.getTasks().getByName(GitSetupPlugin.CHECKOUT_TASK);
        checkout.setRev(branch);
        LazyConfiguration.lazyConfiguration(checkout, new Runnable() {
            public void run() {
                BasicValidator.notNull(checkout.getRev(),
                    "Task " + checkout.getPath() + " does not know the target revision to check out.\n" +
                        "In Circle CI builds, it is automatically configured from 'CIRCLE_BRANCH' environment variable.\n" +
                        "If you are trying to run this task outside Circle, you can export the environment variable.\n" +
                        "Alternatively, you can set the task's 'rev' property explicitly.");
            }
        });

        //update release needed task based on Travis' env variables
        String pr = System.getenv("CIRCLE_PULL_REQUEST");
        LOG.info("Pull request from 'CIRCLE_PULL_REQUEST' env variable: {}", pr);
        final boolean isPullRequest = pr != null && !pr.trim().isEmpty() && !pr.equals("false");
        LOG.info("Pull request build: {}", isPullRequest);

        String commitMessage = Shell.exec("git log --format=\"%s\" -n 1 $CIRCLE_SHA1");
        project.getTasks().withType(ReleaseNeededTask.class, new Action<ReleaseNeededTask>() {
            public void execute(ReleaseNeededTask t) {
                t.setCommitMessage(commitMessage);
                t.setPullRequest(isPullRequest);
            }
        });
    }
}
