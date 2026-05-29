package com.px4.hawkeye.android.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class RenderSessionTest {

    @Test
    fun `replay session round-trips through a bundle-like map`() {
        val extras = RenderSession.Replay(filePath = "/data/library/abc.ulg").toExtras()
        val decoded = RenderSession.fromExtras(extras)
        assertThat(decoded).isNotNull().isInstanceOf(RenderSession.Replay::class)
        assertThat((decoded as RenderSession.Replay).filePath).isEqualTo("/data/library/abc.ulg")
    }

    @Test
    fun `live session round-trips`() {
        val extras = RenderSession.Live(host = "192.168.1.42", port = 19410).toExtras()
        val decoded = RenderSession.fromExtras(extras)
        assertThat(decoded).isNotNull().isInstanceOf(RenderSession.Live::class)
        val live = decoded as RenderSession.Live
        assertThat(live.host).isEqualTo("192.168.1.42")
        assertThat(live.port).isEqualTo(19410)
    }

    @Test
    fun `absent mode decodes to null (legacy inbox launch)`() {
        assertThat(RenderSession.fromExtras(emptyMap())).isNull()
    }

    @Test
    fun `live mode with missing host decodes to null`() {
        val extras = mapOf(RenderSession.KEY_MODE to RenderSession.MODE_LIVE, RenderSession.KEY_PORT to "19410")
        assertThat(RenderSession.fromExtras(extras)).isNull()
    }

    @Test
    fun `replay mode with missing path decodes to null`() {
        val extras = mapOf<String, String?>(RenderSession.KEY_MODE to RenderSession.MODE_REPLAY)
        assertThat(RenderSession.fromExtras(extras)).isNull()
    }
}
