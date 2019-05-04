/*
* Copyright 2019. Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin

/**
 * A marker for a grails plugin, "yakworks.grails-plugin", will apply GrailsPluginPublishPlugin later after config
 */
@CompileStatic
class GrailsPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        //order is important here
        //ShippablePlugin/JavaLibraryPlugin has to come before the grails-plugin as it sets up the sourcesJar and javadocJar
        project.plugins.apply(JavaLibraryPlugin)
        project.plugins.apply("org.grails.grails-plugin")
        //JavaPublishPlugin has to get applied after the grails-plugin has been applied or it doesn't add the dependencies to the pom properly
        project.plugins.apply(JavaPublishPlugin)
        project.rootProject.plugins.apply(MavenConfPlugin) //this should come last after JavaPublishPlugin as it finalizes the maven/bintray setups
    }
}
