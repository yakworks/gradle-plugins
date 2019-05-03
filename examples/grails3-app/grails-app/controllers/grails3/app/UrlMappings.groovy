/* Copyright 2019. 9ci Inc. Licensed under the Apache License, Version 2.0 */
package grails3.app

import groovy.transform.CompileDynamic

@CompileDynamic
class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
