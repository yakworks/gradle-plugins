/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import yakworks.gradle.util.ProjectUtil

import yakworks.commons.ConfigMap
import yakworks.gradle.DefaultsPlugin

/**
 * Continuous delivery for Java/Groovy/Grails with CirclePlugin.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java/Groovy/Grails multi-project build.
 */
@CompileStatic
class ShipYakRootPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShipYakRootPlugin)

    ConfigMap config

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())

        def skplugin = project.plugins.apply(YamlConfigShipYakPlugin) as YamlConfigShipYakPlugin

        project.plugins.apply(DefaultsPlugin)

    }

}
