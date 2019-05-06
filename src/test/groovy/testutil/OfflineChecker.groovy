/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package testutil

class OfflineChecker {

    static isOffline() {
        isOffline("http://google.com")
    }

    static isOffline(String testUrl) {
        try {
            new URL(testUrl).withInputStream { }
            return false
        } catch (Exception ignored) {
            return true
        }
    }
}
