/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.util

import groovy.transform.CompileStatic

/**
 * String utilities.
 */
@CompileStatic
class StringUtil {

    //TODO (maybe) convert to Java at some point

    /**
     * Classic string join
     */
    static String join(Collection<String> collection, String separator) {
        return collection.join(separator)
    }

    /**
     * Capitalizes string
     */
    static String capitalize(String input) {
        return input.capitalize()
    }

    /**
     * Checks if input is empty, if input is not null its 'toString()' value will be used.
     */
    static boolean isEmpty(Object input) {
        return input == null || isEmpty(input.toString())
    }

    /**
     * Checks if input is empty
     */
    static boolean isEmpty(String input) {
        return input == null || input.isEmpty()
    }
}
