package com.dzen.campfire.server.executors.accounts

import com.dzen.campfire.api.API
import com.dzen.campfire.api.requests.accounts.RAccountsAdminRemove
import com.dzen.campfire.api.tools.ApiException
import com.dzen.campfire.server.controllers.*

class EAccountsAdminRemove : RAccountsAdminRemove(0, false) {

    @Throws(ApiException::class)
    override fun check() {
        ControllerFandom.checkCan(apiAccount, API.LVL_PROTOADMIN)
        if (accountId == 1L) throw ApiException(API.ERROR_ACCESS)
    }

    override fun execute(): Response {
        ControllerAccounts.remove(accountId, removePublications)

        return Response()
    }


}
