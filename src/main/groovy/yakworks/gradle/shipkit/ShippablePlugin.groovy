/*
* Copyright 2019. Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * A base marker for any type of library or war that will be shippable/publishable. In other areas of gradle here
 * other areas look for this marker to do certain things.
 */
@CompileDynamic
class ShippablePlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShippablePlugin)

    //ConfigMap config

    void apply(Project project) {
        project.rootProject.plugins.apply(ShipyakPlugin)
    }

}
