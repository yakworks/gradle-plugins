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
 * A marker for a groovy library, "yakworks.ship.groovy"
 */
@CompileStatic
class GroovyLibPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(JavaPublishPlugin)
        project.rootProject.plugins.apply(MavenConfPlugin)//should come last after JavaPublishPlugin as it needs to have MavenPublishPlugin
    }
}