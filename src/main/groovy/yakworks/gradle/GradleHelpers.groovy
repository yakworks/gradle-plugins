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

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class GradleHelpers {

    static boolean bool(Project prj, String propertyName, boolean defaultReturn = false){
        def p = prop(prj, propertyName)
        if(p == null) return defaultReturn
        if (p instanceof String) return p.toBoolean()
        return p.asBoolean()
    }
    //returns the prop using findProperty so it doesn't thrown an error
    static prop(Project prj, String propertyName) {
        return prj.findProperty(propertyName)
    }

    //sets the prop or creates it on ext if not found
    @CompileDynamic
    static void prop(Project prj, String propertyName, val) {
        //if it has the property then set it.
        if(prj.hasProperty(propertyName)){
            prj.setProperty(propertyName, val)
        } else { //add it
            ext(prj)[propertyName] = val
        }
    }

    @CompileDynamic
    static void setPropIfEmpty(Project prj, String propertyName, defaultValue) {
        if (!prj.findProperty(propertyName) && defaultValue != null) {
            prop(prj, propertyName, defaultValue)
        }
    }

    /**
     * Genrally you'll pass in an environment var like SOME_PROP.
     * look in System.env then System.properties, convert to camelCase and findProperty
     * @return the prop or null
     */
    static def searchProps(Project prj, String prop) {
        if (System.getenv(prop) != null) {
            return System.getenv(prop)
        } else if (System.properties[prop]) {
            return System.properties[prop]
        } else {
            return prj.findProperty(toCamelCase(prop)) //returns null if not found
        }
    }

    //converts SOME_PROP env prop name to someProp
    static String toCamelCase( String text ) {
        text = text.toLowerCase().replaceAll( "(_)([A-Za-z0-9])", { List<String> it -> it[2].toUpperCase() } )
        //println text
        return text
    }

    static String shExecute(String command){
        return ['sh', '-c', command].execute().text.trim()
    }

    static void setGitDefaultsProps(Project prj) {
        // sets up default composed props on ext from base props in gradle.properties
        //!!!properties should go there, not here!!
        // its assumed that certain props exists already as base lines to use
        //** Github props used for both doc generation links, publishing docs to gh-pages and maven/bintray publish
        String gslug = prj.findProperty("gitHubSlug")
        if (!gslug) gslug = GradleHelpers.shExecute("git config --local remote.origin.url|sed -n 's#.*/\\(.*/[^.]*\\)\\.git#\\1#p'")
        if (gslug) {
            def repoAndOrg = gslug.split("/")
            setPropIfEmpty prj, 'gitHubOrg', repoAndOrg[0]
            setPropIfEmpty prj, 'gitHubRepo', repoAndOrg[1]
        }
        setPropIfEmpty prj, 'gitHubRepo',   prj.name //defualts to project name
        setPropIfEmpty prj, 'gitHubSlug',   "${prj['gitHubOrg']}/${prj['gitHubRepo']}".toString()
        setPropIfEmpty prj, 'gitHubUrl',    "https://github.com/${prj['gitHubSlug']}".toString()
        setPropIfEmpty prj, 'gitHubIssues', "${prj['gitHubUrl']}/issues".toString()
        setPropIfEmpty prj, 'websiteUrl',   "https://${prj['gitHubOrg']}.github.io/${prj['gitHubRepo']}".toString()
    }

    static void setupPublishProps(Project prj) {
        // sets up default composed props on ext from base props in gradle.properties
        //!!!properties should go there, not here!!
        // its assumed that certain props exists already as base lines to use
        //** Github props used for both doc generation links, publishing docs to gh-pages and maven/bintray publish
        String gslug = prj.findProperty("gitHubSlug")
        if(!gslug) gslug = GradleHelpers.shExecute("git config --local remote.origin.url|sed -n 's#.*/\\(.*/[^.]*\\)\\.git#\\1#p'")
        if (gslug){
            def repoAndOrg = gslug.split("/")
            setPropIfEmpty prj, 'gitHubOrg', repoAndOrg[0]
            setPropIfEmpty prj, 'gitHubRepo', repoAndOrg[1]
        }
        setPropIfEmpty prj, 'gitHubRepo', prj.name //defualts to project name
        setPropIfEmpty prj, 'gitHubSlug', "${prj['gitHubOrg']}/${prj['gitHubRepo']}".toString()
        setPropIfEmpty prj, 'gitHubUrl', "https://github.com/${prj['gitHubSlug']}".toString()
        setPropIfEmpty prj, 'gitHubIssues', "${prj['gitHubUrl']}/issues".toString()

        //** Publishing Bintray, Artifactory settings
        setPropIfEmpty prj, 'websiteUrl', "https://${prj['gitHubOrg']}.github.io/${prj['gitHubRepo']}".toString()
        setPropIfEmpty prj, 'bintrayOrg', prj['gitHubOrg']

        setPropIfEmpty prj, 'isSnapshot', prj.version.toString().endsWith("-SNAPSHOT")
        setPropIfEmpty prj, 'isBintrayPublish', (prj.findProperty('bintrayRepo')?:false)

        //***Maven publish
        setPropIfEmpty prj, 'mavenRepoUrl', 'http://repo.9ci.com/grails-plugins' //'http://repo.9ci.com/oss-snapshots'
        setPropIfEmpty prj, 'mavenPublishUrl', prj['mavenRepoUrl']

        def mu = searchProps(prj, "MAVEN_REPO_USER")?:""
        def mk = searchProps(prj, "MAVEN_REPO_KEY")?:""
        setPropIfEmpty(prj, 'mavenRepoUser', mu)
        setPropIfEmpty(prj, 'mavenRepoKey', mk)

        if(prj['isSnapshot'] && prj.findProperty('mavenSnapshotUrl')){
            prop(prj, 'mavenPublishUrl', prj['mavenSnapshotUrl'])
        }

        String devs = prj.findProperty('developers') ?: [nodev: "Lone Ranger"]
        devs = devs instanceof Map ? devs : new groovy.json.JsonSlurper().parseText(devs)
        setPropIfEmpty prj, 'pomDevelopers', devs

        //** Helpful dir params
        setPropIfEmpty prj, 'gradleDir', "${prj.rootDir}/gradle"

        //println "isSnapshot " + prj.isSnapshot
        //println "prj.version " + prj.version
    }

    static ext(Project prj){
        prj.extensions.extraProperties
        //prj.ext
    }

    static expand(Project prj, String string){
        Template template;
        try {
            SimpleTemplateEngine engine = new SimpleTemplateEngine()
            template = engine.createTemplate(string)
        } finally {
            //original.close();
        }
        StringWriter writer = new StringWriter()
        template.make([project: prj]).writeTo(writer)
        return new StringReader(writer.toString())
    }

    /**
     * Deeply merges the contents of each Map in sources, merging from
     * "right to left" and returning the merged Map.
     *
     * Mimics 'extend()' functions often seen in JavaScript libraries.
     * Any specific Map implementations (e.g. TreeMap, LinkedHashMap)
     * are not guaranteed to be retained. The ordering of the keys in
     * the result Map is not guaranteed. Only nested maps will be
     * merged; primitives, objects, and other collection types will be
     * overwritten.
     *
     * The source maps will not be modified.
     */

    @CompileDynamic
    Map merge(Map[] sources) {
        if (sources.length == 0) return [:]
        if (sources.length == 1) return sources[0]

        sources.inject([:]) { result, source ->
            source.each { k, v ->
                result[k] = result[k] instanceof Map ? merge(result[k], v) : v
            }
            result
        } as Map
    }
}
