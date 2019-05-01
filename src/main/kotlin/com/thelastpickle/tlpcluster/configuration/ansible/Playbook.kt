package com.thelastpickle.tlpcluster.configuration.ansible

import org.apache.commons.io.FileUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File

class Playbook {

    fun copyResourcesToLocation(base: File) {
        val ansible = Reflections("com.thelastpickle.tlpcluster.ansible", ResourcesScanner())
        val files = ansible.getResources(".*".toPattern())

        for (f in files) {
            val input = this.javaClass.getResourceAsStream("/" + f)
            val outputFile = f.replace("com/thelastpickle/tlpcluster/", "")

            val output = File(base, outputFile)
            println("Writing ${output.absolutePath}")

            output.absoluteFile.parentFile.mkdirs()
            FileUtils.copyInputStreamToFile(input, output)
        }
    }
}