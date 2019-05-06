/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.config

import groovy.transform.CompileStatic
import net.sf.corn.cps.CPScanner
import net.sf.corn.cps.PackageNameFilter
import net.sf.corn.cps.ResourceNameFilter
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.yaml.snakeyaml.Yaml
import yakworks.gradle.GradleHelpers
import yakworks.commons.ConfigMap

//import static yakworks.gradle.GradleHelpers.prop

/**
 * Loads the default.yml from files from gradle plugins and merges in yml configFiles either from the configFiles
 * or from the gradle.yml or gradle/config.yml
 */
@CompileStatic
class YamlConfigPlugin implements Plugin<Project> {
    //private final static Logger LOG = Logging.getLogger(YamlConfigPlugin)

    ConfigMap config

    void apply(Project prj) {
        if (prj.rootProject != prj) {
            throw new GradleException('YamlConfigPlugin must only be applied to the root project')
        }
        String environment = prj.findProperty('env') ?: 'dev'
        //def configFile = prj.file('config.groovy')

        List configFileNames =  []
        if(prj.hasProperty('configFiles')){
            configFileNames = prj.property('configFiles').toString().split(',') as List
        } else {
            //do defaults for config.groovy and config.yml
            configFileNames.add("${prj.rootDir}/gradle.yml")
            configFileNames.add("${prj.rootDir}/build.yml")
            configFileNames.add("${prj.rootDir}/gradle/config.yml")
            configFileNames.add("${prj.rootDir}/gradle/build.yml")
            configFileNames.add("${prj.rootDir}/gradle/shipkit.yml")
        }
        configFileNames = configFileNames as List<String>

        config = new ConfigMap()
        config.addToBinding([
            project: prj,
            findAnyProperty: { String prop -> GradleHelpers.findAnyProperty(prj, prop) },
            props: [
                findAny: { String prop -> GradleHelpers.findAnyProperty(prj, prop) }
            ]
        ])
//        config.extraBinding['project']         = prj
//        config.extraBinding['property']        = { String prop -> prj.property(prop) }
//        config.extraBinding['findProperty']    = { String prop -> prj.findProperty(prop) }
//        config.extraBinding['findAnyProperty'] = { String prop -> GradleHelpers.findAnyProperty(prj, prop) }

        loadClassPathDefaults(prj, config) //looks for /configs/defaults.yml

        configFileNames.each { String fname ->
            File configFile = prj.file(fname)
            if(configFile.exists()){
                if(fname.endsWith('.groovy')){
                    ConfigObject cfgObj = new ConfigSlurper(environment).parse(configFile.toURI().toURL())
                    config.merge(cfgObj)
                } else if(fname.endsWith('.yml')){
                    Map ymlMap = new Yaml().load(new FileInputStream(configFile))
                    config.merge(ymlMap)
                }
            }
        }

        //assign the config object
        GradleHelpers.prop(prj, 'config', config)
        //setup core project items if they exit
        GradleHelpers.setPropIfEmpty(prj, 'description', config['description'])
        //assign group if in config
        if(config['group']){
            prj.allprojects { Project prjsub ->
                //println "prj.group for ${prjsub.name}: ${prjsub.group}"
                prjsub.setGroup(config['group'])
            }
        }
    }

    /**
     * first look for any /configs/defaults.yml on the classpath
     */
    void loadClassPathDefaults(Project prj, ConfigMap config){
        List<URL> resources = CPScanner.scanResources(new PackageNameFilter("configs"), new ResourceNameFilter("defaults.yml"))
        //println "resources: $resources"
        resources.each { URL url ->
            //def co = new ConfigMap()
            Reader ymlExpanded = url.newReader() //GradleHelpers.expand(prj, url.text)
            //co.putAll(loadYaml(ymlExpanded))
            config.merge(loadYaml(ymlExpanded))
        }
        config
    }

    /**
     * load the ymlSource file, ymlSource should be some type of reader
     */
    Map loadYaml(Reader ymlSource){
        Yaml yaml = new Yaml()
        Map loaded = yaml.load(ymlSource)
        //loaded = yaml.load((Reader)ymlSource)
        //println loaded
        return loaded
    }

}
