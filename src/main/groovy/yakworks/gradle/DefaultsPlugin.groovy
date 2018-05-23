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

import org.gradle.api.*
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.javadoc.Javadoc
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import static org.gradle.api.logging.LogLevel.*

import static GradleHelpers.searchProps
import static GradleHelpers.setPropIfEmpty

//@CompileStatic
class DefaultsPlugin implements Plugin<Project> {

    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        //final ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration()
        //setup defaults props
        setupProperties(rootProject)
        //apply default plugins
        rootProject.plugins.apply('com.energizedwork.idea-project-components')
        //rootProject.plugins.apply('com.dorongold.task-tree')

        rootProject.allprojects { prj ->
            prj.plugins.withId('java') {
                //this is for CI to cache dependencies see https://github.com/palantir/gradle-configuration-resolver-plugin
                prj.plugins.apply('com.palantir.configuration-resolver')

                //add our default repositories to search.
                RepositoryHandler rh = prj.repositories
                rh.jcenter()
                rh.mavenCentral()
                rh.maven { url "https://repo.grails.org/grails/core" }
                rh.maven { url "https://dl.bintray.com/9ci/grails-plugins"}

                silentJavadocWarnings(prj)

            }
            prj.plugins.withType(JavaLibraryPlugin){
                prj.plugins.withId('groovy') {
                    prj.plugins.apply(CodenarcPlugin)
                }
            }
            addSpotless(prj)
        }

        rootProject.plugins.apply(DocmarkPlugin)


    }

    /**
     * remove doclint warnings that pollute javadoc logs when building with java8
     */
    private void silentJavadocWarnings(Project project) {
        if (JavaVersion.current().isJava8Compatible()) {
            project.tasks.withType(Javadoc) {
                options.addStringOption('Xdoclint:none', '-quiet')
            }
        }
    }

    private void addSpotless(Project project) {
        project.plugins.apply('com.diffplug.gradle.spotless')

        project.plugins.withId('codenarc') {
//            Task spotlessCheck = project.tasks.getByName('spotlessCheck')
//            Task spotlessApply = project.tasks.getByName('spotlessApply')
            project.tasks.getByName('codenarcMain').dependsOn('spotlessCheck')
        }

        project.spotless.groovyGradle {
            target '**/*.gradle', 'build.gradle', 'gradle/*.gradle'
            trimTrailingWhitespace()
            indentWithSpaces(2)
            endWithNewline()
        }
        project.spotless.format 'grailsConf', {
            target 'grails-app/conf/**/*.groovy'
            trimTrailingWhitespace()
            indentWithSpaces(4) // this only checks for tabs and can replace with 4 spaces it it finds them
            endWithNewline()
        }

        project.plugins.withId('groovy') {
            //java {
                //googleJavaFormat()
            //    licenseHeader "/* Copyright \$YEAR. ${project.author}. Licensed under the Apache License, Version 2.0 */"
            //    target project.fileTree('.') {
            //        include 'src/main/groovy/gorm/**/*.java'
            //    }
            // }
            project.spotless.groovy {
                target project.fileTree('.') {
                    include 'src/main/groovy/**/*.groovy', 'grails-app/**/*.groovy',
                        'src/test/groovy/**/*.groovy', 'src/integration-test/groovy/**/*.groovy'
                    exclude '**/*.java', '**/conf/**/*.groovy'
                }
                trimTrailingWhitespace()
                indentWithSpaces(4) // this only checks for tabs and can replace with 4 spaces it it finds them
                endWithNewline()
            }
            project.plugins.withType(JavaLibraryPlugin){
                if(project.findProperty('licenseHeader')) project.spotless.formats.groovy.licenseHeader(project.licenseHeader)
                if(project.findProperty('licenseHeaderFile')) project.spotless.formats.groovy.licenseHeaderFile(project.licenseHeaderFile)
            }
        }
    }


}
