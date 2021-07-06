/*
* Copyright 2020 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic

import yakworks.gradle.util.PropertiesUtil

/**
 * handles the snapshot property in the version.props file
 */
@CompileStatic
class YakVersionInfo {

    String version
    String previousVersion
    boolean isSnapshot

    YakVersionInfo(String version, String previousVersion, boolean isSnapshot) {
        this.version = version;
        this.previousVersion = previousVersion;
        this.isSnapshot = isSnapshot
    }

    static YakVersionInfo fromFile(File versionFile) {
        Properties properties = PropertiesUtil.readProperties(versionFile);
        String ver = properties.getProperty("version");
        if (ver == null) {
            throw new IllegalArgumentException("Missing 'version=' properties in file: " + versionFile);
        }
        def isSnap = properties.getProperty("snapshot")?.toBoolean()
        ver = maybeSnapshot(isSnap, ver);
        String publishedVersion = properties.getProperty("publishedVersion")
        return new YakVersionInfo(ver, publishedVersion, isSnap);
    }

    static String maybeSnapshot(Boolean isSnap, String version) {
        if (isSnap && !version.endsWith("-SNAPSHOT")) {
            version += "-SNAPSHOT";
        }
        return version;
    }


    YakVersionInfo bumpVersion() {
        return this;
    }

}
