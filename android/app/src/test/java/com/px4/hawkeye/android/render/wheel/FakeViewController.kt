package com.px4.hawkeye.android.render.wheel

import com.px4.hawkeye.android.render.ViewController
import com.px4.hawkeye.android.render.ViewSnapshot

class FakeViewController : ViewController {
    var snapshot: ViewSnapshot = ViewSnapshot.Default

    val camModes = mutableListOf<Int>()
    val orthoModes = mutableListOf<Int>()
    var panelToggles = 0

    override fun view(): ViewSnapshot = snapshot

    override fun setCamMode(camMode: Int) {
        camModes += camMode
    }

    override fun setOrthoMode(orthoMode: Int) {
        orthoModes += orthoMode
    }

    override fun toggleSidePanel() {
        panelToggles++
    }

    var topInsetPx: Int = 0
    override fun setTopInset(px: Int) {
        topInsetPx = px
    }
}
