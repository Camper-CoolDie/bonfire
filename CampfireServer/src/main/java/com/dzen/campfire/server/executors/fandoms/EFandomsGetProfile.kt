package com.dzen.campfire.server.executors.fandoms

import com.dzen.campfire.api.requests.fandoms.RFandomsGetProfile

class EFandomsGetProfile : RFandomsGetProfile(0, 0){


    override fun execute(): Response {

        val viceExecutor = EFandomsViceroyGet()
        viceExecutor.apiAccount = apiAccount
        viceExecutor.fandomId = fandomId
        viceExecutor.languageId = languageId
        val vice = viceExecutor.execute()

        val pinExecutor = EFandomsGetPinedPost()
        pinExecutor.apiAccount = apiAccount
        pinExecutor.fandomId = fandomId
        pinExecutor.languageId = languageId
        val pin = pinExecutor.execute()

        val subExecutor = EFandomsGetSubscribtion()
        subExecutor.apiAccount = apiAccount
        subExecutor.fandomId = fandomId
        subExecutor.languageId = languageId
        val sub = subExecutor.execute()

        val backExecutor = EFandomsGetBackground()
        backExecutor.apiAccount = apiAccount
        backExecutor.fandomId = fandomId
        backExecutor.languageId = languageId
        val back = backExecutor.execute()

        // TODO
        val subChatsCount = 0L
        val tagsCount = 0L
        val categoriesCount = 0L
        val karma30 = 0L
        val subscribersCount = 0L
        val rubricsCount = 0L
        val wikiArticlesCount = 0L
        val relayRacesCount = 0L

        return Response(
            vice.account, vice.date,
            pin.pinnedPost,
            sub.subscriptionType, sub.notifyImportant,
            back.imageTitleId, back.imageTitleGifId,
            subChatsCount,
            tagsCount, categoriesCount,
            karma30,
            subscribersCount,
            rubricsCount,
            wikiArticlesCount,
            relayRacesCount,
        )
    }


}
