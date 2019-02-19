package com.thelastpickle.tlpcluster

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.net.URL

/**
 * Creates a temporary file from a resource location
 */
class ResourceFile(val resource: URL) {

    val fp: File
    val log = logger()

    init {
        checkNotNull(resource)

        val tmpDir = File(System.getProperty("user.home"), ".tlp-cluster/tmp")

        if(!tmpDir.exists()) {
            log.debug { "Creating temporary directory at $tmpDir" }
            tmpDir.mkdir()
        }



        fp = File.createTempFile(
                FilenameUtils.getBaseName(resource.file),
                FilenameUtils.getExtension(resource.file),
                tmpDir)

        IOUtils.copy(resource.openStream(), FileUtils.openOutputStream(fp))

    }

    val path : String
        get() = fp.absolutePath

}