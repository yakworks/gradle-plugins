package yakworks.groovy

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper

@CompileStatic
class Pogo {

    /**
     * Merge the a map, nested or not, into the pogo. Uses the InvokerHelper.setProperties(values)
     *
     * @param pogo
     * @param values
     * @param args various options for performing the merge
     *   - ignoreNulls : (boolean) defaults to true
     */
    void merge( Map args = [:], Object pogo, Map values){
        boolean ignoreNulls = args.containsKey('ignoreNulls') ? args['ignoreNulls'] : true
        if(ignoreNulls){
            values = Maps.deepPrune(values)
        }
        InvokerHelper.setProperties(pogo, values)
    }

    //standard deep copy implementation
    //take from here https://stackoverflow.com/questions/13155127/deep-copy-map-in-groovy
    //also see @groovy.transform.AutoClone
    def deepcopy(orig) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ObjectOutputStream oos = new ObjectOutputStream(bos)
        oos.writeObject(orig)
        oos.flush()
        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray())
        ObjectInputStream ois = new ObjectInputStream(bin)
        return ois.readObject()
    }

}
