package com.px4.hawkeye.feature.replay.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.px4.hawkeye.core.domain.DataError
import com.px4.hawkeye.core.domain.Result
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File

class LibraryFileStoreTest {

    private fun store(dir: File, now: Long = 1_000L) = LibraryFileStore(dir, clock = { now })

    @Test
    fun `write copies bytes into the library and returns the size`(@TempDir dir: File) {
        val bytes = "hello ulog".toByteArray()

        val result = store(dir).write(ByteArrayInputStream(bytes), "abc.ulg")

        assertThat(result).isEqualTo(Result.Success(bytes.size.toLong()))
        val written = File(File(dir, "library"), "abc.ulg")
        assertThat(written.exists()).isTrue()
        assertThat(written.readBytes().toList()).isEqualTo(bytes.toList())
    }

    @Test
    fun `stage copies the library file into the inbox and writes the millis token`(@TempDir dir: File) {
        val store = store(dir, now = 4_242L)
        val bytes = "payload".toByteArray()
        store.write(ByteArrayInputStream(bytes), "abc.ulg")

        val result = store.stage("abc.ulg")

        assertThat(result).isEqualTo(Result.Success(Unit))
        val current = File(File(dir, "inbox"), "current.ulg")
        val ready = File(File(dir, "inbox"), ".ready")
        assertThat(current.readBytes().toList()).isEqualTo(bytes.toList())
        assertThat(ready.readText()).isEqualTo("4242")
    }

    @Test
    fun `re-staging overwrites the inbox file`(@TempDir dir: File) {
        val store = store(dir)
        store.write(ByteArrayInputStream("first".toByteArray()), "a.ulg")
        store.write(ByteArrayInputStream("second".toByteArray()), "b.ulg")

        store.stage("a.ulg")
        store.stage("b.ulg")

        val current = File(File(dir, "inbox"), "current.ulg")
        assertThat(current.readText()).isEqualTo("second")
    }

    @Test
    fun `staging a missing file fails with NOT_FOUND`(@TempDir dir: File) {
        val result = store(dir).stage("ghost.ulg")

        assertThat(result).isEqualTo(Result.Error(DataError.Local.NOT_FOUND))
    }

    @Test
    fun `delete removes the library file`(@TempDir dir: File) {
        val store = store(dir)
        store.write(ByteArrayInputStream("x".toByteArray()), "a.ulg")
        val file = File(File(dir, "library"), "a.ulg")
        assertThat(file.exists()).isTrue()

        store.delete("a.ulg")

        assertThat(file.exists()).isFalse()
    }
}
