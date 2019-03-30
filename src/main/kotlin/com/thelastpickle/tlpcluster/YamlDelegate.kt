package com.thelastpickle.tlpcluster

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.reflect.KProperty

/**
 * Making yaml mapping easy
 * Usage example:
 *
 * val yaml : ObjectMapper by YamlDelegate
 */
class YamlDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) : ObjectMapper = yaml

    companion object {
        val yaml = ObjectMapper(YAMLFactory()).registerKotlinModule()
    }
}

