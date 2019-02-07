package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.io.InputStream

class Yaml(val parser: JsonNode) {

    companion object {
        val yamlFactory = YAMLFactory()
        val mapper = ObjectMapper(YAMLFactory())

        fun create(fp: File) : Yaml {
            val tmp = mapper.readTree(fp)
            return Yaml(tmp)
        }

        fun create(inputStream: InputStream) : Yaml {
            val tmp = mapper.readTree(inputStream)
            return Yaml(tmp)
        }
    }
    fun setProperty(name: String, value: String) {
        val tmp = parser as ObjectNode
        tmp.put(name, value)
    }


    fun setSeeds(seeds: List<String>) {
        val seedNode = parser.get("seed_provider").first().get("parameters").first()
        val tmp = seedNode as ObjectNode
        val seedList = seeds.joinToString(",")
        tmp.put("seeds", seedList)
    }

    /**
     * Convenience method
     */
    fun setSeeds(seeds: Seeds) {
        setSeeds(seeds.seeds)
    }

    fun write(path: String) {
        mapper.writeValue(File(path), parser)
    }

}