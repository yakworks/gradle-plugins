/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

import yakworks.commons.ConfigMap
import yakworks.gradle.CodenarcPlugin
import yakworks.gradle.DefaultsPlugin
import yakworks.gradle.ShippablePlugin
import yakworks.gradle.util.ProjectUtil

/**
 * yakworks.gradle-plugin, the shipkit one wont work as it depends on travis. This will work for circle.
 */
@CompileStatic
class GradlePluginPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        //TODO not sure this should be required
        ProjectUtil.requireRootProject(project, this.getClass())

        YamlConfigShipYakPlugin ycplugin = project.plugins.apply(YamlConfigShipYakPlugin) as YamlConfigShipYakPlugin
        ConfigMap config = ycplugin.config

        // project.plugins.apply(CiPublishPlugin)
        project.plugins.apply(DefaultsPlugin)
        // project.plugins.apply(DocsReleasePlugin)

        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(CodenarcPlugin)

        project.plugins.apply(JavaPublishPlugin)
        // project.plugins.apply(PublishingRepoSetupPlugin)

    }

}
