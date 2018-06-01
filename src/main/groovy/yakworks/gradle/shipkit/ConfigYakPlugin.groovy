package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.gradle.configuration.ShipkitConfiguration
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import org.shipkit.internal.gradle.version.VersioningPlugin
import org.shipkit.internal.util.PropertiesUtil
import yakworks.commons.ConfigMap
import yakworks.commons.Pogo
import yakworks.gradle.config.YamlConfigPlugin
import yakworks.commons.Shell

import static yakworks.gradle.GradleHelpers.prop

/**
 * Uses the yaml config plugin to setup shipkit and others
 * This should be the first one applied so it can setup the snapshot configuration and the yaml-config defaults
 *
 */
@CompileStatic
public class ConfigYakPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(ConfigYakPlugin);

    ConfigMap config

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
        config = project.plugins.apply(YamlConfigPlugin).config

        //sets the fullname repo from git if its null
        String gslug = config['github.fullName']
        if (!gslug) {
            String sedPart = $/sed -n 's#.*/\(.*/[^.]*\)\.git#\1#p'/$
            gslug = Shell.exec("git config --get remote.origin.url | $sedPart")
            //the sed above should have gotten back owner/repo
            config.merge('github.fullName', gslug)
        }

        //github
        setProps(shipConfig.gitHub, config['github'])
        shipConfig.gitHub.repository = config['github.fullName']

        //git
        setProps(shipConfig.git, config['git'])
        shipConfig.git.user = config['git.config.user']
        shipConfig.git.email = config['git.config.email']

        //team
        setProps(shipConfig.team, config['team'])

        //releaseNotes
        setProps(shipConfig.releaseNotes, config['releaseNotes'])

        setupMavenPublishProps(project, config)

    }

    void setProps(pogo, cfgMap){
        ConfigMap curConfig = (ConfigMap)cfgMap
        InvokerHelper.setProperties(pogo, curConfig.evalAll().prune())
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

    void setupMavenPublishProps(final Project prj, ConfigMap config){
        if(!config['maven.publishUrl']) config.merge('maven.publishUrl', config['maven.repoUrl'])

        if(prj['isSnapshot'] && config['maven.snapshotUrl']){
            config.merge('maven.publishUrl', config['maven.snapshotUrl'])
        }
    }

}
