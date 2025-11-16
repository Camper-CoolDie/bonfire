package com.dzen.campfire.server.executors.post

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.publications.history.HistoryAdminPinFandom
import com.dzen.campfire.api.models.publications.history.HistoryAdminUnpinFandom
import com.dzen.campfire.api.models.publications.moderations.posts.ModerationPinPostInFandom
import com.dzen.campfire.api.models.publications.post.PublicationPost
import com.dzen.campfire.api.requests.post.RPostPinFandom
import com.dzen.campfire.server.controllers.*
import com.dzen.campfire.api.tools.ApiException

class EPostPinFandom : RPostPinFandom(0, 0, 0, "") {
    override fun check() {
        comment = ControllerModeration.parseComment(comment, apiAccount.id)

        if (postId > 0L) {
            val publication = ControllerPublications.getPublication(postId, apiAccount.id)
            if (publication == null) throw ApiException(API.ERROR_GONE)
            if (publication !is PublicationPost) throw ApiException(API.ERROR_GONE)

            ControllerFandom.checkCan(apiAccount, publication.fandom.id, publication.fandom.languageId, API.LVL_MODERATOR_PIN_POST)
        } else {

            ControllerFandom.checkCan(apiAccount, fandomId, languageId, API.LVL_MODERATOR_PIN_POST)
        }
    }

    override fun execute(): Response {
        val oldPostId = ControllerFandom.changePinnedPost(fandomId, languageId, postId)
        if (postId != oldPostId) {
            ControllerPublications.moderation(
                ModerationPinPostInFandom(comment, postId, oldPostId),
                apiAccount.id,
                fandomId,
                languageId,
                postId
            )

            if (postId > 0L) {
                ControllerPublicationsHistory.put(
                    postId,
                    HistoryAdminPinFandom(apiAccount.id, apiAccount.imageId, apiAccount.name, comment)
                )
            }

            ControllerPublicationsHistory.put(
                oldPostId,
                HistoryAdminUnpinFandom(apiAccount.id, apiAccount.imageId, apiAccount.name, comment)
            )
        }

        return Response()
    }
}
