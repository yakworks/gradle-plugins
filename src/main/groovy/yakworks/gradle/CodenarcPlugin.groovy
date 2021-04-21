/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle

import groovy.transform.CompileDynamic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * sets up codenarc with predefined defaults the way we want them.
 */
@CompileDynamic
class CodenarcPlugin implements Plugin<Project> {

    //final static String CODENARC_VERSION = "1.3"
    //final static String CODENARC_EXTRA_VERSION = "1.0.2"

    void apply(Project prj) {
        prj.plugins.apply('codenarc')
        if(!prj.hasProperty('yakworks')) prj.ext.yakworks = [:]
        prj.yakworks.getCodenarcRuleSet = { ->
            String ruleSets = this.getClass().getResource('/codenarcRulesets.groovy').text
            ruleSets = ruleSets.replace("/*@extCodenarcRulesets@*/", prj.findProperty('codenarcRuleset')?:'')
            return prj.resources.text.fromString(ruleSets)
        }

        prj.afterEvaluate {
            Map cfg = prj.config.codenarc
            prj.codenarc {
                config = prj.yakworks.getCodenarcRuleSet()
                toolVersion = cfg.toolVersion
                reportFormat = cfg.reportFormat
                //ignoreFailures = true
                maxPriority1Violations = cfg.maxPriority1Violations
                maxPriority2Violations = cfg.maxPriority2Violations
                maxPriority3Violations = cfg.maxPriority3Violations
            }
            codenarcSettings(prj, "codenarcMain", cfg.main)
            codenarcSettings(prj, "codenarcTest", cfg.test)
            codenarcSettings(prj, "codenarcIntegrationTest", cfg.integrationTest)
        }

        prj.dependencies {
            delegate.codenarc("org.codenarc:CodeNarc:${prj.config.codenarc.toolVersion}")
            // delegate.codenarc("io.9ci.yakworks:codenarc-extra:$CODENARC_EXTRA_VERSION")
        }
    }

    void codenarcSettings(Project prj, String extName, Map cfg){
        if(!prj.findProperty(extName)) return
        prj."$extName" {
            if(cfg.enabled == false) {
                exclude '**/*'
            }
            ignoreFailures = cfg.ignoreFailures
            cfg.excludes?.each{
                exclude it
            }
        }
    }

}
