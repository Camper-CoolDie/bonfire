package com.dzen.campfire.server.executors.tags

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.publications.moderations.tags.ModerationTagRemove
import com.dzen.campfire.api.models.publications.tags.PublicationTag
import com.dzen.campfire.api.requests.tags.RTagsRemove
import com.dzen.campfire.server.controllers.ControllerFandom
import com.dzen.campfire.server.controllers.ControllerFandomTags
import com.dzen.campfire.server.controllers.ControllerModeration
import com.dzen.campfire.server.controllers.ControllerPublications
import com.dzen.campfire.api.tools.ApiException


class ETagsRemove : RTagsRemove("", 0) {

    private var publication: PublicationTag? = null
    private var publicationParent: PublicationTag? = null

    @Throws(ApiException::class)
    override fun check() {

        comment = ControllerModeration.parseComment(comment, apiAccount.id)
        publication = ControllerFandomTags.getTag(publicationId)

        if (publication == null) throw ApiException(API.ERROR_GONE)

        if (publication!!.publicationType != API.PUBLICATION_TYPE_TAG) throw ApiException(E_BAD_TYPE)

        ControllerFandom.checkCanOrThrow(apiAccount, publication!!.fandom.id, publication!!.fandom.languageId, API.LVL_MODERATOR_TAGS)
    }

    override fun execute(): Response {
        if (publication!!.parentPublicationId != 0L) {
            publicationParent = ControllerFandomTags.getTag(publication!!.parentPublicationId)
        }

        val (newTagsCount, newCategoriesCount) = ControllerFandomTags.removeTag(publication!!.id)

        ControllerPublications.moderation(
            ModerationTagRemove(
                comment,
                publication!!.id,
                publication!!.parentPublicationId,
                publication!!.name,
                publication!!.imageId,
                publicationParent?.name ?: null,
                publicationParent?.imageId ?: 0
            ),
            apiAccount.id,
            publication!!.fandom.id,
            publication!!.fandom.languageId,
            publication!!.id
        )

        return Response(newTagsCount, newCategoriesCount)
    }

}
