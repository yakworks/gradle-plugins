package yakworks.gradle

import spock.lang.Specification

class DocmarkPluginSpec extends Specification {

    def "ReplaceVersionRegex"() {
        when:
        String vpat = /foo:bar:(\d+)\.(\d+)\.(\d+)(-(\w+)\.(\d+))?+/
        String v = "compile 'foo:bar:1.1.1' //baz"
        then:
        v.replaceAll(vpat, 'foo:bar:1.1.2-RC.1') == "compile 'foo:bar:1.1.2-RC.1' //baz"

        when:
        v = "compile 'foo:bar:1.1.1-RC.1' //baz"

        then:
        v.replaceAll(vpat, 'foo:bar:1.1.2-RC.1') == "compile 'foo:bar:1.1.2-RC.1' //baz"
    }
}
