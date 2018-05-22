package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import org.shipkit.internal.gradle.version.VersioningPlugin
import org.shipkit.internal.util.PropertiesUtil
import yakworks.groovy.ConfigMap
import yakworks.gradle.config.YamlConfigPlugin
import yakworks.groovy.Shell

import static yakworks.gradle.GradleHelpers.prop

/**
 * This should be the first one applied so it can setup the snapshot configuration and the yaml-config defaults
 */
@CompileStatic
public class ConfigYakPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(ConfigYakPlugin);

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //addSnaphotTaskFromVersionProp has to be done before ShipkitConfiguration, so it can add the snapshot task to the startParams
        addSnaphotTaskFromVersionProp(project)
        /* turn down the loggin for ShipkitConfigurationPlugin so it doesn't warn about no shipkit.gradle file
        Logger shipkitConfigLogger = LoggerFactory.getLogger("ShipkitConfigurationPlugin");
        Level oldLogLevel = shipkitConfigLogger.getLevel();
        shipkitConfigLogger.setLevel(Level.ERROR); // or whatever level you want

        // When you want to return to the old log level
        shipkitConfigLogger.setLevel(oldLogLevel);
         */
        ShipkitConfiguration shipConfig = project.plugins.apply(ShipkitConfigurationPlugin).configuration
        ConfigMap config = project.plugins.apply(YamlConfigPlugin).config

        //sets the fullname repo from git if its null
        String gslug = config['github.fullName']
        if (!gslug) {
            gslug = Shell.exec("git config --local remote.origin.url|sed -n 's#.*/\\(.*/[^.]*\\)\\.git#\\1#p'")
            config.merge('github.fullName', gslug)
        }
        //github
        shipConfig.gitHub.with {
            repository = config['github.fullName']
            writeAuthToken = config['github.writeAuthToken']
            readOnlyAuthToken = config['github.readOnlyAuthToken']
        }
        //git
        shipConfig.git.with {
            releasableBranchRegex = config['git.releasableBranchRegex']
            commitMessagePostfix = config['git.commitMessagePostfix']
            user = config['git.config.user'] //the use on the commits
            email = config['git.config.email']
        }
        //team
        shipConfig.team.with {
            developers = config['team.developers'] as Collection
            contributors = (Collection)config['team.contributors'] ?: contributors
            ignoredContributors = (Collection)config['team.ignoredContributors'] ?: ignoredContributors
        }
        //releaseNotes
        shipConfig.releaseNotes.with {
            file = config['releaseNotes.file'] ?: file
            ignoreCommitsContaining = (Collection)config['releaseNotes.ignoreCommitsContaining'] ?: ignoreCommitsContaining
            labelMapping = (Map)config['releaseNotes.labelMapping'] ?: labelMapping
        }

    }

    void addSnaphotTaskFromVersionProp(Project project) {
        final File versionFile = project.file(VersioningPlugin.VERSION_FILE_NAME);
        boolean bSnapshot = PropertiesUtil.readProperties(versionFile).getProperty("snapshot")?.toBoolean()
        List startTasks = project.gradle.startParameter.taskNames
        prop(project, 'isSnapshot', startTasks.contains('snapshot') || bSnapshot)

        boolean excludedTasks = startTasks.any { ['resolveConfigurations', 'clean'].contains(it)}

        if(prop(project, 'isSnapshot') && !excludedTasks) {
            startTasks.add(0, 'snapshot')
            project.gradle.startParameter.taskNames = startTasks
            LOG.lifecycle("  Snapshot set in versions file. Added snapshot task.")
            //println project.gradle.startParameter.taskNames
        }
    }

}
