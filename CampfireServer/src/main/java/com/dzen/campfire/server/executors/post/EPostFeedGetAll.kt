package com.dzen.campfire.server.executors.post

import com.dzen.campfire.api.API
import com.dzen.campfire.api.requests.post.RPostFeedGetAll
import com.dzen.campfire.server.controllers.ControllerAccounts
import com.dzen.campfire.server.controllers.ControllerPost.filterNsfw
import com.dzen.campfire.server.controllers.ControllerPublications
import com.dzen.campfire.server.tables.TAccounts
import com.dzen.campfire.server.tables.TPublications
import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.sql.SqlWhere

class EPostFeedGetAll : RPostFeedGetAll(0, emptyArray(), false, 0, false, false) {

    override fun check() {
    }

    override fun execute(): Response {

        val ignoreFandomsIds = ControllerAccounts.getBlackListFandoms(apiAccount.id)

        val select = ControllerPublications.instanceSelect(apiAccount.id)
                .where(TPublications.publication_type, "=", API.PUBLICATION_TYPE_POST)
                .where(SqlWhere.WhereIN(TPublications.language_id, arrayOf(*languagesId, -1L)))
                .where(TPublications.status, "=", API.STATUS_PUBLIC)
                .where(TPublications.date_create, "<", if (offsetDate == 0L) Long.MAX_VALUE else offsetDate)
                .filterNsfw(apiAccount.id, requestApiVersion)
                .count(COUNT)
                .sort(TPublications.date_create, false)

        if(apiAccount.accessTag <= 300){
            select.where(TPublications.fandom_id, "<>", API.FANDOM_CAMPFIRE_HELLO_ID)
        }

        if(noSubscribes) {
            val subscribeTag: String = ControllerAccounts.get(apiAccount.id, TAccounts.subscribes).next()!!
            val split = subscribeTag.split(',')
            var subscribeTagFandoms = "'-1'"
            for (i in split) {
                val xxx = i.split("-")
                if (xxx.size > 1) subscribeTagFandoms += ",${xxx[0]}'"
            }

            if (subscribeTag.isNotEmpty()) {
                select.where(SqlWhere.WhereString(
                        "(${TPublications.fandom_id} NOT IN($subscribeTagFandoms))"
                ))
            }
        }

        select.where(TPublications.parent_fandom_closed, "=", 0)
        select.where(TPublications.closed, "=", 0)

        if (!noKarmaCategory) select.where(TPublications.tag_7, "=", karmaCategory)
        if(noSubscribes)
        if (importantOnly) select.where(TPublications.important, "=", API.PUBLICATION_IMPORTANT_IMPORTANT)
        if (ignoreFandomsIds.isNotEmpty()) select.where(SqlWhere.WhereIN(TPublications.fandom_id, true, ignoreFandomsIds))

        var posts = ControllerPublications.parseSelect(Database.select("EPostFeedGetAll", select))
        posts = ControllerPublications.loadSpecDataForPosts(apiAccount.id, posts, requestApiVersion)

        return Response(posts)
    }


}
