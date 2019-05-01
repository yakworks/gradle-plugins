package yakworks.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * sets up codenarc with predefined defaults the way we want them.
 */
class CodenarcPlugin implements Plugin<Project> {

    final static String CODENARC_VERSION = "1.3"
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
            prj.codenarc {
                toolVersion = CODENARC_VERSION
                config = prj.yakworks.getCodenarcRuleSet()
                reportFormat = 'html'
                //ignoreFailures = true
                maxPriority1Violations = 0
                maxPriority2Violations = 4
                maxPriority3Violations = 4
            }
        }

        prj.dependencies {
            delegate.codenarc("org.codenarc:CodeNarc:$CODENARC_VERSION")
            delegate.codenarc("io.9ci.yakworks:codenarc-extra:$CODENARC_EXTRA_VERSION") //FIXME this is really all we need to depend on?
        }
    }

    private void addCodenarc(Project prj) {

    }
}
