package com.dzen.campfire.server.controllers

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.fandoms.Account
import com.dzen.campfire.api.models.fandoms.Fandom
import com.dzen.campfire.api.models.lvl.LvlInfoAdmin
import com.dzen.campfire.api.models.lvl.LvlInfoModeration
import com.dzen.campfire.api.models.lvl.LvlInfoUser
import com.dzen.campfire.api.tools.ApiAccount
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.optimizers.OptimizerEffects
import com.dzen.campfire.server.tables.TCollisions
import com.dzen.campfire.server.tables.TFandoms
import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.sql.SqlQuerySelect

object ControllerFandom {
    fun getFandom(fandomId: Long): Fandom? {
        val v = Database.select(
            "ControllerFandom.get",
            instanceSelect(fandomId)
        )

        if (v.isEmpty()) return null
        return parseSelect(v)
    }

    fun instanceSelect(fandomId: Long) = instanceSelect().where(TFandoms.id, "=", fandomId)
    fun instanceSelect() = SqlQuerySelect(TFandoms.NAME,
        TFandoms.id,
        TFandoms.name,
        TFandoms.image_id,
        TFandoms.image_title_id,
        TFandoms.date_create,
        TFandoms.creator_id,
        TFandoms.subscribers_count,
        TFandoms.status,
        TFandoms.fandom_category,
        TFandoms.fandom_closed,
        TFandoms.karma_cof,
        "wiki_articles_count",
        "params_1",
        "params_2",
        "params_3",
        "params_4"
    )

    fun parseSelectArray(v: ResultRows): Array<Fandom> = Array(v.rowsCount) { parseSelect(v) }
    fun parseSelect(v: ResultRows): Fandom {
        return Fandom().apply {
            id = v.next()
            name = v.next()
            imageId = v.next()
            imageTitleId = v.next()
            dateCreate = v.next()
            creatorId = v.next()
            subscribersCount = v.next()
            status = v.next()
            category = v.next()
            closed = v.next<Int>() != 0
            karmaCof = v.next()
            wikiArticlesCount = v.next()
            params = Array(4) { v.next() }
        }
    }

    fun parseSelectArrayWithLanguageId(
        v: ResultRows
    ): Array<Fandom> = Array(v.rowsCount) { parseSelectWithLanguageId(v) }

    fun parseSelectWithLanguageId(v: ResultRows): Fandom {
        return Fandom().apply {
            id = v.next()
            languageId = v.next()
            name = v.next()
            imageId = v.next()
            imageTitleId = v.next()
            dateCreate = v.next()
            creatorId = v.next()
            subscribersCount = v.next()
            status = v.next()
            category = v.next()
            closed = v.next<Int>() != 0
            karmaCof = v.next()
            wikiArticlesCount = v.next()
            params = Array(4) { v.next() }
        }
    }

    fun getKarma30(fandomId: Long, languageId: Long, accountId: Long): Long {
        return ControllerCollisions.getCollisionValue1(
            accountId,
            fandomId,
            languageId,
            API.COLLISION_KARMA_30
        )
    }

    fun getModerators(fandomId: Long, languageId: Long): Array<Long> {
        val v = Database.select(
            "ControllerFandom.getModerators", 1
            """
                select owner_id from collisions
                join accounts as a on a.id = collisions.owner_id
                where collision_type = ${API.COLLISION_KARMA_30}
                    and collision_id = ?
                    and collision_sub_id = ?
                    and (a.lvl >= ${API.LVL_MODERATOR_BLOCK.lvl} and value_1 >= ${API.LVL_MODERATOR_BLOCK.karmaCount}
                    and (a.lvl < ${API.LVL_ADMIN_MODER.lvl} or value_1 < ${API.LVL_ADMIN_MODER.karmaCount})
            """,
            fandomId, languageId
        )
        return Array(v.rowsCount) { v.next() }
    }

