package com.thelastpickle.tlpcluster.terraform

import com.thelastpickle.tlpcluster.Context
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.InputStream


class TFJson(val context: Context, val file: InputStream) {

    private var tree = context.json.readTree(file)
    private var log = logger()

    companion object {
        fun parse(context: Context, path: File): TFJson {
            return TFJson(context, path.inputStream())
        }
    }

    fun getClusterName() : String {
        return tree.path("variable").path("ClusterName").path("default").textValue()
    }
}