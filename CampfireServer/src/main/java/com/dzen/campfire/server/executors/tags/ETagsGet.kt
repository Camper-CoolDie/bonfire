package com.dzen.campfire.server.executors.tags

import com.dzen.campfire.api.API
import com.dzen.campfire.api.requests.tags.RTagsGet
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.ControllerFandomTags

class ETagsGet : RTagsGet(0) {

    @Throws(ApiException::class)
    override fun check() {
        super.check()
    }

    override fun execute(): Response {
        val publication = ControllerFandomTags.getTag(tagId)
        if (publication == null) throw ApiException(API.ERROR_GONE)

        return Response(publication)
    }
}
