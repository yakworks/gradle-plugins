/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.javadoc.Groovydoc
import yakworks.commons.ConfigMap

/**
 * Tasks and configuration for generating mkdocs and groovydocs.
 */
@CompileStatic
class DocmarkPlugin implements Plugin<Project> {

    public static final String UPDATE_README_TASK = "updateReadmeVersions"
    public static final String MKDOCS_COPY_TASK = "mkdocsPrepare"
    public static final String MKDOCS_BUILD_TASK = "mkdocsBuild"
    public static final String PUBLISH_DOCS_TASK = "publishDocs"
    public static final String GROOVYDOC_MERGE_TASK = "groovydocMerge"

    public static final String GIT_PUSH_TASK = "gitPublishPush" // ajoberstar's GitPublishPlugin.PUSH_TASK
    //public static final String TEST_RELEASE_TASK = "testRelease"

    @CompileDynamic
    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        //ShipkitConfiguration conf = rootProject.plugins.apply(ShipkitConfigurationPlugin).configuration
        //do after groovy is applied to pubProjects above
        addGroovydocMergeTask(rootProject)

        //boolean enableDocsPublish = rootProject.hasProperty('enableDocsPublish')? Boolean.valueOf(rootProject['enableDocsPublish'].toString()) : true
        boolean enableDocsPublish = rootProject.config['docs.enabled']
        if(rootProject.file('mkdocs.yml').exists() && enableDocsPublish) {
            addMkdocsTasks(rootProject)
            addGitPublish(rootProject)
        }

        addUpdateReadmeVersions(rootProject)

        Task pubDocsTask = rootProject.tasks.create(PUBLISH_DOCS_TASK)
        pubDocsTask.with {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = 'Builds and publishes docs to github'
            dependsOn GIT_PUSH_TASK
        }
    }

    //this should be called from inside of an afterEvaluate so the JavaLibraryPlugin will have been applied properly to subs
    private Set<Project> getShippableProjects(Project rootProject) {
        rootProject.allprojects.findAll { prj ->
            //println "${prj.path} hasPlugin JavaLibraryPlugin " + prj.plugins.hasPlugin(JavaLibraryPlugin)
            prj.plugins.hasPlugin(ShippablePlugin)
        }
        //rootProject.allprojects.findAll { prj -> prj.plugins.hasPlugin(BintrayPlugin) }
    }

    @CompileDynamic
    private void addMkdocsTasks(Project prj) {
        ConfigMap config = prj.config
        Task copyTask = prj.tasks.create(MKDOCS_COPY_TASK)
        copyTask.with {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = 'Copy docs to build and run replace filters to update tokens. Called by mkdocsBuild'
            doLast {
                prj.copy {
                    from 'docs'
                    into "$prj.buildDir/mkdocs/docs"
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
                        String siteUrl = prj.isSnapshot ? "$config.websiteUrl/snapshot" : config.websiteUrl //prj.property('websiteUrl')
                        def newline = line.startsWith('repo_url:') ? "repo_url: ${config.github.repoUrl}" : line
                        newline.startsWith('site_url:') ? "site_url: $siteUrl" : newline
                    }
                    def tokens = [version: prj.version, title: config.title, description: prj.description,
                                  author: config.author, gitHubSlug: config.github.fullName, ]
                    filter(ReplaceTokens, tokens: tokens)
                }
            }
        }

        Task mkdocsBuildTask = prj.task(MKDOCS_BUILD_TASK, type: Exec) {
            group = 'documentation'
            description = 'prepares docs and runs the mkdocs'
            dependsOn copyTask
            workingDir "$prj.buildDir/mkdocs"
            commandLine 'mkdocs', 'build'
        }
    }

    @CompileDynamic
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
            commitMessage = "Docs published for ${project.version} release [skip ci]".toString()
        }

        project.gitPublishCopy.dependsOn MKDOCS_BUILD_TASK
        project.gitPublishCopy.mustRunAfter MKDOCS_BUILD_TASK
        project.gitPublishCopy.inputs.files project.groovydocMerge //forces the copy task to depend on up to date groovydoc
    }

    /** Updates the version in the README.md */
    //https://stackoverflow.com/questions/17465353/how-to-replace-a-string-for-a-buildvariant-with-gradle-in-android-studio/17572644#17572644
    private void addUpdateReadmeVersions(Project rootProject) {
        Task copyTask = rootProject.tasks.create(UPDATE_README_TASK)
        copyTask.with {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = 'Updates the README with version token'
            doLast {
                //updates the Version: 6.1.x-whatever in the first yakworks logo part
                def updatedContent = rootProject.file('README.md').getText('UTF-8')
                updatedContent = replaceVersionRegex(rootProject, updatedContent)
                rootProject.file('README.md').write(updatedContent, 'UTF-8')
            }
        }
    }

    String replaceVersionRegex(Project prj, String content) {
        String version = (prj.version as String).trim()
        String updatedContent = content.replaceFirst(/(?i)version:\s*[\d\.]+[^\s]+/, "Version: $version")
        //VersionInfo info = project.getExtensions().getByType(VersionInfo.class)
        getShippableProjects(prj).each { p ->
            //update any subproject dependencies examples, ie `gorm-tools:6.1.0"`
            updatedContent = updatedContent.replaceFirst(/${p.name}:[\d\.]+[^"]+/, "${p.name}:$version")
            //update any dependencies for plugin style versions, ie `id "yakworks.gorm-tools" version "1.2.3"`
            updatedContent = updatedContent.replaceFirst(/(?i)${p.name}"\sversion\s"[\d\.]+[^\s]+"/, "${p.name}\" version \"$version\"")
        }
        return updatedContent
    }

//    private void addGroovydocDefaults(Project project) {
//        project.plugins.withId('groovy') {
//            groovydocLinks(project.tasks.groovydoc)
//        }
//    }

    /**
     * Adds the groovydocMerge task to meld together all publishable source sets into single docs
     */
    @CompileDynamic
    private void addGroovydocMergeTask(Project project) {
        /* modeled from here:
            https://github.com/nebula-plugins/gradle-aggregate-javadocs-plugin/blob/
            master/src/main/groovy/nebula/plugin/javadoc/NebulaAggregateJavadocPlugin.groovy*/
        //println "addCombineGroovyDocsTask"
        Task tsk = project.task(GROOVYDOC_MERGE_TASK, type: Groovydoc, overwrite: true)
        //do it after entire project evaluated so
        project.gradle.projectsEvaluated {
            Set<Project> pubProjects = project.allprojects.findAll { prj ->
                prj.plugins.hasPlugin(ShippablePlugin) && prj.plugins.hasPlugin('groovy')
            }
            //println "groovydocMerge task for - " + pubProjects
            tsk.with {
                description = 'Aggregates groovydoc API documentation .'
                group = JavaBasePlugin.DOCUMENTATION_GROUP
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

    /**
     * updates the groovydoc links so they are clickable into external docs
     */
    @CompileDynamic
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
