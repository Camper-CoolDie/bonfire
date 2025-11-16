package com.dzen.campfire.server.controllers

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.fandoms.FandomLink
import com.dzen.campfire.server.tables.TFandoms
import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.sql.SqlQuerySelect
import com.sup.dev.java_pc.sql.SqlQueryUpdate

object ControllerFandomProfile {
    fun getCategory(fandomId: Long): Long {
        return Database.select(
            "ControllerFandom.getCategory",
            SqlQuerySelect(TFandoms.NAME, TFandoms.fandom_category)
                .where(TFandoms.id, "=", fandomId)
        ).next()
    }

    fun changeCategory(fandomId: Long, category: Long): Long {
        return Database.select(
            "ControllerFandom.changeCategory", 1,
            """
                update fandoms
                set fandom_category = ?
                where fandom_id = ?
                returning old.fandom_category
            """,
            category, fandomId
        ).next()
    }

    fun getParams(fandomId: Long): Array<Array<Long>> {
        val v = Database.select(
            "ControllerFandom.getParams",
            SqlQuerySelect(TFandoms.NAME, "params_1", "params_2", "params_3", "params_4")
                .where(TFandoms.id, "=", fandomId)
        )
        return Array(4) { v.next() }
    }

    fun changeParams(fandomId: Long, params: Array<Array<Long>>): Array<Array<Long>> {
        val v = Database.select(
            "ControllerFandom.changeParams", 4,
            """
                update fandoms
                set params_1 = ?,
                    params_2 = ?,
                    params_3 = ?,
                    params_4 = ?
                where fandom_id = ?
                returning old.params_1, old.params_2, old.params_3, old.params_4
            """,
            params[1], params[2], params[3], params[4], fandomId
        )
        return Array(4) { v.next() }
    }

    fun getKarmaCof(fandomId: Long): Long {
        return Database.select(
            "ControllerFandom.getKarmaCof",
            SqlQuerySelect(TFandoms.NAME, TFandoms.karma_cof)
                .where("fandom_id", "=", fandomId)
        ).next()
    }

    fun changeKarmaCof(fandomId: Long, karmaCof: Long): Long {
        return Database.select(
            "ControllerFandom.changeKarmaCof", 1,
            """
                update fandoms
                set karma_cof = ?
                where fandom_id = ?
                returning old.karma_cof
            """,
            karmaCof, fandomId
        ).next()
    }

    fun changeDescription(fandomId: Long, languageId: Long, description: String): String {
        return Database.select(
            "ControllerFandom.changeDescription", 1,
            """
                update fandom_profiles
                set description = ?
                where fandom_id = ? and language_id = ?
                returning old.description
            """,
            description, fandomId, languageId
        ).next()
    }

