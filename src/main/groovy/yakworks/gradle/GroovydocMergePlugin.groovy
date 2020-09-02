/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.javadoc.Groovydoc

/**
 * Merges together all the groovvydocs for shippable libs in a project
 */
@CompileStatic
class GroovydocMergePlugin implements Plugin<Project> {

    public static final String GROOVYDOC_MERGE_TASK = "groovydocMerge"

    //public static final String GIT_PUSH_TASK = "gitPublishPush" // ajoberstar's GitPublishPlugin.PUSH_TASK
    //public static final String TEST_RELEASE_TASK = "testRelease"

    @CompileDynamic
    void apply(Project rootProject) {
        if (rootProject.rootProject != rootProject) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        //ShipkitConfiguration conf = rootProject.plugins.apply(ShipkitConfigurationPlugin).configuration
        //do after groovy is applied to pubProjects above
        addGroovydocMergeTask(rootProject)
    }

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
                prj.plugins.hasPlugin(ShippablePlugin) && prj.plugins.hasPlugin('groovy') && !prj.plugins.hasPlugin(NotShippablePlugin)
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
            link('https://docs.groovy-lang.org/latest/html/gapi/', 'groovy.', 'org.codehaus.groovy.', 'org.apache.groovy.')
            link('http://gorm.grails.org/latest/hibernate/api/', 'grails.gorm.', 'grails.orm', 'org.grails.datastore.', 'org.grails.orm.')
            link('https://docs.grails.org/latest/api', 'grails.', 'org.grails.')
            link('https://testing.grails.org/latest/api', 'grails.testing.', 'org.grails.testing.')
        }
    }

}
