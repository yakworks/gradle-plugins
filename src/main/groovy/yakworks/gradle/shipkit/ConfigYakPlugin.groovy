package yakworks.gradle.shipkit;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ObjectConfigurationAction;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.init.InitPlugin;
import org.shipkit.internal.gradle.util.ProjectUtil;
import org.shipkit.internal.gradle.version.VersioningPlugin;
import org.shipkit.internal.util.PropertiesUtil;
import org.shipkit.version.VersionInfo;

import java.io.File;
import java.util.List;

/**
 * This should be the first one applied so it can setup the snapshot configuration and the yaml-config defaults
 */
public class ConfigYakPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(ConfigYakPlugin.class);

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //addSnaphotTaskFromVersionProp has to be done before ShipkitConfiguration, so it can add the snapshot task to the startParams
        addSnaphotTaskFromVersionProp(project)
        ShipkitConfiguration shipConfig = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration()

        ConfigObject conf = project.conf
        //git
        shipConfig.git.with {
            releasableBranchRegex = conf.git.releasableBranchRegex
            commitMessagePostfix = conf.git.commitMessagePostfix
            user = conf.git.config.user
            email = conf.git.config.email
        }

        //git
        shipConfig.gitHub.repository
        shipConfig.gitHub.with {
            repository = conf.github.fullName
            writeAuthToken = conf.github.writeAuthToken
            readOnlyAuthToken = conf.github.readOnlyAuthToken
        }

    }

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

}
