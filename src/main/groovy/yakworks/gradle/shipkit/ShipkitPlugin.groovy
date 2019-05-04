/*
* Copyright 2019. Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Use ShipyakPlugin
 */
@Deprecated
@CompileStatic
class ShipkitPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.plugins.apply(ShipyakPlugin)
    }

}
