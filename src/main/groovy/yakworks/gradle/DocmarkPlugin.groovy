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
import org.gradle.api.*
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.javadoc.Javadoc
import org.shipkit.internal.gradle.java.JavaLibraryPlugin

import static ProjectUtils.searchProps
import static ProjectUtils.setPropIfEmpty

//@CompileStatic
class DocmarkPlugin implements Plugin<Project> {

    public static final String UPDATE_README_TASK = "updateReadmeVersions"
    public static final String TEST_RELEASE_TASK = "testRelease"

    void apply(Project rootProject) {
        //do after groovy is applied to pubProjects above
        addCombineGroovyDocsTask(rootProject)
        addMkdocsTasks(rootProject)
        addGitPublish(rootProject)
        addUpdateReadmeVersions(rootProject)
    }

    //this should be called from inside of an afterEvaluate so the JavaLibraryPlugin will have been applied properly to subs
    private Set<Project> getLibraryProjects(Project rootProject) {
        rootProject.allprojects.findAll { prj ->
            //println "${prj.path} hasPlugin JavaLibraryPlugin " + prj.plugins.hasPlugin(JavaLibraryPlugin)
            prj.plugins.hasPlugin(JavaLibraryPlugin)
        }
        //rootProject.allprojects.findAll { prj -> prj.plugins.hasPlugin(BintrayPlugin) }
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
            commitMessage = "publish ${project.version} doc updates [skip ci]"
        }

        project.gitPublishCopy.dependsOn 'mkdocsBuild'
        project.gitPublishCopy.mustRunAfter 'mkdocsBuild'
        project.gitPublishCopy.inputs.files project.groovydocMerge //forces the copy task to depend on up to date groovydoc
    }

    /** Updates the version in the README.md */
    //https://stackoverflow.com/questions/17465353/how-to-replace-a-string-for-a-buildvariant-with-gradle-in-android-studio/17572644#17572644
    private void addUpdateReadmeVersions(Project rootProject) {
        Task copyTask = rootProject.tasks.create(UPDATE_README_TASK)
        copyTask.doLast {
            //updates the Version: 6.1.x-whatever in the first yakworks logo part
            def updatedContent = rootProject.file('README.md').getText('UTF-8')
            updatedContent = replaceVersionRegex(rootProject, updatedContent)
            rootProject.file('README.md').write(updatedContent, 'UTF-8')
        }
    }

    String replaceVersionRegex(Project prj, String content) {
        String updatedContent = content.replaceFirst(/(?i)version:\s*[\d\.]+[^\s]+/, "Version: $prj.version")
        //update any subproject dependencies examples, ie `gorm-tools:6.1.0-SNAPSHOT"`
        getLibraryProjects(prj).each { p ->
            updatedContent = updatedContent.replaceFirst(/${p.name}:[\d\.]+[^"]+/, "${p.name}:${prj.version.trim()}")
        }
        return updatedContent
    }

//    private void addGroovydocDefaults(Project project) {
//        project.plugins.withId('groovy') {
//            groovydocLinks(project.tasks.groovydoc)
//        }
//    }

    private void addCombineGroovyDocsTask(Project project) {
        //modeled from here https://github.com/nebula-plugins/gradle-aggregate-javadocs-plugin/blob/master/src/main/groovy/nebula/plugin/javadoc/NebulaAggregateJavadocPlugin.groovy
        //println "addCombineGroovyDocsTask"
        Task tsk = project.task("groovydocMerge", type: Groovydoc, overwrite: true)
        project.gradle.projectsEvaluated {
            Set<Project> pubProjects = getLibraryProjects(project)
            //println "groovydocMerge task for - " + pubProjects
            tsk.with {
                description = 'Aggregates groovydoc API documentation .'
                //group = JavaBasePlugin.DOCUMENTATION_GROUP
                docTitle project.version
                dependsOn pubProjects.groovydoc
                source pubProjects.groovydoc.source
                destinationDir project.file("$project.buildDir/docs/groovydoc")
                classpath = project.files(pubProjects.groovydoc.classpath)
                groovyClasspath = project.files(pubProjects.groovydoc.groovyClasspath)
            }
            groovydocLinks(tsk)
        }
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

}
