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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.javadoc.Javadoc
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import yakworks.commons.ConfigMap

@CompileStatic
class DefaultsPlugin implements Plugin<Project> {

    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }

        //setup defaults props

        //apply default plugins
        rootProject.plugins.apply('com.energizedwork.idea-project-components')
        //rootProject.plugins.apply('com.dorongold.task-tree')

        rootProject.allprojects { Project prj ->
            prj.plugins.withId('java') {
                //this is for CI to cache dependencies see https://github.com/palantir/gradle-configuration-resolver-plugin
                prj.plugins.apply('com.palantir.configuration-resolver')

                addDefaultRepos(prj)

                silentJavadocWarnings(prj)

            }
            //if its a groovy library then add codeNarc in
            prj.plugins.withType(JavaLibraryPlugin){
                prj.plugins.withId('groovy') {
                    prj.plugins.apply(CodenarcPlugin)
                }
            }
            addSpotless(prj)
        }

        rootProject.plugins.apply(DocmarkPlugin)

    }

    @CompileDynamic
    void addDefaultRepos(Project prj) {
        //add our default repositories to search.
        RepositoryHandler rh = prj.repositories
        rh.jcenter()
        rh.mavenCentral()
        rh.maven { url "https://repo.grails.org/grails/core" }
        rh.maven { url "https://dl.bintray.com/9ci/grails-plugins"}
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

            //if it has both the library plugin applied and it has the header specified
            project.plugins.withType(JavaLibraryPlugin){
                if(cfg.licenseHeader){
                    licenseHeader(cfg.licenseHeader)
                } else if(cfg.licenseHeaderFile) {
                    licenseHeader(cfg.licenseHeaderFile)
                }
            }
        }
    }

}
