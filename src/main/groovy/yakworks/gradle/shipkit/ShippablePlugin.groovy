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

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.java.JavaPublishPlugin

/**
 * A base marker for any type of library or war that will be shippable/publishable. In other areas of gradle here
 * It will look for this marker to do certain things.
 */
@CompileStatic
class ShippablePlugin implements Plugin<Project> {

    void apply(Project project) {
        //make sure the ShipkitPlugin applied to the root. this will apply it to it self if this is a single
        //project and only the yakworks.grails-plugin is applied
        project.rootProject.plugins.apply(ShipkitPlugin)
    }
}
