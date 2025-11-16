package com.sayzen.campfiresdk.screens.fandoms.view

import android.view.View
import android.widget.TextView
import com.dzen.campfire.api.API_TRANSLATE
import com.sayzen.campfiresdk.R
import com.sayzen.campfiresdk.controllers.ControllerApi
import com.sayzen.campfiresdk.controllers.ControllerKarma
import com.sayzen.campfiresdk.controllers.t
import com.sayzen.campfiresdk.models.events.fandom.EventFandomRemoveModerator
import com.sayzen.campfiresdk.screens.activities.user_activities.SRelayRacesList
import com.sayzen.campfiresdk.screens.fandoms.SSubscribers
import com.sayzen.campfiresdk.screens.fandoms.STags
import com.sayzen.campfiresdk.screens.fandoms.chats.SFandomChatsList
import com.sayzen.campfiresdk.screens.fandoms.moderation.moderators.SModeration
import com.sayzen.campfiresdk.screens.fandoms.rating.SRating
import com.sayzen.campfiresdk.screens.fandoms.rubrics.SRubricsList
import com.sayzen.campfiresdk.screens.wiki.SWikiList
import com.sayzen.campfiresdk.support.adapters.XFandom
import com.sup.dev.android.libs.screens.navigator.Navigator
import com.sup.dev.android.views.cards.Card
import com.sup.dev.android.views.settings.SettingsMini
import com.sup.dev.android.views.views.layouts.LayoutCorned
import com.sup.dev.java.libs.eventBus.EventBus

