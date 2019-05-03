package yakworks.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CodeNarcPlugin
/**
 * sets up codenarc with predefined defaults the way we want them.
 */
class CodenarcPlugin implements Plugin<Project> {

    //final static String CODENARC_VERSION = "1.3"
    final static String CODENARC_EXTRA_VERSION = "1.0.2"

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
            Map cfgMain = prj.config.codenarc.main
            prj.codenarcMain {
                ignoreFailures = cfgMain.ignoreFailures
                cfgMain.excludes?.each{
                    exclude it
                }
            }
            Map cfgTest = prj.config.codenarc.test
            prj.codenarcTest {
                if(cfgTest.enabled == false) {
                    exclude '**/*'
                }
                ignoreFailures = cfgTest.ignoreFailures
                cfgTest.excludes?.each{
                    exclude it
                }
            }
        }

        prj.dependencies {
            delegate.codenarc("org.codenarc:CodeNarc:${prj.config.codenarc.toolVersion}")
            delegate.codenarc("io.9ci.yakworks:codenarc-extra:$CODENARC_EXTRA_VERSION")
        }
    }

}