    fun changeRootChatBackground(fandomId: Long, languageId: Long, backgroundId: Long) {
        Database.update(
            "ControllerFandom.changeRootChatBackground",
            SqlQueryUpdate("fandom_profiles")
                .update("root_chat_background_id", "=", backgroundId)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun changeBackground(fandomId: Long, languageId: Long, backgroundId: Long, backgroundGifId: Long) {
        Database.update(
            "ControllerFandom.changeBackground",
            SqlQueryUpdate("fandom_profiles")
                .update("background_id", "=", backgroundId)
                .update("background_gif_id", "=", backgroundGifId)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun getViceroy(fandomId: Long, languageId: Long): Pair<Long, Long> { // Pair(viceroyId, viceroyAssignedAt)
        val v = Database.select(
            "ControllerFandom.getViceroy",
            SqlQuerySelect(
                "fandom_profiles",
                "viceroy_id",
                "extract(epoch from viceroy_assigned_at) * 1000"
            )
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
        return Pair(v.next(), v.next())
    }

    fun getViceroyId(fandomId: Long, languageId: Long): Long {
        return Database.select(
            "ControllerFandom.getViceroyId",
            SqlQuerySelect("fandom_profiles", "viceroy_id")
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        ).next()
    }

    fun changeViceroy(fandomId: Long, languageId: Long, viceroyId: Long): Long {
        return Database.select(
            "ControllerFandom.changeViceroy", 1,
            """
                update fandom_profiles
                set viceroy_id = ?, viceroy_assigned_at = now()
                where fandom_id = ? and language_id = ?
                returning old.viceroy_id
            """,
            viceroyId, fandomId, languageId
        ).next()
    }

    fun getAdditionalNames(fandomId: Long, languageId: Long): Array<String> {
        return Database.select(
            "ControllerFandom.getAdditionalNames",
            SqlQuerySelect("fandom_profiles", "additional_names")
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        ).next()
    }

    fun changeAdditionalNames(fandomId: Long, languageId: Long, names: Array<String>) {
        Database.update(
            "ControllerFandom.changeAdditionalNames",
            SqlQueryUpdate("fandom_profiles")
                .update("additional_names", names)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun getLinks(fandomId: Long, languageId: Long): Array<FandomLink> {
        val v = Database.select(
            "ControllerFandom.getLinks",
            SqlQuerySelect(
                "fandom_profiles, unnest(fandom_profiles.links) as l",
                "(l).icon", "(l).title", "(l).url"
            )
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
        return Array(v.rowsCount) { i ->
            FandomLink().apply {
                index = i
                title = v.next()
                url = v.next()
                imageIndex = v.next()
            }
        }
    }

    fun addLink(fandomId: Long, languageId: Long, link: FandomLink) {
        Database.update(
            "ControllerFandom.addLink",
            SqlQueryUpdate("fandom_profiles")
                .update("links", "array_append(links, row(?, ?, ?)::fandom_link)")
                .value(link.title)
                .value(link.url)
                .value(link.imageIndex)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun removeLink(fandomId: Long, languageId: Long, index: Long) {
        Database.update(
            "ControllerFandom.removeLink",
            SqlQueryUpdate("fandom_profiles")
                .update("links", "array_remove(links, links[?])")
                .value(index)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun changeLink(fandomId: Long, languageId: Long, link: FandomLink) {
        Database.update(
            "ControllerFandom.changeLink",
            SqlQueryUpdate("fandom_profiles")
                .value(linkId)
                .update("links", "array_replace(links, links[?], row(?, ?, ?)::fandom_link)")
                .value(link.index)
                .value(link.title)
                .value(link.url)
                .value(link.imageIndex)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun getGallery(fandomId: Long, languageId: Long): Array<Long> {
        return Database.select(
            "ControllerFandom.getGallery",
            SqlQuerySelect("fandom_profiles", "gallery")
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        ).next()
    }

    fun addGalleryImage(fandomId: Long, languageId: Long, imageId: Long) {
        Database.update(
            "ControllerFandom.addGalleryImage",
            SqlQueryUpdate("fandom_profiles")
                .update("gallery", "array_append(gallery, ?)")
                .value(imageId)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun removeGalleryImage(fandomId: Long, languageId: Long, imageId: Long) {
        Database.update(
            "ControllerFandom.removeGalleryImage",
            SqlQueryUpdate("fandom_profiles")
                .update("gallery", "array_remove(gallery, ?)")
                .value(imageId)
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        )
    }

    fun getPinnedPostId(fandomId: Long, languageId: Long): Long {
        return Database.select(
            "ControllerFandom.getPinnedPostId",
            SqlQuerySelect("fandom_profiles", "pinned_post_id")
                .where("fandom_id", "=", fandomId)
                .where("language_id", "=", languageId)
        ).next()
    }

    fun changePinnedPost(fandomId: Long, languageId: Long, postId: Long): Long {
        return Database.select(
            "ControllerFandom.changePinnedPost", 1,
            """
                update fandom_profiles
                set pinned_post_id = ?
                where fandom_id = ? and language_id = ?
                returning old.pinned_post_id
            """,
            postId, fandomId, languageId
        ).next()
    }
}