class CardButtons(
        private val xFandom: XFandom
) : Card(R.layout.screen_fandom_card_buttons) {
    private val eventBus = EventBus
            .subscribe(EventFandomRemoveModerator::class) { onEventFandomRemoveModerator(it) }

    var loaded = false
    var expanded = false

    var subChatsCount = 0L
    var tagsCount = 0L
    var categoriesCount = 0L
    var karma30 = 0L
    var subscribersCount = 0L
    var rubricsCount = 0L
    var wikiArticlesCount = 0L
    var relayRacesCount = 0L

    override fun bindView(view: View) {
        super.bindView(view)
        val vLayoutCorned: LayoutCorned = view.findViewById(R.id.vLayoutCorned)

        val vKarmaButton: SettingsMini = view.findViewById(R.id.vKarmaButton)
        val vTagsButton: SettingsMini = view.findViewById(R.id.vTagsButton)
        val vWikiButton: SettingsMini = view.findViewById(R.id.vWikiButton)
        val vRubricsButton: SettingsMini = view.findViewById(R.id.vRubricsButton)
        val vRelayRacesButton: SettingsMini = view.findViewById(R.id.vRelayRacesButton)

        val vShowMoreTouch: View = view.findViewById(R.id.vShowMoreTouch)
        val vChatsButton: SettingsMini = view.findViewById(R.id.vChatsButton)
        val vSubscribersButton: SettingsMini = view.findViewById(R.id.vSubscribersButton)
        val vModerationButton: SettingsMini = view.findViewById(R.id.vModerationButton)

        vKarmaButton.setTitle(t(API_TRANSLATE.app_karma))
        vTagsButton.setTitle(t(API_TRANSLATE.app_tags))
        vWikiButton.setTitle(t(API_TRANSLATE.app_wiki))
        vRubricsButton.setTitle(t(API_TRANSLATE.app_rubrics))
        vRelayRacesButton.setTitle(t(API_TRANSLATE.app_relay_races))
        vChatsButton.setTitle(t(API_TRANSLATE.app_chats))
        vSubscribersButton.setTitle(t(API_TRANSLATE.app_subscribers))
        vModerationButton.setTitle(t(API_TRANSLATE.app_moderation))

        vLayoutCorned.makeSoftware()
        vKarmaButton.setOnClickListener { Navigator.to(SRating(xFandom.getId(), xFandom.getLanguageId())) }
        vTagsButton.setOnClickListener { STags.instance(xFandom.getId(), xFandom.getLanguageId(), Navigator.TO) }
        vWikiButton.setOnClickListener { Navigator.to(SWikiList(xFandom.getId(), xFandom.getLanguageId(), 0, "")) }
        vRubricsButton.setOnClickListener { Navigator.to(SRubricsList(xFandom.getId(), xFandom.getLanguageId(), 0, true)) }
        vRelayRacesButton.setOnClickListener { Navigator.to(SRelayRacesList(xFandom.getId(), xFandom.getLanguageId())) }
        vChatsButton.setOnClickListener { Navigator.to(SFandomChatsList(xFandom.getId(), xFandom.getLanguageId())) }
        vSubscribersButton.setOnClickListener { Navigator.to(SSubscribers(xFandom.getId(), xFandom.getLanguageId())) }
        vModerationButton.setOnClickListener { Navigator.to(SModeration(xFandom.getId(), xFandom.getLanguageId())) }
        vShowMoreTouch.setOnClickListener {
            expanded = !expanded
            updateInfo()
        }

        updateInfo()
    }

    fun updateInfo() {
        val view = getView() ?: return

        val vShowMore: TextView = view.findViewById(R.id.vShowMore)
        val vContainerDetails: View = view.findViewById(R.id.vContainerDetails)
        vShowMore.setText(if (expanded) t(API_TRANSLATE.fandom_hide_details) else t(API_TRANSLATE.fandom_show_details))
        vContainerDetails.visibility = if (expanded) View.VISIBLE else View.GONE

        if (loaded) {
            val vKarmaButton: SettingsMini = view.findViewById(R.id.vKarmaButton)
            val vTagsButton: SettingsMini = view.findViewById(R.id.vTagsButton)
            val vWikiButton: SettingsMini = view.findViewById(R.id.vWikiButton)
            val vRubricsButton: SettingsMini = view.findViewById(R.id.vRubricsButton)
            val vRelayRacesButton: SettingsMini = view.findViewById(R.id.vRelayRacesButton)
            val vChatsButton: SettingsMini = view.findViewById(R.id.vChatsButton)
            val vSubscribersButton: SettingsMini = view.findViewById(R.id.vSubscribersButton)

            val karmaColor = ControllerKarma.getKarmaColorHex(karma30)
            vKarmaButton.setSubtitle(t(API_TRANSLATE.fandom_button_karma_subtitle, "{${karmaColor} ${karma30}}"))
            ControllerApi.makeTextHtml(vKarmaButton.vSubtitle!!)

            vTagsButton.setSubtitle(t(API_TRANSLATE.fandom_button_tags_subtitle, tagsCount, categoriesCount))
            vWikiButton.setSubtitle(t(API_TRANSLATE.fandom_button_wiki_subtitle, wikiArticlesCount))
            vRubricsButton.setSubtitle(rubricsCount.toString())
            vRelayRacesButton.setSubtitle(relayRacesCount.toString())
            vChatsButton.setSubtitle((subChatsCount + 1).toString()) // +1 for root chat
            vSubscribersButton.setSubtitle(subscribersCount.toString())
        }
    }

    fun setParams(
        subChatsCount: Long,
        tagsCount: Long,
        categoriesCount: Long,
        karma30: Long,
        subscribersCount: Long,
        rubricsCount: Long,
        wikiArticlesCount: Long,
        relayRacesCount: Long,
    ) {
        this.subChatsCount = subChatsCount
        this.tagsCount = tagsCount
        this.categoriesCount = categoriesCount
        this.karma30 = karma30
        this.subscribersCount = subscribersCount
        this.rubricsCount = rubricsCount
        this.wikiArticlesCount = wikiArticlesCount
        this.relayRacesCount = relayRacesCount

        loaded = true
        updateInfo()
    }

    //
    //  EventBus
    //

    private fun onEventFandomRemoveModerator(e: EventFandomRemoveModerator) {
        if (e.fandomId == xFandom.getId() && e.languageId == xFandom.getLanguageId()) {
            update()
        }
    }

}
