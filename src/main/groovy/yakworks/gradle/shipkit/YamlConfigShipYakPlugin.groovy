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

import yakworks.commons.ConfigMap
import yakworks.gradle.config.YamlConfigPlugin
import yakworks.gradle.util.ProjectUtil
import yakworks.gradle.util.PropertiesUtil

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
        def ymlplugin = project.plugins.apply(YamlConfigPlugin) as YamlConfigPlugin
        config = ymlplugin.config

        //sets the fullname repo from git if its null
        ConfigMap ghConfig = (ConfigMap)config.project
        String gslug = ghConfig.fullName

        //releaseNotes
        //setProps(shipConfig.releaseNotes, config['releaseNotes'])

        // if version.properties uses the publishedVersion instead of previousVersion then use that
        final File versionFile = project.file(VERSION_FILE_NAME)
        final YakVersionInfo versionInfo = YakVersionInfo.fromFile(versionFile)
        final String version = versionInfo.getVersion()
        prop(project, 'isSnapshot', versionInfo.isSnapshot)
        project.allprojects { Project prj ->
            prj.setVersion(version)
        }
        setupMavenPublishProps(project, config)
    }

    void setProps(Object pogo, Object cfgMap){
        ConfigMap curConfig = (ConfigMap)cfgMap
        InvokerHelper.setProperties(pogo, curConfig.evalAll().prune())
    }

    void setupMavenPublishProps(final Project prj, ConfigMap config){
        if(!config['maven.publishUrl']) config.merge('maven.publishUrl', config['maven.repoUrl'])

        if(prj['isSnapshot'] && config['maven.snapshotUrl']){
            config.merge('maven.publishUrl', config['maven.snapshotUrl'])
        }
    }

}
