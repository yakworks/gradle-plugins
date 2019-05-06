/*
* Copyright 2019. Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import com.jfrog.bintray.gradle.BintrayExtension
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
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

        //order is important here
        //ShippablePlugin/JavaLibraryPlugin has to come before the grails-plugin as it sets up the sourcesJar and javadocJar
        project.plugins.apply(JavaLibraryPlugin)
        project.plugins.apply("org.grails.grails-plugin")
        //JavaPublishPlugin has to get applied after the grails-plugin has been applied or it doesn't add the dependencies to the pom properly
        project.plugins.apply(JavaPublishPlugin)
        //this should come last after JavaPublishPlugin as it finalizes the maven/bintray setups
        project.rootProject.plugins.apply(PublishingRepoSetupPlugin)

        //post processing cleanup
        addGrailsPluginBintrayAttribute(project)
        cleanDepsInPom(project)

        addGrailsRepos(project)
    }

    @CompileDynamic
    static addGrailsRepos(Project project) {
        project.rootProject.allprojects { Project prj ->
            ['org.grails.grails-plugin', 'org.grails.grails-web'].each { plugId ->
                prj.plugins.withId(plugId) {
                    //add our default grails repositories to search.
                    RepositoryHandler rh = prj.repositories
                    rh.maven { url "https://repo.grails.org/grails/core" }
                    rh.maven { url "https://dl.bintray.com/9ci/grails-plugins" }
                }
            }
        }
    }

    /**
     * adds grails-plugin attibute to bintray pkg. not really sure if or why this is needed
     * @param project
     */
    //@CompileDynamic
    void addGrailsPluginBintrayAttribute(Project project) {
        project.plugins.withType(JavaBintrayPlugin) {
            project.afterEvaluate { prj ->
                final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension)
                bintray.pkg.version.attributes = ["grails-plugin": "${prj['group']}:${prj['name']}".toString()]
            }
        }
    }

    /**
     * Taken from GrailsCentralPublishGradlePlugin in grails-core. its the 'org.grails.grails-plugin-publish'
     * Cleans up dependencies without versions and removes the bom dependencyManagement stuff and adds the grails-plugin.xml artefact
     */
    @SuppressWarnings('NestedBlockDepth')
    @CompileDynamic
    void cleanDepsInPom(Project project) {
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction( {
                publications {
                    javaLibrary(MavenPublication) {
                        artifact getGrailsPluginArtifact(project)
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
                        }
                    }
                }
            })
        }
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
