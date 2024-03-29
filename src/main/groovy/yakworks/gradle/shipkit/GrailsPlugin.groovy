/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ClosureBackedAction

import yakworks.gradle.CodenarcPlugin
import yakworks.gradle.NotShippablePlugin
import yakworks.gradle.ShippablePlugin

/**
 * A marker for a grails plugin, "yakworks.grails-plugin", will apply GrailsPluginPublishPlugin later after config
 */
@CompileStatic
class GrailsPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.rootProject.plugins.apply(ShipYakRootPlugin)
        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(CodenarcPlugin)
        //order is important here
        //ShippablePlugin/JavaLibraryPlugin has to come before the grails-plugin as it sets up the sourcesJar and javadocJar
        project.plugins.apply(JavaSourcesDocJarPlugin)
        project.plugins.apply("org.grails.grails-plugin")

        if(!project.plugins.hasPlugin(NotShippablePlugin)){
            //JavaPublishPlugin has to get applied after the grails-plugin has been applied or it doesn't add the dependencies to the pom properly
            project.plugins.apply(JavaPublishPlugin)
            //post processing cleanup
            cleanDepsInPom(project)
        }

    }

    /**
     * Taken from GrailsCentralPublishGradlePlugin in grails-core. its the 'org.grails.grails-plugin-publish'
     * Removes dependencies without versions and removes the bom dependencyManagement stuff
     */
    @SuppressWarnings('NestedBlockDepth')
    @CompileDynamic
    void cleanDepsInPom(Project project) {
        project.extensions.configure PublishingExtension, new ClosureBackedAction({
            publications {
                javaLibrary(MavenPublication) {
                    // artifact getGrailsPluginArtifact(project)
                    pom.withXml {
                        Node pomNode = asNode()
                        if (pomNode.dependencyManagement) {
                            pomNode.dependencyManagement[0].replaceNode {}
                        }
                        pomNode.dependencies.dependency.findAll {
                            it.version.text().isEmpty()
                        }.each {
                            it.replaceNode {}
                        }

                        //simce gradle 4.8 garbarge exclusions show up, get rid of them
                        pomNode.dependencies.dependency.exclusions.exclusion.findAll { Node dep ->
                            dep.artifactId.text() in ['grails-plugin-async', 'grails-plugin-events', 'grails-plugin-converters',
                                                      'grails-plugin-gsp', 'grails-plugin-testing', 'grails-datastore-simple']
                        }.each {
                            it.replaceNode {}
                        }
                        //now get ride of the empty exclusions
                        pomNode.dependencies.dependency.exclusions.findAll { Node dep ->
                            !dep.value()
                        }.each {
                            it.replaceNode {}
                        }
                    }
                }
            }
        })
    }

    @CompileDynamic
    Map<String, String> getGrailsPluginArtifact(Project project) {
        def directory
        try {
            directory = project.sourceSets.main.groovy.outputDir
        } catch (e) {
            directory = project.sourceSets.main.output.classesDir
        }
        [source: "${directory}/META-INF/grails-plugin.xml".toString(),
         classifier: "plugin",
         extension: 'xml']
    }
}
