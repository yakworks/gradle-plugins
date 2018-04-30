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

//import static yakworks.gradle.GradleHelpers.prop

@CompileStatic
class YakworksConfigPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(YakworksConfigPlugin)

    void apply(Project prj) {
        if (prj.rootProject != prj) {
            throw new GradleException('yakworks.defaults must only be applied to the root project')
        }
        String environment = prj.property('env') ?: 'dev'
        //def configFile = prj.file('config.groovy')

        List configFileNames =  []
        if(prj.hasProperty('configFiles')){
            configFileNames = prj.property('configFiles').toString().split(',') as List
        } else {
            //do defaults for config.groovy and config.yml
            configFileNames.add("${prj.rootDir}/gradle/config.yml")
        }
        configFileNames = configFileNames as List<String>
        ConfigObject config = new ConfigObject()
        configFileNames.each { String fname ->
            File configFile = prj.file(fname)
            if(configFile.exists()){
                if(fname.endsWith('.groovy')){
                    ConfigObject cfgObj = new ConfigSlurper(environment).parse(configFile.toURI().toURL())
                    config.merge(cfgObj)
                } else if(fname.endsWith('.yml')){
                    Yaml yaml = new Yaml()
                    Map ymlMap = yaml.load(new FileInputStream(configFile))
                    ConfigObject cfgObj = new ConfigObject()
                    cfgObj.putAll(ymlMap)
                    config.merge(cfgObj)
                }
            }
        }
        GradleHelpers.prop(prj, 'config', config)
    }

    /**
     * first look for any defaultConfig.yml on the classpath
     */
    ConfigObject loadDefaults(){
        ConfigObject config = new ConfigObject()
        List<URL> resources = CPScanner.scanResources(new PackageNameFilter("configs.*"), new ResourceNameFilter("*defaults.yml"))
        resources.each { URL ymlSource ->
            config.merge(loadYaml(ymlSource))
        }
        config
    }

    /**
     * first look for any defaultConfig.yml on the classpath
     */
    ConfigObject loadYaml(ymlSource){
        Yaml yaml = new Yaml()
        Map ymlMap = yaml.load((Reader)ymlSource)
        ConfigObject cfgObj = new ConfigObject()
        cfgObj.putAll(ymlMap)
        cfgObj
    }

}
