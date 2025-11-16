package com.dzen.campfire.server.executors.tags

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.publications.moderations.tags.ModerationTagChange
import com.dzen.campfire.api.models.publications.tags.PublicationTag
import com.dzen.campfire.api.requests.tags.RTagsChange
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.ControllerFandom
import com.dzen.campfire.server.controllers.ControllerFandomTags
import com.dzen.campfire.server.controllers.ControllerModeration
import com.dzen.campfire.server.controllers.ControllerPublications
import com.sup.dev.java_pc.tools.ToolsImage


class ETagsChange : RTagsChange(0, null, "", null, false) {

    private var publication: PublicationTag? = null
    private var publicationParent: PublicationTag? = null
    private var oldName: String? = null
    private var oldImageId: Long = 0

    @Throws(ApiException::class)
    override fun check() {

        if (removeImage && image != null) throw ApiException(E_BAD_PARAMS)
        comment = ControllerModeration.parseComment(comment, apiAccount.id)

        publication = ControllerFandomTags.getTag(publicationId)

        if (publication == null) throw ApiException(API.ERROR_GONE)

        if (image != null) {
            if (image!!.size > API.TAG_IMAGE_WEIGHT) throw ApiException(E_BAD_IMAGE_WEIGHT)
            if (!ToolsImage.checkImageScaleUnknownType(image!!, API.TAG_IMAGE_SIDE, API.TAG_IMAGE_SIDE, true, false, true)) throw ApiException(E_BAD_IMAGE_SIZE)
        }

        if (name != null && (name!!.length < API.TAG_NAME_MIN_L || name!!.length > API.TAG_NAME_MAX_L)) throw ApiException(E_BAD_NAME_SIZE)

        ControllerFandom.checkCanOrThrow(apiAccount, publication!!.fandom.id, publication!!.fandom.languageId, API.LVL_MODERATOR_TAGS)
    }

    override fun execute(): Response {
        oldName = publication!!.name
        oldImageId = publication!!.imageId

        if (publication!!.parentPublicationId != 0L) {
            publicationParent = ControllerFandomTags.getTag(publication!!.parentPublicationId)
        }

        publication = ControllerFandomTags.changeTag(
            publication!!,
            name,
            if (removeImage) byteArrayOf() else image
        )

        ControllerPublications.moderation(
            ModerationTagChange(
                comment,
                publication.id,
                publication.parentPublicationId,
                publication.name,
                oldName,
                publication.imageId,
                oldImageId,
                publicationParent?.name ?: "",
                publicationParent?.imageId ?: 0
            ),
            apiAccount.id,
            publication.fandom.id,
            publication.fandom.languageId,
            publication.id
        )

        return Response()
    }

}
