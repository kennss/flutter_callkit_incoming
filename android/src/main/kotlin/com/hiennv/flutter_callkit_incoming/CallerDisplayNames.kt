/**
 * @file        CallerDisplayNames.kt
 * @description SnowChat fork — the caller name the app gave a call that is already on screen
 *              (updateCallerName), in memory only. Every place the plugin draws a caller name
 *              (incoming / ongoing notification, full-screen incoming screen, Telecom Connection)
 *              resolves it through here, so the name never has to go into the call Data that the
 *              plugin writes to SharedPreferences (ACTIVE_CALLS) or into a Telecom extras bundle:
 *              the app always raises the call as "SnowChat Call" and names it afterwards.
 * @author      Kennt Kim
 * @company     Calida Lab
 * @created     2026-11-23
 * @lastUpdated 2026-11-23
 *
 * @functions
 *  - CallerDisplayNames.set(callId, name): remember the name and tell open screens
 *  - CallerDisplayNames.resolve(data): the name to draw for a call bundle (override, else the bundle's)
 *  - CallerDisplayNames.remove(callId) / clear(): forget a call's name / every name
 *  - CallerDisplayNames.addListener / removeListener: screens that redraw on a change
 *
 * Notes: keys are the call ids the app used (Data.id); values are names — never logged.
 *        Entries live until the call ends (receiver end / decline / timeout, plugin endCall /
 *        endAllCalls) or the process dies.
 */
package com.hiennv.flutter_callkit_incoming

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

object CallerDisplayNames {

    /** Call id → name the app set. Process memory only. */
    private val names = ConcurrentHashMap<String, String>()

    /** Called on the main thread with the call id whose name changed. */
    private val listeners = CopyOnWriteArraySet<(String) -> Unit>()

    fun set(callId: String, name: String) {
        if (callId.isEmpty() || name.isEmpty()) return
        names[callId] = name
        val notify = Runnable { listeners.forEach { it(callId) } }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notify.run()
        } else {
            Handler(Looper.getMainLooper()).post(notify)
        }
    }

    fun get(callId: String): String? = names[callId]

    /** The name to draw for [data]: the app's name for the call, else the bundle's own. */
    fun resolve(data: Bundle?): String {
        val id = data?.getString(CallkitConstants.EXTRA_CALLKIT_ID, "") ?: ""
        if (id.isNotEmpty()) {
            names[id]?.let { return it }
        }
        return data?.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "") ?: ""
    }

    fun remove(callId: String) {
        names.remove(callId)
    }

    fun clear() {
        names.clear()
    }

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }
}
