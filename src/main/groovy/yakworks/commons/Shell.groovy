package yakworks.commons

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Simple way to run shell scripts and some shell scripts that are helpful to have.
 *
 */
@Slf4j
@CompileStatic
class Shell {

    /**
     * calls ['sh', '-c', command].execute().text.trim()
     */
    static String exec(String command){
        return ['sh', '-c', command].execute().text.trim()
    }

}
