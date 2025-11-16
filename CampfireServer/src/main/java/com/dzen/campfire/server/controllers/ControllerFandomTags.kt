package com.dzen.campfire.server.controllers

import com.dzen.campfire.api.API
import com.dzen.campfire.api.models.fandoms.Account
import com.dzen.campfire.api.models.publications.tags.PublicationTag
import com.dzen.campfire.server.tables.TPublications
import com.sup.dev.java.libs.json.Json
import com.sup.dev.java_pc.sql.Database
import com.sup.dev.java_pc.sql.SqlQueryUpdate

object ControllerFandomTags {
    fun getTag(tagId: Long): PublicationTag? {
        val v = Database.select(
            "ControllerFandom.getTag"
            ControllerPublications.instanceSelect(0)
                .where(TPublications.publication_type, "=", API.PUBLICATION_TYPE_TAG)
                .where(TPublications.id, "=", tagId)
                .where(TPublications.status, "=", API.STATUS_PUBLIC)
        )
        if (v.isEmpty()) return null

        val tag = ControllerPublications.parseSelect(v)[0] as PublicationTag
        tag.restoreFromJsonDB()
        return tag
    }

    fun getTags(fandomId: Long, languageId: Long): Array<PublicationTag> {
        val v = Database.select(
            "ControllerFandom.getTags"
            ControllerPublications.instanceSelect(0)
                .where(TPublications.publication_type, "=", API.PUBLICATION_TYPE_TAG)
                .where(TPublications.fandom_id, "=", fandomId)
                .where(TPublications.language_id, "=", languageId)
                .where(TPublications.status, "=", API.STATUS_PUBLIC)
        )

        return ControllerPublications
            .parseSelect(v)
            .reversed()
            .map {
                val tag = it as PublicationTag
                tag.restoreFromJsonDB()
                tag
            }
            .toTypedArray()
    }

    // the returned count depends on categoryId (0 - categoriesCount, non-zero - tagsCount)
    fun createTag(
        fandomId: Long,
        languageId: Long,
        creator: Account,
        name: String,
        icon: ByteArray,
        categoryId: Long
    ): Pair<PublicationTag, Long> { // Pair(publicationTag, count)
        val tag = PublicationTag().apply {
            fandom.id = fandomId
            fandom.languageId = languageId
            this.creator = creator
            dateCreate = System.currentTimeMillis()
            tag_1 = dateCreate
            status = API.STATUS_PUBLIC
            category = getCategory(fandomId)

            this.name = name
            parentPublicationId = categoryId
            if (!icon.isEmpty()) {
                imageId = ControllerResources.put(icon, API.RESOURCES_PUBLICATION_TAG)
            }
        }

        tag.jsonDB = tag.jsonDB(true, Json())
        tag.id = Database.insert(
            "ControllerFandom.createTag",
            TPublications.NAME,
            TPublications.fandom_id, fandomId,
            TPublications.language_id, languageId,
            TPublications.publication_category, tag.category,
            TPublications.publication_type, tag.publicationType
            TPublications.date_create, tag.dateCreate,
            TPublications.creator_id, creator.id,
            TPublications.publication_json, tag.jsonDB,
            TPublications.parent_publication_id, categoryId,
            TPublications.tag_1, tag.tag_1,
            TPublications.status, tag.status
        )

        val function = "update_" + (if (categoryId == 0) "categories" else "tags") + "_count(?, ?)"
        val v = Database.select(
            "ControllerFandom.createTag updateCount", 1,
            "select $function",
            fandomId, languageId
        )
        return Pair(tag, v.next())
    }

    // if tagId is a category, all tags under it are removed too
    fun removeTag(tagId: Long): Pair<Long, Long> { // Pair(tagsCount, categoriesCount)
        val v = Database.select(
            "ControllerFandom.removeTag", 2,
            """
                update units
                set status = ${API.STATUS_DEEP_BLOCKED}
                where id = ? or parent_unit_id = ?
                returning new.fandom_id, new.language_id
            """,
            tagId, tagId
        )
        val fandomId = v.next()
        val languageId = v.next()

        val v = Database.select(
            "ControllerFandom.removeTag updateCount", 2,
            "select update_tags_count(?, ?), update_categories_count(?, ?)",
            fandomId, languageId, fandomId, languageId
        )
        return Pair(v.next(), v.next())
    }

    // to remove the icon, pass empty ByteArray
    fun changeTag(tag: PublicationTag, name: String?, icon: ByteArray?): PublicationTag {
        if (name != null) {
            tag.name = name
        }

        if (icon != null) {
            if (icon.isEmpty()) {
                ControllerResources.remove(tag.imageId)
                tag.imageId = 0L
            } else if (tag.imageId == 0L) {
                tag.imageId = ControllerResources.put(icon, API.RESOURCES_PUBLICATION_TAG)
            } else {
                tag.imageId = ControllerResources.removeAndPut(tag.imageId, icon, API.RESOURCES_PUBLICATION_TAG)
            }
        }

        Database.update(
            "ControllerFandom.changeTag",
            SqlQueryUpdate(TPublications.NAME)
                .where(TPublications.publication_type, "=", API.PUBLICATION_TYPE_TAG)
                .where(TPublications.id, "=", tagId)
                .updateValue(TPublications.publication_json, tag.jsonDB(true, Json()))
        )
        return tag
    }

    fun moveTag(tagId: Long, categoryId: Long): Long {
        val oldCategoryId = Database.select(
            "ControllerFandom.moveTag", 1,
            """
                update units
                set parent_unit_id = ?
                where unit_type = ${API.PUBLICATION_TYPE_TAG}
                    and parent_unit_id != 0
                    and id = ?
                returning old.parent_unit_id
            """,
            categoryId, tagId
        ).next()

        // i have zero understanding how this part works and i can't optimize it even more
        val v = Database.select(
            "ControllerFandom.moveTag selectPostTagsIds", 2
            """
                select owner_id, array_agg(c.collision_id) from collisions
                where collision_type = ${API.COLLISION_TAG}
                    and collision_id = ?
                left join collisions c on collision_type = ${API.COLLISION_TAG}
                    and c.owner_id = owner_id
            """,
            tagId
        )

        while (v.hasNext()) {
            val postId = v.next()
            val tagsIds = v.next()
            val tags = ControllerPublications.parseSelect(Database.select(
                "ControllerFandom.moveTag selectPostTags",
                ControllerPublications.instanceSelect(0)
                    .where(TPublications.publication_type, "=", API.PUBLICATION_TYPE_TAG)
                    .where(TPublications.id, "in", tagsIds)
                    .where(TPublications.status, "=", API.STATUS_PUBLIC)
            )).map { it as PublicationTag }.toTypedArray()

            val resTagsIds = ArrayList<Long>()
            for (tag in tags) {
                if (tag.parentPublicationId == 0L) continue

                if (!resTagsIds.contains(tag.id)) resTagsIds.add(tag.id)
                if (!resTagsIds.contains(tag.parentPublicationId)) resTagsIds.add(tag.parentPublicationId)
            }

            ControllerPublications.removeCollisions(postId, API.COLLISION_TAG)
            ControllerPublications.putCollisions(postId, resTagsIds.toTypedArray(), API.COLLISION_TAG)
        }

        return oldCategoryId
    }

    fun moveCategoryBefore(categoryId: Long, beforeCategoryId: Long) {
        // TODO
        TODO("ETagsMoveCategory")
    }

    fun moveTagBefore(tagId: Long, beforeTagId: Long) {
        // TODO
        TODO("ETagsMoveTag")
    }
}
