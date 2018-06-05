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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class ProjectUtils {

    @CompileDynamic
    static void setPropIfEmpty(Project prj, String propertyName, defaultValue) {
        if (!prj.hasProperty(propertyName) && defaultValue != null) {
            prj.ext[propertyName] = defaultValue
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
}