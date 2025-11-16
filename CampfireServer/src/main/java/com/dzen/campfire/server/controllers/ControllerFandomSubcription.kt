package com.dzen.campfire.server.controllers

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.fandoms.Account
import com.dzen.campfire.api.models.fandoms.Fandom
import com.dzen.campfire.server.tables.TCollisions
import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.sql.SqlQueryRemove
import com.sup.dev.java_pc.sql.SqlQuerySelect

object ControllerFandomSubscription {
    fun getSubscription(
        fandomId: Long,
        languageId: Long,
        accountId: Long
    ): Pair<Long, Boolean> { // Pair(type, notifyImportant)
        return Pair(
            ControllerCollisions.getCollisionValue1OrDef(
                accountId,
                fandomId,
                languageId,
                API.COLLISION_FANDOM_SUBSCRIBE,
                API.PUBLICATION_IMPORTANT_NONE
            ),
            ControllerCollisions.checkCollisionExist(
                accountId,
                fandomId,
                languageId,
                API.COLLISION_FANDOM_NOTIFY_IMPORTANT,
            )
        )
    }

    fun getSubscriptions(accountId: Long): Array<Pair<Long, Long>> { // Pair(fandomId, languageId)
        val v = Database.select(
            "ControllerFandom.getSubscriptions",
            SqlQuerySelect(TCollisions.NAME, TCollisions.collision_id, TCollisions.collision_sub_id)
                .where(TCollisions.collision_type, "=", API.COLLISION_FANDOM_SUBSCRIBE)
                .where(TCollisions.owner_id, "=", accountId)
        )
        return Array(v.rowsCount) { Pair(v.next(), v.next()) }
    }

    fun getSubscriptions(accountId: Long, count: Long, offset: Long): Array<Fandom> {
        val v = Database.select(
            "ControllerFandom.getSubscriptions", 17,
            """
                select
                    collision_id, collision_sub_id,
                    f.name,
                    f.image_id, f.image_title_id,
                    f.date_create,
                    f.creator_id,
                    f.subscribers_count,
                    f.status,
                    f.fandom_category,
                    f.fandom_closed,
                    f.karma_cof,
                    f.wiki_articles_count,
                    f.params1, f.params2, f.params3, f.params4
                from collisions
                join fandoms as f on f.id = collision_id
                where collision_type = ${API.COLLISION_FANDOM_SUBSCRIBE}
                    and owner_id = ?
                order by f.subscribers_count desc, f.date_create
                limit ? offset ?
            """,
            accountId, count, offset
        )
        return ControllerFandom.parseSelectArrayWithLanguageId(v)
    }

    fun getSubscribers(fandomId: Long, languageId: Long, notifyImportant: Boolean): Array<Long> {
        val v = Database.select(
            "ControllerFandom.getSubscribers",
            SqlQuerySelect(TCollisions.NAME, TCollisions.owner_id)
                .where(
                    TCollisions.collision_type, "=",
                    if (notifyImportant) API.COLLISION_FANDOM_NOTIFY_IMPORTANT
                    else API.COLLISION_FANDOM_SUBSCRIBE
                )
                .where(TCollisions.collision_id, "=", fandomId)
                .where(TCollisions.collision_sub_id, "=", languageId)
                .where(
                    TCollisions.value_1, "!=",
                    if (notifyImportant) Long.MIN_VALUE
                    else API.PUBLICATION_IMPORTANT_NONE
                )
        )
        return Array(v.rowsCount) { v.next() }
    }

    fun getSubscribers(fandomId: Long, languageId: Long, count: Long, offset: Long): Array<Account> {
        val v = Database.select(
            "ControllerFandom.getSubscribers", 7,
            """
                select
                    owner_id,
                    a.lvl,
                    a.last_online_time,
                    a.name,
                    a.img_id,
                    a.sex,
                    a.karma_count_30
                from collisions
                join accounts as a on a.id = owner_id
                where collision_type = ${API.COLLISION_FANDOM_SUBSCRIBE}
                    and collision_id = ?
                    and collision_sub_id = ?
                order by date_create desc
                limit ? offset ?
            """,
            fandomId, languageId, count, offset
        )
        return ControllerAccounts.parseSelect(v)
    }

    fun isSubscribed(
        fandomId: Long,
        languageId: Long,
        accountId: Long
    ) = ControllerCollisions.getCollisionValue1OrDef(
        accountId,
        fandomId,
        languageId,
        API.COLLISION_FANDOM_SUBSCRIBE,
        API.PUBLICATION_IMPORTANT_NONE
    ) != API.PUBLICATION_IMPORTANT_NONE

    fun changeSubscription(
        fandomId: Long,
        languageId: Long,
        accountId: Long,
        type: Long,
        notifyImportant: Boolean
    ): Pair<Long, Long> { // Pair(subscribersCountTotal, subscribersCountLanguage)
        Database.remove(
            "ControllerFandom.changeSubscription delete",
            SqlQueryRemove(TCollisions.NAME)
                .where(TCollisions.collision_type, "in",
                    "(${API.COLLISION_FANDOM_SUBSCRIBE}, ${API.COLLISION_FANDOM_NOTIFY_IMPORTANT})")
                .where(TCollisions.owner_id, "=", accountId)
                .where(TCollisions.collision_id, "=", fandomId)
                .where(TCollisions.collision_sub_id, "=", languageId)
        )

        if (type != API.PUBLICATION_IMPORTANT_NONE) {
            ControllerCollisions.putCollisionValue1(
                accountId,
                fandomId,
                languageId,
                API.COLLISION_FANDOM_SUBSCRIBE,
                type
            )
        }
        if (type != API.PUBLICATION_IMPORTANT_NONE && notifyImportant) {
            ControllerCollisions.putCollisionValue1(
                accountId,
                fandomId,
                languageId,
                API.COLLISION_FANDOM_NOTIFY_IMPORTANT,
                1
            )
        }

        ControllerAccounts.updateSubscribeTag(accountId)
        val v = Database.select(
            "ControllerFandom.changeSubscription updateCount",
            SqlQuerySelect("update_subscribers_count(?, ?)", "count_total", "count_language")
                .value(fandomId)
                .value(languageId)
        )
        return Pair(v.next(), v.next())
    }
}
