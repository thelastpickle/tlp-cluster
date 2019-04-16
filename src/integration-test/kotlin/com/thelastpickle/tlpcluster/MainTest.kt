package com.thelastpickle.tlpcluster

import org.junit.jupiter.api.Test

class MainTest {

    @Test
    fun basicTest() {
        main(arrayOf("init", "tlp-cluster", "no ticket", "automated test suite", "-s", "1"))
        main(arrayOf("up",  "--yes"))
        main(arrayOf("use", "3.11.4"))
        main(arrayOf("install"))
        main(arrayOf("start"))
        main(arrayOf("down",  "--yes"))
        main(arrayOf("clean"))
    }


    fun init() {

    }
}