alter table app_user
    add column password_reset_token varchar(120),
    add column password_reset_expires_at timestamptz;

create unique index idx_app_user_password_reset_token
    on app_user(password_reset_token)
    where password_reset_token is not null;
