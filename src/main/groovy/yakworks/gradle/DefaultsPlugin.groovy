/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.javadoc.Javadoc

@CompileStatic
class DefaultsPlugin implements Plugin<Project> {

    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }

        //apply some helpful  default plugins
        //this is a wrapper on the normal IDEA plugin that make config better
        rootProject.plugins.apply("com.github.erdi.extended-idea")
        //rootProject.plugins.apply('com.dorongold.task-tree')

        rootProject.allprojects { Project prj ->

            prj.plugins.withId('java') {
                //this is for CI to cache dependencies see https://github.com/palantir/gradle-configuration-resolver-plugin
                prj.plugins.apply('com.palantir.configuration-resolver')

                addDefaultRepos(prj)
                silentJavadocWarnings(prj)
            }
            //add codenarc to groovy projects that are shippable
            prj.plugins.withType(ShippablePlugin){
                prj.plugins.withId('groovy') {
                    prj.plugins.apply(CodenarcPlugin)
                }
            }

            prj.plugins.apply(SpotlessYakPlugin)
        }

        rootProject.plugins.apply(DocmarkPlugin)

    }

    /**
     * Add the jcenter and mavenCentral repos as defaults
     * Also adds the epo.grails.org/grails/core if they are grails projects
     */
    @CompileDynamic
    void addDefaultRepos(Project prj) {
        //add our default repositories to search.
        RepositoryHandler rh = prj.repositories
        rh.jcenter()
        rh.mavenCentral()

        ['org.grails.grails-plugin', 'org.grails.grails-web'].each { plugId ->
            prj.plugins.withId(plugId) {
                rh.maven { url "https://repo.grails.org/grails/core" }
                rh.maven { url "https://dl.bintray.com/9ci/grails-plugins" }
            }
        }
    }

    /**
     * remove doclint warnings that pollute javadoc logs when building with java8
     */
    @CompileDynamic
    void silentJavadocWarnings(Project project) {
        if (JavaVersion.current().isJava8Compatible()) {
            project.tasks.withType(Javadoc) {
                options.addStringOption('Xdoclint:none', '-quiet')
            }
        }
    }
}
