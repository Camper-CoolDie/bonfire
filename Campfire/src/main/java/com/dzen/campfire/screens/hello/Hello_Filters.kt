package com.dzen.campfire.screens.hello

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.dzen.campfire.R
import com.dzen.campfire.api.API
import com.dzen.campfire.api.API_TRANSLATE
import com.sayzen.campfiresdk.controllers.ControllerSettings
import com.sayzen.campfiresdk.controllers.t
import com.sup.dev.android.tools.ToolsView
import com.sup.dev.android.views.settings.SettingsCheckBox

class Hello_Filters(
    val screen: SCampfireHello,
    private val demoMode: Boolean
) {
    val view: View = ToolsView.inflate(screen.vContainer, R.layout.screen_campfire_hello_filters)
    private val vGames: SettingsCheckBox = view.findViewById(R.id.vGames)
    private val vAnime: SettingsCheckBox = view.findViewById(R.id.vAnime)
    private val vArt: SettingsCheckBox = view.findViewById(R.id.vArt)
    private val vMovies: SettingsCheckBox = view.findViewById(R.id.vMovies)
    val vNext: Button = view.findViewById(R.id.vNext)
    private val vTextFilters: TextView = view.findViewById(R.id.vTextFilters)

    private val list = arrayListOf(vGames, vAnime, vMovies, vArt)

    init {
        for (i in list) i.setOnClickListener { updateFinishEnabled() }

        vTextFilters.text = t(API_TRANSLATE.into_feed_filters)
        vNext.text = t(API_TRANSLATE.app_continue)
        vGames.setTitle(t(API_TRANSLATE.category_games))
        vAnime.setTitle(t(API_TRANSLATE.category_anime))
        vMovies.setTitle(t(API_TRANSLATE.category_movies))
        vArt.setTitle(t(API_TRANSLATE.category_art))

        vNext.setOnClickListener { finish() }

        updateFinishEnabled()
    }

    private fun updateFinishEnabled() {
        var b = false
        for (i in list) if (i.isChecked()) b = true
        vNext.isEnabled = b
    }

    private fun finish() {
        val categories = ArrayList<Long>()
        if (vGames.isChecked()) categories.add(API.CATEGORY_GAMES)
        if (vAnime.isChecked()) categories.add(API.CATEGORY_ANIME)
        if (vArt.isChecked()) categories.add(API.CATEGORY_ART)
        if (vMovies.isChecked()) categories.add(API.CATEGORY_MOVIES)
        if (!demoMode) {
            categories.add(API.CATEGORY_OTHER)
            ControllerSettings.feedCategories = categories.toTypedArray()
        }
        screen.toNextScreen()
    }
}
