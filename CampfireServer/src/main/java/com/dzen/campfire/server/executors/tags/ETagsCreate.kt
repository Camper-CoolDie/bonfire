package com.dzen.campfire.server.executors.tags

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.publications.moderations.tags.ModerationTagCreate
import com.dzen.campfire.api.models.publications.tags.PublicationTag
import com.dzen.campfire.api.requests.tags.RTagsCreate
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.ControllerAccounts
import com.dzen.campfire.server.controllers.ControllerAchievements
import com.dzen.campfire.server.controllers.ControllerFandom
import com.dzen.campfire.server.controllers.ControllerFandomTags
import com.dzen.campfire.server.controllers.ControllerModeration
import com.dzen.campfire.server.controllers.ControllerOptimizer
import com.dzen.campfire.server.controllers.ControllerPublications

import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.tools.ToolsImage

class ETagsCreate : RTagsCreate("", "", 0, 0, 0, null) {

    private var publicationParent: PublicationTag? = null

    override fun check() {

        comment = ControllerModeration.parseComment(comment, apiAccount.id)

        if (parentId != 0L) {
            publicationParent = ControllerFandomTags.getTag(parentId)
            if (publicationParent == null) throw ApiException(E_PARENT_DONT_EXIST)
            publicationParent!!.restoreFromJsonDB()
            if (publicationParent!!.publicationType != API.PUBLICATION_TYPE_TAG || publicationParent!!.parentPublicationId != 0L) throw ApiException(E_PARENT_BAD_TYPE)
        }

        if (image != null) {
            if (image!!.size > API.TAG_IMAGE_WEIGHT) throw ApiException(E_BAD_IMAGE_WEIGHT)
            if (!ToolsImage.checkImageScaleUnknownType(image!!, API.TAG_IMAGE_SIDE, API.TAG_IMAGE_SIDE, true, false, true)) throw ApiException(E_BAD_IMAGE_SIZE)
        }

        if (name.length < API.TAG_NAME_MIN_L || name.length > API.TAG_NAME_MAX_L) throw ApiException(E_BAD_NAME_SIZE)

        ControllerFandom.checkCanOrThrow(apiAccount, fandomId, languageId, API.LVL_MODERATOR_TAGS)
    }

    override fun execute(): Response {
        val creator = ControllerAccounts.instance(
            apiAccount.id,
            apiAccount.accessTag,
            System.currentTimeMillis(),
            apiAccount.name,
            apiAccount.imageId,
            apiAccount.sex,
            apiAccount.accessTagSub
        )

        val (publication, newCount) = ControllerFandomTags.createTag(
            fandomId,
            languageId,
            creator,
            name,
            image ?: byteArrayOf(),
            parentId
        )

        ControllerPublications.moderation(
            ModerationTagCreate(
                comment,
                publication.id,
                publicationParent?.id ?: 0,
                publication.name,
                publication.imageId,
                publicationParent?.name ?: "",
                publicationParent?.imageId ?: 0
            ),
            apiAccount.id,
            fandomId,
            languageId,
            publication.id
        )

        ControllerOptimizer.putCollisionWithCheck(apiAccount.id, API.COLLISION_ACHIEVEMENT_TAG_CREATE)
        ControllerAchievements.addAchievementWithCheck(apiAccount.id, API.ACHI_CREATE_TAG)

        return Response(publication.id, newCount)
    }

}
