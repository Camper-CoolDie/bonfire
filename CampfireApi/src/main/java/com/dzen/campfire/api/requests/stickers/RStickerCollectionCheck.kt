package com.dzen.campfire.api.requests.stickers

import com.dzen.campfire.api.tools.client.Request
import com.sup.dev.java.libs.json.Json

open class RStickerCollectionCheck(
        var stickerId: Long
) : Request<RStickerCollectionCheck.Response>() {

    override fun jsonSub(inp: Boolean, json: Json) {
        stickerId = json.m(inp, "stickerId", stickerId)
    }

    override fun instanceResponse(json: Json): Response {
        return Response(json)
    }

    class Response : Request.Response {

        var inCollection = false

        constructor(json: Json) {
            json(false, json)
        }

        constructor(inCollection:Boolean) {
            this.inCollection=inCollection
        }

        override fun json(inp: Boolean, json: Json) {
            inCollection = json.m(inp, "inCollection", inCollection)
        }

    }

}
