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
package yakworks.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin

/**
 * basic marker for a grails plugin, "yakworks.grails-plugin", will apply GrailsPluginPublishPlugin later after config
 */
//@CompileStatic
class GrailsPlugin implements Plugin<Project> {

    void apply(Project project) {
        //make sure the ShipkitPlugin applied to the root. this will apply it to it self if this is a single
        //project and only the yakworks.grails-plugin is applied
        project.rootProject.plugins.apply(ShipkitPlugin)
        project.plugins.apply('groovy')
        //this has to come before the grails-plugin as it sets up the sourcesJar and javadocJar
        project.plugins.apply(JavaLibraryPlugin)
        project.plugins.apply("org.grails.grails-plugin")
    }
}
