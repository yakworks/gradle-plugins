/* Copyright 2019. 9ci Inc. Licensed under the Apache License, Version 2.0 */
package grails3.app

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration

class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}
