/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * since most of the plugins add the ShippablePlugin by default this allows
 * to add 'yakworks.not-shippable' to example and test app in a project while
 * getting the benefit of the main plugin.
 *
 */
@CompileDynamic
class NotShippablePlugin implements Plugin<Project> {
    private final static Logger log = Logging.getLogger(NotShippablePlugin)

    //ConfigMap config

    void apply(Project project) {
        //Tag it
        assert project
    }

}