    fun getModerators(fandomId: Long, languageId: Long, count: Long, offset: Long): Array<Account> {
        val v = Database.select(
            "ControllerFandom.getModerators", 7
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
                join accounts as a on a.id = collisions.owner_id
                where collision_type = ${API.COLLISION_KARMA_30}
                    and collision_id = ?
                    and collision_sub_id = ?
                    and (a.lvl >= ${API.LVL_MODERATOR_BLOCK.lvl} and value_1 >= ${API.LVL_MODERATOR_BLOCK.karmaCount}
                    and (a.lvl < ${API.LVL_ADMIN_MODER.lvl} or value_1 < ${API.LVL_ADMIN_MODER.karmaCount})
                order by date_create desc
                limit ? offset ?
            """,
            fandomId, languageId, count, offset
        )
        return ControllerAccounts.parseSelect(v)
    }

    fun getModerationFandomsCount(accountId: Long): Long {
        return Database.select(
            "ControllerFandom.getModerationFandomsCount",
            SqlQuerySelect(TCollisions.NAME, "count(*)")
                .where(TCollisions.collision_type, "=", API.COLLISION_KARMA_30)
                .where(TCollisions.owner_id, "=", accountId)
                .where(TCollisions.value_1, ">=", API.LVL_MODERATOR_BLOCK.karmaCount)
        ).nextLongOrZero()
    }

    //
    // Checkers
    //

    fun checkExists(fandomId: Long): Boolean {
        return Database.select(
            "ControllerFandom.checkExist",
            SqlQuerySelect(TFandoms.NAME, "count(*)")
                .where(TFandoms.id, "=", fandomId)
        ).next() > 0
    }

    fun checkCanOrThrow(account: ApiAccount, level: LvlInfoUser) {
        if (ControllerOptimizer.isProtoadmin(account.id)) return
        if (account.accessTag < level.lvl) throw ApiException(API.ERROR_ACCESS)
        if (level.karmaCount > 0 && account.accessTagSub < level.karmaCount) throw ApiException(API.ERROR_ACCESS)
        ControllerAccounts.checkAccountBanned(account.id)
    }

    fun checkCan(account: ApiAccount, level: LvlInfoUser): Boolean {
        try {
            checkCanOrThrow(account, level)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun checkCanOrThrow(account: ApiAccount, level: LvlInfoAdmin) {
        if (ControllerOptimizer.isProtoadmin(account.id)) return
        if (OptimizerEffects.get(account.id, API.EFFECT_INDEX_ADMIN_BAN) != null) throw ApiException(API.ERROR_ACCESS)
        if (account.accessTag < level.lvl) throw ApiException(API.ERROR_ACCESS)
        if (ControllerAccounts.isBot(account)) throw ApiException(API.ERROR_ACCESS)
        if (level.karmaCount > 0 && account.accessTagSub < level.karmaCount) throw ApiException(API.ERROR_ACCESS)
        ControllerAccounts.checkAccountBanned(account.id)
    }

    fun checkCan(account: ApiAccount, level: LvlInfoAdmin): Boolean {
        try {
            checkCanOrThrow(account, level)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun checkCanOrThrow(account: ApiAccount, fandomId: Long, languageId: Long, level: LvlInfoModeration) {
        if (ControllerOptimizer.isProtoadmin(account.id)) return
        if (account.id == getViceroyId(fandomId, languageId)) return
        if (OptimizerEffects.get(account.id, API.EFFECT_INDEX_ADMIN_BAN) != null) throw ApiException(API.ERROR_ACCESS)
        if (account.accessTag < level.lvl) throw ApiException(API.ERROR_ACCESS)
        if (ControllerAccounts.isBot(account)) throw ApiException(API.ERROR_ACCESS)
        checkCan(account, API.LVL_ADMIN_MODER)
        ControllerAccounts.checkAccountBanned(account.id, fandomId, languageId)
    }

    fun checkCan(account: ApiAccount, level: LvlInfoModeration): Boolean {
        try {
            checkCanOrThrow(account, level)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
