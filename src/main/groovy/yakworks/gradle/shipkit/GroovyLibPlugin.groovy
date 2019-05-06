/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.java.JavaPublishPlugin
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
        project.plugins.apply(JavaPublishPlugin)
        project.rootProject.plugins.apply(PublishingRepoSetupPlugin)//should come last after JavaPublishPlugin as it needs to have MavenPublishPlugin
    }
}
