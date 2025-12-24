package com.sayzen.campfiresdk.controllers

import com.sup.dev.android.app.SupAndroid
import com.sup.dev.android.models.EventConfigurationChanged
import com.sup.dev.android.models.EventStyleChanged
import com.sup.dev.android.tools.ToolsAndroid
import com.sup.dev.java.libs.eventBus.EventBus

object ControllerLocale {
    private val eventBus = EventBus
            .subscribe(EventConfigurationChanged::class) { onEventConfigurationChanged(it) }
            .subscribe(EventStyleChanged::class) { onEventStyleChanged(it) }

    fun init() {
        val lang = ToolsAndroid.getLanguage(SupAndroid.appContext!!)
        if (ControllerSettings.appLanguage != lang) {
            // onEventStyleChanged is called after setting appLanguage - avoid recursion
            ControllerSettings.appLanguage = lang
            SupAndroid.activity?.recreate()
        }
    }

    fun setLanguage(lang: String) {
        ToolsAndroid.setLanguage(SupAndroid.appContext!!, lang)
        ControllerSettings.appLanguage = lang
    }

    //
    // EventBus
    //

    private fun onEventConfigurationChanged(e: EventConfigurationChanged) {
        init()
    }

    private fun onEventStyleChanged(e: EventStyleChanged) {
        val lang = ControllerSettings.appLanguage
        if (ToolsAndroid.getLanguage(SupAndroid.appContext!!) != lang) {
            ToolsAndroid.setLanguage(SupAndroid.appContext!!, lang)
            SupAndroid.activity!!.recreate()
        }
    }
}
