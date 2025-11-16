package com.dzen.campfire.server.executors.tags

import com.dzen.campfire.api.requests.tags.RTagsGetAll
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.ControllerFandomTags

class ETagsGetAll : RTagsGetAll(0, 0) {

    @Throws(ApiException::class)
    override fun check() {
        super.check()
    }

    override fun execute(): Response {
        val publications = ControllerFandomTags.getTags(fandomId, languageId)

        return Response(publications)
    }
}
