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
 * A baseline marker for any type of library or war that will be shipped, published, released etc. as opposed to a testing project,
 * example project etc, where its ok to be less stringent in quality and standards.
 * Not to be confused with ShipYak or Shikit, this is more high level.
 * other areas or gradle plugins look for this marker to do certain things such as enforcing license header, groovydoc merging,
 * codenarc checking.
 *
 */
@CompileDynamic
class ShippablePlugin implements Plugin<Project> {
    private final static Logger log = Logging.getLogger(ShippablePlugin)

    //ConfigMap config

    void apply(Project project) {
        //Tag it
        assert 1==1
    }

}
