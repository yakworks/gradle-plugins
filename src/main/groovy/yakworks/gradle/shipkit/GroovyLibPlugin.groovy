/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

import yakworks.gradle.CodenarcPlugin
import yakworks.gradle.NotShippablePlugin
import yakworks.gradle.ShippablePlugin

/**
 * A marker for a groovy library, "yakworks.groovy-lib"
 */
@CompileStatic
class GroovyLibPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.rootProject.plugins.apply(ShipYakRootPlugin)
        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(CodenarcPlugin)
        project.plugins.apply(JavaSourcesDocJarPlugin)

        if(!project.plugins.hasPlugin(NotShippablePlugin)){
            //JavaPublishPlugin has to get applied after the grails-plugin has been applied or it doesn't add the dependencies to the pom properly
            project.plugins.apply(JavaPublishPlugin)
            //this should come last after JavaPublishPlugin as it finalizes the maven setups
            project.plugins.apply(PublishingRepoSetupPlugin)
            // project.rootProject.plugins.apply(PublishingRepoSetupPlugin)
        }
    }
}
