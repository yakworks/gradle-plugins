package yakworks.groovy

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class Maps {

    /**
     * https://gist.github.com/robhruska/4612278
     * Deeply merges the contents of each Map in sources, merging from
     * "right to left" and returning the merged Map.
     *
     * Mimics 'extend()' functions often seen in JavaScript libraries.
     * Any specific Map implementations (e.g. TreeMap, LinkedHashMap)
     * are not guaranteed to be retained. The ordering of the keys in
     * the result Map is not guaranteed. Only nested maps will be
     * merged; primitives, objects, and other collection types will be
     * overwritten.
     *
     * The source maps will not be modified.
     */
    static Map merge(Map[] sources) {
        if (sources.length == 0) return [:]
        if (sources.length == 1) return sources[0]

        sources.inject([:]) { result, source ->
            source.each { k, v ->
                result[k] = result[k] instanceof Map ? merge(result[k] as Map, v as Map) : v
            }
            result
        } as Map
    }

    /**
     * removes all nulls and optionally empty maps lists and stings as well
     *
     * @param map the map to prune
     * @param pruneEmpty removes empty maps, lists and strings as well
     * @return the pruned map
     */
    static Map deepPrune(Map map, boolean pruneEmpty = false) {
        map.collectEntries { k, v -> [k, v instanceof Map ? deepPrune(v as Map) : v]}
        .findAll { k, v ->
            if(pruneEmpty && (v instanceof List || v instanceof Map) && v){
                //if()
            } else {
                return v != null
            }

        }
    }
}
