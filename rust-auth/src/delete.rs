use crate::AuthServer;
use c_core::services::auth::AuthError;

impl AuthServer {
    pub(crate) async fn _delete_user(&self, user_id: i64) -> Result<(), AuthError> {
        sqlx::query!("delete from oauth2_grants where user_id = $1", user_id)
            .execute(&self.base.pool)
            .await?;

        let new_name = format!("deleted#{user_id}");

        self._terminate_all_sessions_internal(user_id).await?;
        self._change_name(user_id, new_name, true).await?;

        sqlx::query!(
            "update users set email = null, password = null, tfa_mode = null, tfa_data = NULL \
             where id = $1",
            user_id,
        )
        .execute(&self.base.pool)
        .await?;

        Ok(())
    }
}
