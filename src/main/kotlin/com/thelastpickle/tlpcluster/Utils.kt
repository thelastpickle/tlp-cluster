package com.thelastpickle.tlpcluster

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream


class Utils {
    companion object {
        fun inputstreamToTempFile(inputStream: InputStream, prefix: String, directory: String) : File {
            val tempFile = File.createTempFile(prefix, "", File(directory))
            tempFile.deleteOnExit()

            val outputStream = FileOutputStream(tempFile)

            IOUtils.copy(inputStream, outputStream)
            outputStream.flush()
            outputStream.close()

            return tempFile
        }

        fun resourceToTempFile(resourcePath: String, directory: String) : File {
            val resourceName = File(resourcePath).name
            val resourceStream = this::class.java.getResourceAsStream(resourcePath)
            return Utils.inputstreamToTempFile(resourceStream, "${resourceName}_", directory)
        }

        fun prompt(question: String, default: String) : String {
            print("$question [$default]: ")
            var line = (readLine() ?: default).trim()

            if(line.equals(""))
                line = default

            return line
        }

    }
}