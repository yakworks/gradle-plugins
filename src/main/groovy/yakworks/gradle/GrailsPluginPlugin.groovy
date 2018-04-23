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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.java.JavaLibraryPlugin

//@CompileStatic
class GrailsPluginPlugin implements Plugin<Project> {

    void apply(Project project) {
        //setup defaults props
        //setupProperties(project)
        project.plugins.apply('groovy')
        project.plugins.apply(JavaLibraryPlugin)
        project.plugins.apply("org.grails.grails-plugin")
        //project.plugins.apply('org.grails.grails-plugin')
        //addGrailsPublishConfig(project)
    }

    private void addGrailsPublishConfig(Project project) {
        project.plugins.withId('org.grails.grails-plugin') {
            project.plugins.apply('org.grails.grails-plugin-publish')

            project.grailsPublish {
                title = project.title
                desc = project.description
                userOrg = project.bintrayOrg
                repo = project.bintrayRepo
                developers = project.pomDevelopers
                githubSlug = project.githubSlug
                issueTrackerUrl = project.githubIssues
                websiteUrl = project.websiteUrl
                vcsUrl = project.githubUrl
            }

            if (project.findProperty('isSnapshot') || !project.findProperty('bintrayRepo')) {
                //for snapshots setup the normal repo way, this will enable the 'publish' task
                project.publishing {
                    //println "SNAPSHOT - " + project.name + ":" + project.version.toString()
                    repositories {
                        maven {
                            url project.artifactoryUrl
                            credentials {
                                username project.artifactoryUser
                                password project.artifactoryPassword
                            }
                        }
                    }
                }
            }
//            else {
//                project.task("publish", dependsOn:'bintrayUpload' , overwrite:true){
//                    description = 'convienince to call bintrayUpload'
//                    group = 'publishing'
//                }
//            }
        }
    }
}
