package com.dzen.campfire.server.controllers

import com.dzen.campfire.api.API
import com.dzen.campfire.api.tools.ApiException

object ControllerModeration {
    fun parseComment(comment: String, userId: Long = 0L): String {
        if (ControllerCensor.containsSwearing(comment)) {
            val account = ControllerAccounts.getAccount(userId)
            if (account != null) {
                ControllerEffects.makeSystem(
                    account,
                    API.EFFECT_INDEX_ADMIN_BAN,
                    System.currentTimeMillis() + 3600_000L * 24 * 3,
                    API.EFFECT_COMMENT_SWEARING,
                )
            }
            throw ApiException(API.ERROR_BAD_COMMENT)
        }

        if (comment.length < API.MODERATION_COMMENT_MIN_L || comment.length > API.MODERATION_COMMENT_MAX_L) {
            throw ApiException(API.ERROR_BAD_COMMENT)
        }
        return comment
    }
}
