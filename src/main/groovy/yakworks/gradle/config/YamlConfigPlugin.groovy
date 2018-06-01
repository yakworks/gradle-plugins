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

@CompileStatic
class YamlConfigPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(YamlConfigPlugin)

    ConfigMap config

    void apply(Project prj) {
        if (prj.rootProject != prj) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        String environment = prj.findProperty('env') ?: 'dev'
        //def configFile = prj.file('config.groovy')

        List configFileNames =  []
        if(prj.hasProperty('configFiles')){
            configFileNames = prj.property('configFiles').toString().split(',') as List
        } else {
            //do defaults for config.groovy and config.yml
            configFileNames.add("${prj.rootDir}/gradle.yml")
            configFileNames.add("${prj.rootDir}/gradle/config.yml")
        }
        configFileNames = configFileNames as List<String>

        config = new ConfigMap()
        config.extraBinding['project']      = prj
        config.extraBinding['property']     = { String prop -> prj.property(prop) }
        config.extraBinding['findProperty'] = { String prop -> prj.findProperty(prop) }
        config.extraBinding['findAnyProperty'] = { String prop -> GradleHelpers.findAnyProperty(prj, prop) }

        loadClassPathDefaults(prj, config)

        configFileNames.each { String fname ->
            File configFile = prj.file(fname)
            if(configFile.exists()){
                if(fname.endsWith('.groovy')){
                    ConfigObject cfgObj = new ConfigSlurper(environment).parse(configFile.toURI().toURL())
                    config.merge(cfgObj)
                } else if(fname.endsWith('.yml')){
                    Map ymlMap = new Yaml().load(new FileInputStream(configFile))
                    //def co = new ConfigObject()
                    //co.putAll(ymlMap)
                    config.merge(ymlMap)
                }
            }
        }

        GradleHelpers.prop(prj, 'config', config)
        //setup core project items if they exit
        GradleHelpers.setPropIfEmpty(prj, 'description', config['description'])
        GradleHelpers.setPropIfEmpty(prj, 'group', config['group'])
        GradleHelpers.setPropIfEmpty(prj, 'name', config['name'])
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
     * first look for any defaultConfig.yml on the classpath
     */
    Map loadYaml(ymlSource){
        Yaml yaml = new Yaml()
        Map loaded = yaml.load((Reader)ymlSource)
        //loaded = yaml.load((Reader)ymlSource)
        //println loaded
        return loaded
    }

}
