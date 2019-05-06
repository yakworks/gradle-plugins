/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package testutil

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipMaker {

    private final File tmpDir
    private int counter = 1

    ZipMaker(File tmpDir) {
        this.tmpDir = tmpDir
        assert tmpDir.isDirectory()
        assert tmpDir.list().length == 0
    }

    /**
     * creates zip file with the contents, contents are pairs of: pathInZip, content, pathInZip, content, etc.
     */
    File newZip(String ... contents) {
        assert contents.length % 2 == 0
        assert contents.length != 0

        File zip = new File(tmpDir, "zip-${counter++}.zip")
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))

        def path = null
        for (String c : contents) {
            assert c != null
            if (path == null) {
                path = c
            } else {
                out.putNextEntry(new ZipEntry(path))
                out.write(c.bytes)
                out.closeEntry()
                path = null
            }
        }

        out.close()
        return zip
    }
}
