/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.diffplug.gradle.spotless.SpotlessExtension

@CompileStatic
class SpotlessYakPlugin implements Plugin<Project> {

    void apply(Project project) {
        addSpotless(project)
    }

    @CompileDynamic
    private void addSpotless(Project project) {
        SpotlessExtension spotless = project.plugins.apply(com.diffplug.gradle.spotless.SpotlessPlugin).extension

        //make sure spotless runs first in the checks
        project.plugins.withId('codenarc') {
            project.tasks.getByName('codenarcMain').dependsOn('spotlessCheck')
        }

        spotlessFromConfig project, spotless, 'groovyGradle'

        project.plugins.withId('groovy') {
            spotlessFromConfig project, spotless, 'groovy'
            spotlessFromConfig project, spotless, 'groovyTests'

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

        def clos = {
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
            if(cfg.importOrder) importOrder(*cfg.importOrder)

            /* if its a shippable library item then makes sure a license header is applied
            (as opposed to an example or test project)
            */
            project.plugins.withType(ShippablePlugin){
                if(cfg.licenseHeader){
                    licenseHeader(cfg.licenseHeader)
                } else if(cfg.licenseHeaderFile) {
                    licenseHeader(cfg.licenseHeaderFile)
                }
            }
        }

        if(formatName in ['groovy','groovyGradle']){
            spotless."$formatName" clos
        } else {
            spotless.format formatName, clos
        }

    }

}
