package com.example.ipilotvoz

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt

class IPilotAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: IPilotAccessibilityService? = null }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Executa o comando e devolve uma frase curta de retorno (para voz/notificação). */
    fun execute(cmd: Cmd): String {
        val pkg = rootInActiveWindow?.packageName?.toString() ?: ""
        if (Cfg.pkgHints(this).none { pkg.lowercase().contains(it) })
            return "Abra o i-Pilot na tela. App atual: $pkg"
        return when (cmd) {
            is Cmd.Speed -> { repeatTap(if (cmd.up) Btn.PLUS else Btn.MINUS, cmd.steps)
                (if (cmd.up) "Aumentando" else "Diminuindo") + " velocidade" }
            is Cmd.Turn -> {
                val n = (cmd.degrees / Cfg.degPerTap(this).toFloat()).roundToInt().coerceIn(1, 36)
                repeatTap(if (cmd.right) Btn.RIGHT else Btn.LEFT, n)
                "${if (cmd.right) "Direita" else "Esquerda"} ${cmd.degrees} graus" }
            Cmd.Anchor -> { repeatTap(Btn.ANCHOR, 1); "Âncora" }
            Cmd.Prop -> { repeatTap(Btn.PROP, 1); "Hélice" }
            Cmd.HeadingLock -> { repeatTap(Btn.NORTH, 1); "Rumo travado" }
        }
    }

    fun repeatTap(b: Btn, times: Int, i: Int = 0) {
        if (i >= times) return
        val dm = resources.displayMetrics
        val (fx, fy) = Cfg.pos(this, b)
        val path = Path().apply { moveTo(fx * dm.widthPixels, fy * dm.heightPixels) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60)).build()
        val next = { handler.postDelayed({ repeatTap(b, times, i + 1) }, 300) }
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { next() }
            override fun onCancelled(d: GestureDescription?) { next() }
        }, null)
    }
}
