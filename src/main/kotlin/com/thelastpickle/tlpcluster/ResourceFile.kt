package com.thelastpickle.tlpcluster

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.net.URL

/**
 * Creates a temporary file from a resource location
 */
class ResourceFile(val resource: URL) {

    val fp: File

    init {
        checkNotNull(resource)

        fp = File.createTempFile(
                FilenameUtils.getBaseName(resource.file),
                FilenameUtils.getExtension(resource.file))

        IOUtils.copy(resource.openStream(), FileUtils.openOutputStream(fp))

    }

    val path : String
        get() = fp.absolutePath

}