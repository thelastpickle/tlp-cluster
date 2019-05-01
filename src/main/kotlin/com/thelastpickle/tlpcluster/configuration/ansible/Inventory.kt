package com.thelastpickle.tlpcluster.configuration.ansible

import java.io.OutputStream

/**
 * Writes
 */
class Inventory(val cassandra: List<String>, val stress: List<String>, val monitoring: String) {

    fun write(out: OutputStream) {
        val buf = out.bufferedWriter()

        buf.write("[cassandra]")
        buf.newLine()

        for (c in cassandra) {
            buf.write(c)
            buf.newLine()
        }

        buf.write("[stress]")
        buf.newLine()

        for(s in stress) {
            buf.write(s)
            buf.newLine()
        }

        buf.write("[monitoring]")
        buf.newLine()
        buf.write(monitoring)
        buf.newLine()

        buf.flush()
    }

}