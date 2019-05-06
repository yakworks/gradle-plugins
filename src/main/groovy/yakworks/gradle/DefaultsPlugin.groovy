/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
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

            addSpotless(prj)
        }

        rootProject.plugins.apply(DocmarkPlugin)

    }

    /**
     * Add the jcenter and mavenCentral repos as defaults
     */
    //@CompileDynamic
    void addDefaultRepos(Project prj) {
        //add our default repositories to search.
        RepositoryHandler rh = prj.repositories
        rh.jcenter()
        rh.mavenCentral()
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

    @CompileDynamic
    private void addSpotless(Project project) {
        SpotlessExtension spotless = project.plugins.apply(SpotlessPlugin).extension

        //make sure spotless runs first in the checks
        project.plugins.withId('codenarc') {
            project.tasks.getByName('codenarcMain').dependsOn('spotlessCheck')
        }

        spotlessFromConfig project, spotless, 'groovyGradle'

        project.plugins.withId('groovy') {
            spotlessFromConfig project, spotless, 'groovy'

            //broken out from normal groovy format so it doesn't try and add the license header
            spotless.format 'grailsConf', {
                target 'grails-app/conf/**/*.groovy'
                trimTrailingWhitespace()
                indentWithSpaces(4) // this only checks for tabs and can replace with 4 spaces it it finds them
                endWithNewline()
            }
        }
    }

    @CompileDynamic
    void spotlessFromConfig(Project project, SpotlessExtension spotless, String formatName){
        Map cfg = project.config.spotless[formatName]

        spotless."$formatName" {
            target project.fileTree('.') {
                cfg.includes.each{
                    include it
                }
                cfg.excludes.each{
                    exclude it
                }
            }
            if(cfg.endWithNewline) endWithNewline()
            if(cfg.trimTrailingWhitespace) trimTrailingWhitespace()
            if(cfg.indentWithSpaces) indentWithSpaces(cfg.indentWithSpaces)

            //if its a shippable item then makes sure a license header is applied (as opposed to an example or test project)
            project.plugins.withType(ShippablePlugin){
                if(cfg.licenseHeader){
                    licenseHeader(cfg.licenseHeader)
                } else if(cfg.licenseHeaderFile) {
                    licenseHeader(cfg.licenseHeaderFile)
                }
            }
        }
    }

}
