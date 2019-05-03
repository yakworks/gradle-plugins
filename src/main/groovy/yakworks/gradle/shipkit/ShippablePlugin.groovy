/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        project.rootProject.plugins.apply(ShipkitPlugin)
    }

}
