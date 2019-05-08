/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.release.GradlePortalReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.gradle.DefaultsPlugin
import yakworks.gradle.ShippablePlugin

/**
 * yakworks.gradle-plugin , the shipkit one wont work as it depends on travis. This will work for circle.
 */
@CompileStatic
class GradlePluginPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        //TODO not sure this should be required
        ProjectUtil.requireRootProject(project, this.getClass())

        ConfigMap config = project.plugins.apply(YamlConfigShipYakPlugin).config

        project.plugins.apply(CiPublishPlugin)
        project.plugins.apply(DefaultsPlugin)
        project.plugins.apply(DocsReleasePlugin)

        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(GradlePortalReleasePlugin);
    }

}
