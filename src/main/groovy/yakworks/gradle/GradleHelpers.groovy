/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Project

import yakworks.commons.lang.NameUtils

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
    static void prop(Project prj, String propertyName, Object val) {
        //if it has the property then set it.
        if(prj.hasProperty(propertyName)){
            prj.setProperty(propertyName, val)
        } else { //add it
            ext(prj)[propertyName] = val
        }
    }

    @CompileDynamic
    static void setPropIfEmpty(Project prj, String propertyName, Object defaultValue) {
        if (!prj.findProperty(propertyName) && defaultValue != null) {
            prop(prj, propertyName, defaultValue)
        }
    }

    /**
     * Generally you'll pass in an environment var like SOME_PROP.
     * look in System.env then System.properties, converts to camelCase and then calls findProperty
     *
     * @return the prop or null
     */
    static Object findAnyProperty(Project prj, String prop) {
        if (System.getenv(prop) != null) {
            return System.getenv(prop)
        } else if (System.properties[prop]) {
            return System.properties[prop]
        } else {
            return prj.findProperty(NameUtils.toCamelCase(prop)) //returns null if not found
        }
    }

    static ext(Project prj){
        prj.extensions.extraProperties
        //prj.ext
    }

    static Reader expand(Project prj, String string){
        Template template;
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        template = engine.createTemplate(string)
        StringWriter writer = new StringWriter()
        template.make([project: prj]).writeTo(writer)
        return new StringReader(writer.toString())
    }

}
