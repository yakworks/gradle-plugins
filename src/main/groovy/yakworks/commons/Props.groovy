package yakworks.commons

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.MetaClassHelper

import java.beans.Introspector

@Slf4j
@CompileStatic
class Props {

    /**
     * converts SOME_PROP to someProp
     */
    static String toCamelCase( String text ) {
        text = text.toLowerCase().replaceAll( "(_)([A-Za-z0-9])", { List<String> it -> it[2].toUpperCase() } )
        //println text
        return text
    }



}
