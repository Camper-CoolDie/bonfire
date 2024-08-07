package com.dzen.campfire.server.executors.post

import com.dzen.campfire.api.API
import com.dzen.campfire.api.requests.post.RPostGetAllByRubric
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.ControllerOptimizer
import com.dzen.campfire.server.controllers.ControllerPost.filterNsfw
import com.dzen.campfire.server.controllers.ControllerPublications
import com.dzen.campfire.server.tables.TPublications
import com.sup.dev.java_pc.sql.Database

class EPostGetAllByRubric : RPostGetAllByRubric(0, 0) {

    @Throws(ApiException::class)
    override fun check() {

    }

    override fun execute(): Response {

        val select = ControllerPublications.instanceSelect(apiAccount.id)
                .where(TPublications.tag_6, "=", rubricId)
                .where(TPublications.status, "=", API.STATUS_PUBLIC)
                .filterNsfw(apiAccount.id, requestApiVersion)
                .sort(TPublications.date_create, false)
                .offset_count(offset, COUNT)

        ControllerOptimizer.putCollisionWithCheck(apiAccount.id, API.COLLISION_ACHIEVEMENT_TAG_SEARCH)

        var array = ControllerPublications.parseSelect(Database.select("EPostGetAllByRubric", select))
        array = ControllerPublications.loadSpecDataForPosts(apiAccount.id, array)

        return Response(array)
    }
}
