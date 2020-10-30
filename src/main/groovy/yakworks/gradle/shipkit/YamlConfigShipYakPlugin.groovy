/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
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
import yakworks.commons.Shell
import yakworks.gradle.config.YamlConfigPlugin

import static yakworks.gradle.GradleHelpers.prop

/**
 * Uses the yaml config plugin to setup shipkit and others gradle plugins
 * This should be the first one applied so it can setup the snapshot configuration and the yaml-config defaults
 */
@CompileStatic
public class YamlConfigShipYakPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(YamlConfigShipYakPlugin)
    static final String VERSION_FILE_NAME = "version.properties"

    ConfigMap config

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //addSnaphotTaskFromVersionProp has to be done before ShipkitConfiguration, so it can add the snapshot task to the startParams
        //addSnaphotTaskFromVersionProp(project)
        /* turn down the loggin for ShipkitConfigurationPlugin so it doesn't warn about no shipkit.gradle file
        Logger shipkitConfigLogger = LoggerFactory.getLogger("ShipkitConfigurationPlugin");
        Level oldLogLevel = shipkitConfigLogger.getLevel();
        shipkitConfigLogger.setLevel(Level.ERROR); // or whatever level you want

        // When you want to return to the old log level
        shipkitConfigLogger.setLevel(oldLogLevel);
         */
        def skplugin = project.plugins.apply(ShipkitConfigurationPlugin) as ShipkitConfigurationPlugin
        ShipkitConfiguration shipConfig = skplugin.configuration
        def ymlplugin = project.plugins.apply(YamlConfigPlugin) as YamlConfigPlugin
        config = ymlplugin.config

        //sets the fullname repo from git if its null
        ConfigMap ghConfig = (ConfigMap)config.github
        String gslug = ghConfig.fullName
        //LOG.lifecycle("github.fullName is $gslug")
        if (!gslug) {
            //println "gslug was null so using sed to get config from git"
            String sedPart = $/sed -n 's#.*/\(.*/[^.]*\)\.git#\1#p'/$
            String getRemote = "git config --get remote.origin.url | $sedPart"
            gslug = Shell.exec(getRemote)
            //the sed above should have gotten back owner/repo
            config.merge('github.fullName', gslug)
            LOG.lifecycle("github.fullName was not set so getting it from $getRemote \n" +
                "ghConfig.fullName : ${ghConfig.fullName} , " +
                "gslug from cmd is $gslug")
        }

        //github
        setProps(shipConfig.gitHub, ghConfig)
        //println "ghConfig $ghConfig"
        shipConfig.gitHub.repository = ghConfig.fullName
        //println "gitHubConfig['readOnlyAuthToken'] ${ghConfig.readOnlyAuthToken}"
        //println "gitHubConfig['writeAuthToken'] ${ghConfig.writeAuthToken}"

        //git
        setProps(shipConfig.git, config['git'])
        shipConfig.git.user = config['git.config.user']
        shipConfig.git.email = config['git.config.email']

        //team
        setProps(shipConfig.team, config['team'])

        //releaseNotes
        setProps(shipConfig.releaseNotes, config['releaseNotes'])

        // if version.properties uses the publishedVersion instead of previousVersion then use that
        final File versionFile = project.file(VERSION_FILE_NAME)
        final YakVersionInfo versionInfo = YakVersionInfo.fromFile(versionFile)
        final String version = versionInfo.getVersion()
        prop(project, 'isSnapshot', versionInfo.isSnapshot)
        project.allprojects { Project prj ->
            prj.setVersion(version)
        }
        if(versionInfo.previousVersion){
            shipConfig.setPreviousReleaseVersion(versionInfo.previousVersion)
        }
        setupMavenPublishProps(project, config)
    }

    void setProps(Object pogo, Object cfgMap){
        ConfigMap curConfig = (ConfigMap)cfgMap
        InvokerHelper.setProperties(pogo, curConfig.evalAll().prune())
    }

    void addSnaphotTaskFromVersionProp(Project project) {
        final File versionFile = project.file(VersioningPlugin.VERSION_FILE_NAME);
        boolean bSnapshot = PropertiesUtil.readProperties(versionFile).getProperty("snapshot")?.toBoolean()
        List startTasks = project.gradle.startParameter.taskNames
        prop(project, 'isSnapshot', startTasks.contains('snapshot') || bSnapshot)

        boolean excludedTasks = startTasks.any { ['resolveConfigurations', 'clean'].contains(it) }

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
