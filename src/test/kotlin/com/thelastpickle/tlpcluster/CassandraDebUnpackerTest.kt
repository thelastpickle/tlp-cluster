package com.thelastpickle.tlpcluster

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class CassandraDebUnpackerTest {

    lateinit var downloadDir : Path
    lateinit var unpacker : CassandraDebUnpacker
    lateinit var context: Context

    @BeforeEach
    fun setupUnpacker() {
        context = Context.testContext()
        downloadDir = Files.createTempDirectory("test")
        unpacker = CassandraDebUnpacker("2.1.14", downloadDir)
    }

    @AfterEach
    fun tearDownUnpacker() {
        FileUtils.deleteDirectory(downloadDir.toFile())
    }

    @Test
    fun ensureDownloadCreatesDebPackageAndConfFiles() {
        unpacker.download()

        assertThat(File(downloadDir.toFile(), "cassandra_2.1.14_all.deb")).exists()
        assertThat(File(downloadDir.toFile(), "conf")).exists()

        unpacker.extractConf(context)

    }

    @Test
    fun getURL() {
        val expected = "http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/cassandra_2.1.14_all.deb"
        assertThat(unpacker.getURL()).isEqualTo(expected)
    }

    @Test
    fun getFileName() {
    }

    @Test
    fun getVersion() {
    }

    @Test
    fun getDest() {
    }

    @Test
    fun ensureDebExistsBeforeExtracting() {
        assertThatIllegalStateException().isThrownBy { unpacker.extractConf(context) }
                .withMessageContaining("Check failed")

    }
}