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

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.javadoc.Groovydoc

import static ProjectUtils.searchProps
import static ProjectUtils.setPropIfEmpty

//@CompileStatic
class DefaultsPlugin implements Plugin<Project> {

    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        //setup defaults props
        setupProperties(rootProject)
        //apply idea plugin to root
        rootProject.plugins.apply('com.energizedwork.idea-project-components')
        addSpotless(rootProject)

        rootProject.allprojects { prj ->
            //this is for CI to cache dependencies see https://github.com/palantir/gradle-configuration-resolver-plugin
            prj.plugins.apply('com.palantir.configuration-resolver')
            //add our default repositories to search.
            RepositoryHandler rh = prj.repositories
            rh.jcenter()
            rh.mavenCentral()
            rh.maven { url "https://repo.grails.org/grails/core" }
            rh.maven { url "https://dl.bintray.com/9ci/grails-plugins"}
        }
        //gets all projects that don't start with ':examples' as they are considered "publishable"
        getPubSubprojects(rootProject).each { prj ->
            prj.plugins.apply('groovy')
            //addGrailsPublishConfig(prj)
            //addGroovydocDefaults(prj)
            addSpotless(prj)
            addCodenarc(prj)
        }

        //do after groovy is applied to pubProjects above
        addCombineGroovyDocsTask(rootProject)
        addMkdocsTasks(rootProject)
        addGitPublish(rootProject)
    }

    void setupProperties(Project prj) {
        // sets up default composed props on ext from base props in gradle.properties
        //!!!properties should go there, not here!!
        // its assumed that certain props exists already as base lines to use
        //** Github props used for both doc generation links, publishing docs to gh-pages and maven/bintray publish
        setPropIfEmpty prj, 'githubRepo', prj.name //defualts to project name
        setPropIfEmpty prj, 'githubSlug', "${prj['githubOrg']}/${prj['githubRepo']}".toString()
        setPropIfEmpty prj, 'githubUrl', "https://github.com/${prj['githubSlug']}".toString()
        setPropIfEmpty prj, 'githubIssues', "${prj['githubUrl']}/issues".toString()

        //** Publishing Bintray, Artifactory settings
        setPropIfEmpty prj, 'websiteUrl', "https://${prj['githubOrg']}.github.io/${prj['githubRepo']}".toString()
        setPropIfEmpty prj, 'isSnapshot', prj.version.toString().endsWith("-SNAPSHOT")
        setPropIfEmpty prj, 'bintrayOrg', prj.githubOrg

        setPropIfEmpty prj, 'artifactoryUrl', 'http://repo.9ci.com/oss-snapshots'
        setPropIfEmpty prj, 'artifactoryUser', searchProps(prj, "ARTIFACTORY_USER")
        setPropIfEmpty prj, 'artifactoryPassword', searchProps(prj, "ARTIFACTORY_PASSWORD")

        def devs = prj.findProperty('developers') ?: [nodev: "Lone Ranger"]
        devs = devs instanceof Map ? devs : new groovy.json.JsonSlurper().parseText(devs)
        setPropIfEmpty prj, 'pomDevelopers', devs

        //** Helpful dir params
        setPropIfEmpty prj, 'gradleDir', "${prj.rootDir}/gradle"
    }

    private void addMkdocsTasks(Project prj) {
        Task copyTask = prj.tasks.create('mkdocsCopy')
        copyTask.with {
            group = 'publishing'
            description = 'Copy contents to be published to git.'
            doLast {
                prj.copy {
                    from 'docs'
                    into 'build/mkdocs/docs'
                }
                //Copy the README in as index
                prj.copy {
                    from '.'
                    into "$prj.buildDir/mkdocs/docs"
                    include 'README.md'
                    rename { 'index.md' }
                    filter { line ->
                        //replace links like [foo]( docs/foo.md ) -> [foo](foo.md) and [foo]: docs/foo.md  -> [foo]: foo.md
                        def newl = line.replaceAll(/\(\s*docs\//, '(').replaceAll(/\:\s*docs\//, ': ')
                        return replaceVersionRegex(prj, newl)
                    }
                }
                //Copy the mkdocs.yml and replace the tokens
                prj.copy {
                    from '.'
                    into "$prj.buildDir/mkdocs"
                    include 'mkdocs.yml'
                    filter { line ->
                        String siteUrl = prj.isSnapshot ? "$prj.websiteUrl/snapshot" : prj.websiteUrl //prj.property('websiteUrl')
                        def newline = line.startsWith('repo_url:') ? "repo_url: ${prj.property('githubUrl')}" : line
                        newline.startsWith('site_url:') ? "site_url: $siteUrl" : newline
                    }
                    filter(ReplaceTokens,
                        tokens: ['version', 'title', 'description', 'githubSlug', 'author'].collectEntries {
                            [it, prj."$it".toString()]
                        }
                    )
                }
            }
        }

        Task mkdocsBuildTask = prj.task('mkdocsBuild', type: Exec)
        mkdocsBuildTask.with {
            dependsOn copyTask
            workingDir 'build/mkdocs'
            commandLine 'mkdocs', 'build'
        }
    }

    private void addGitPublish(Project project) {
        project.plugins.apply('org.ajoberstar.grgit')
        project.plugins.apply('org.ajoberstar.git-publish')

        project.gitPublish {
            branch = 'gh-pages'
            contents {
                from "$project.buildDir/mkdocs/site"
                if (project.property('isSnapshot')) { //put in own dir and update relative path
                    into 'snapshot'  //this sets the base relative dir for the rest of the inserts
                }
                from(project.groovydocMerge){ //project.groovydocMerge.outputs.files) {
                    into 'api'
                }
            }

            // (include=keep) if its a snapshot preserve is all, otherwise wipe it
            preserve {
                if (project.property('isSnapshot')) {
                    include "*/**"
                    exclude "snapshot"
                }
            }

            // message used when committing changes
            commitMessage = 'git-publish doc updates [skip ci]'
        }

        project.gitPublishCopy.dependsOn 'mkdocsBuild'
        project.gitPublishCopy.mustRunAfter 'mkdocsBuild'
        project.gitPublishCopy.inputs.files project.groovydocMerge //forces the copy task to depend on up to date groovydoc
    }

    /** Updates the version in the README.md */
    //https://stackoverflow.com/questions/17465353/how-to-replace-a-string-for-a-buildvariant-with-gradle-in-android-studio/17572644#17572644
    private void addUpdateReadmeVersions(Project prj, String fileName) {
        Task copyTask = prj.tasks.create('update-readme-versions')
        copyTask.with {
            doLast {
                //updates the Version: 6.1.x-whatever in the first yakworks logo part
                def updatedContent = new File('README.md').getText('UTF-8')
                updatedContent = replaceVersionRegex(prj, updatedContent)
                new File('README.md').write(updatedContent, 'UTF-8')
            }
        }
    }

    String replaceVersionRegex(Project prj, String content) {
        String updatedContent = content.replaceFirst(/(?i)version:\s*[\d\.]+[^\s]+/, "Version: $prj.version")
        //update any subproject dependencies examples, ie `gorm-tools:6.1.0-SNAPSHOT"`
        getPubSubprojects(prj).each { p ->
            updatedContent = updatedContent.replaceFirst(/${p.name}:[\d\.]+[^"]+/, "${p.name}:${prj.version.trim()}")
        }
        return updatedContent
    }


    private void addGroovydocDefaults(Project project) {
        project.plugins.withId('groovy') {
            groovydocLinks(project.tasks.groovydoc)
        }
    }

    private void addCombineGroovyDocsTask(Project rootProject) {
        //modeled from here https://github.com/nebula-plugins/gradle-aggregate-javadocs-plugin/blob/master/src/main/groovy/nebula/plugin/javadoc/NebulaAggregateJavadocPlugin.groovy
        //println "addCombineGroovyDocsTask"
        Set<Project> pubProjects = getPubSubprojects(rootProject)
        //println "addCombineGroovyDocsTask - " + pubProjects
        Task tsk = rootProject.task("groovydocMerge", type:Groovydoc , overwrite:true){
            description = 'Aggregates groovydoc API documentation .'
            //group = JavaBasePlugin.DOCUMENTATION_GROUP
            docTitle rootProject.version
            dependsOn pubProjects.groovydoc
            source pubProjects.groovydoc.source
            destinationDir rootProject.file("$rootProject.buildDir/docs/groovydoc")
            classpath = rootProject.files(pubProjects.groovydoc.classpath)
            groovyClasspath = rootProject.files(pubProjects.groovydoc.groovyClasspath)
        }
        groovydocLinks(tsk)
    }

    private void groovydocLinks(Task tsk) {
        tsk.with {
            noTimestamp = true
            noVersionStamp = true
            excludes = ['**/*GrailsPlugin.groovy', '**/Application.groovy']
            link('http://download.oracle.com/javase/8/docs/api/', 'java.', 'org.xml', 'javax.', 'org.xml.')
            link("https://docs.spring.io/spring/docs/4.2.x/javadoc-api/", 'org.springframework')
            link('http://groovy.codehaus.org/api/', 'groovy.', 'org.codehaus.groovy.')
            link('http://gorm.grails.org/latest/hibernate/api/', 'grails.gorm.', 'grails.orm', 'org.grails.datastore.', 'org.grails.orm.')
            link('https://docs.grails.org/latest/api', 'grails.', 'org.grails.')
            link('https://testing.grails.org/latest/api', 'grails.testing.', 'org.grails.testing.')
        }
    }


    private Set<Project> getPubSubprojects(Project rootProject) {
        rootProject.subprojects.findAll { prj -> !prj.path.startsWith(":examples") }
        //rootProject.allprojects.findAll { prj -> prj.plugins.hasPlugin(BintrayPlugin) }
    }

    private void addSpotless(Project project) {
        project.plugins.apply('com.diffplug.gradle.spotless')
        project.spotless {
            groovyGradle {
                target '**/*.gradle', 'build.gradle'
                trimTrailingWhitespace()
                indentWithSpaces(2)
                endWithNewline()
            }
            project.plugins.withId('groovy') {
                java {
                    //googleJavaFormat()
                    licenseHeader "/* Copyright \$YEAR. ${project.author}. Licensed under the Apache License, Version 2.0 */"
                    target project.fileTree('.') {
                        include 'src/main/groovy/gorm/**/*.java'
                    }
                }
                groovy {
                    target project.fileTree('.') {
                        include 'src/main/groovy/**/*.groovy', 'grails-app/**/*.groovy'
                        exclude '**/*.java', '**/conf/*.groovy'
                    }
                    licenseHeader "/* Copyright \$YEAR. ${project.author}. Licensed under the Apache License, Version 2.0 */"
                    trimTrailingWhitespace()
                    indentWithSpaces(4) // this only checks for tabs and can replace with 4 spaces it it finds them
                    endWithNewline()
                }
            }
        }
    }

    private void addCodenarc(Project prj) {
        prj.plugins.apply('codenarc')
        if(!prj.hasProperty('yakworks')) prj.ext.yakworks = [:]
        prj.yakworks.getCodenarcRuleSet = { ->
            return prj.resources.text.fromString(this.getClass().getResource('/codenarcRulesets.groovy').text)
        }

        prj.codenarc {
            toolVersion = '1.1'
            config = prj.yakworks.getCodenarcRuleSet()
            reportFormat = 'html'
            //ignoreFailures = true
            maxPriority1Violations = 0
            maxPriority2Violations = 4
            maxPriority3Violations = 4
        }
    }
}
