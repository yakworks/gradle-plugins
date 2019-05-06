/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package testutil

import spock.lang.Specification

class OfflineCheckerTest extends Specification {

    def "knows when is offline"() {
        expect:
        OfflineChecker.isOffline("http://mockitoooooooooo.org")
    }
}
