/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.util

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer

/**
 * Useful to work around Gradle API that requires the use of Groovy.
 */
@CompileStatic
class GradleDSLHelper {

    /**
     * Needed because we cannot access publications or publishing normally via Java
     * Doing so will "resolve" the publications too early and things would not work
     * For example, pom would not have dependencies
     */
    @CompileDynamic
    static void publications(Project project, Action<PublicationContainer> action) {
        project.publishing {
            action.execute(publications)
        }
    }
}
