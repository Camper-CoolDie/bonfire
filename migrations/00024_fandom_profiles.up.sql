create type fandom_link as (
    title text,
    url text,
    icon bigint
);

create table fandom_profiles (
    fandom_id bigint not null references fandoms on delete cascade,
    language_id bigint not null,

    background_id bigint not null,
    background_gif_id bigint not null default 0,
    root_chat_background_id bigint not null default 0,
    description text not null default '',
    gallery bigint[] not null default '{}',
    links fandom_link[] not null default '{}',
    additional_names text[] not null default '{}',
    viceroy_id bigint not null default 0,
    viceroy_assigned_at timestamptz not null default now(),
    pinned_post_id bigint not null default 0,

    subscribers_count bigint not null default 0,
    sub_chats_count bigint not null default 0,
    tags_count bigint not null default 0,
    categories_count bigint not null default 0,
    rubrics_count bigint not null default 0,
    relay_races_count bigint not null default 0
);

create function update_subscribers_count(p_fandom_id bigint, p_language_id bigint) returns table (
    count_total bigint,
    count_language bigint
) as $$
declare cnt_1 bigint, cnt_2 bigint;
begin
    select count(distinct owner_id) into cnt_1 from collisions
    where collision_type = 20 -- COLLISION_FANDOM_SUBSCRIBE
        and collision_id = p_fandom_id
        and value_1 != 1; -- PUBLICATION_IMPORTANT_NONE

    select count(*) into cnt_2 from collisions
    where collision_type = 20 -- COLLISION_FANDOM_SUBSCRIBE
        and collision_id = p_fandom_id
        and collision_sub_id = p_language_id;

    update fandoms
    set subscribers_count = cnt_1
    where id = p_fandom_id;

    update fandom_profiles
    set subscribers_count = cnt_2
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return query select cnt_1, cnt_2;
end;
$$ language plpgsql security definer;

create function update_sub_chats_count(p_fandom_id bigint, p_language_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from chats
    where type = 4 -- CHAT_TYPE_FANDOM_SUB
        and fandom_id = p_fandom_id
        and language_id = p_language_id;

    update fandom_profiles
    set sub_chats_count = cnt
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return cnt;
end;
$$ language plpgsql security definer;

create function update_tags_count(p_fandom_id bigint, p_language_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from units
    where unit_type = 10 -- PUBLICATION_TYPE_TAG
        and status = 2 -- STATUS_PUBLIC
        and fandom_id = p_fandom_id
        and language_id = p_language_id
        and parent_unit_id != 0;

    update fandom_profiles
    set tags_count = cnt
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return cnt;
end;
$$ language plpgsql security definer;

create function update_categories_count(p_fandom_id bigint, p_language_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from units
    where unit_type = 10 -- PUBLICATION_TYPE_TAG
        and status = 2 -- STATUS_PUBLIC
        and fandom_id = p_fandom_id
        and language_id = p_language_id
        and parent_unit_id = 0;

    update fandom_profiles
    set categories_count = cnt
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return cnt;
end;
$$ language plpgsql security definer;

create function update_rubrics_count(p_fandom_id bigint, p_language_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from rubrics
    where status = 2 -- STATUS_PUBLIC
        and fandom_id = p_fandom_id
        and language_id = p_language_id;

    update fandom_profiles
    set rubrics_count = cnt
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return cnt;
end;
$$ language plpgsql security definer;

create function update_relay_races_count(p_fandom_id bigint, p_language_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from activities
    where type = 1 -- ACTIVITIES_TYPE_RELAY_RACE
        and fandom_id = p_fandom_id
        and language_id = p_language_id;

    update fandom_profiles
    set relay_races_count = cnt
    where fandom_id = p_fandom_id and language_id = p_language_id;

    return cnt;
end;
$$ language plpgsql security definer;

--
-- this works trust me
--

with background_id as (
    delete from collisions
    where collision_type = 20004 -- COLLISION_FANDOM_TITLE_IMAGE
    returning owner_id, collision_id, value_1, value_3
),
root_chat_background_id as (
    delete from collisions
    where collision_type = 20012 -- COLLISION_CHAT_BACKGROUND_IMAGE
    returning owner_id, collision_id, value_1
),
description as (
    delete from collisions
    where collision_type = 20000 -- COLLISION_FANDOM_DESCRIPTION
    returning owner_id, collision_id, value_2
),
gallery as (
    delete from collisions
    where collision_type = 20001 -- COLLISION_FANDOM_GALLERY
    returning owner_id, collision_id, value_1
),
links as (
    delete from collisions
    where collision_type = 20002 -- COLLISION_FANDOM_LINK
    returning owner_id, collision_id, row(
        split_part(value_2, 'zle!/---vxка', 1),
        split_part(value_2, 'zle!/---vxка', 2),
        value_1
    )::fandom_link as value
),
additional_names as (
    delete from collisions
    where collision_type = 20005 -- COLLISION_FANDOM_NAMES
    returning owner_id, collision_id, string_to_array(value_2, '~~~') as value
),
viceroy as (
    delete from collisions
    where collision_type = 20014 -- COLLISION_FANDOM_VICEROY
    returning owner_id, collision_id, value_1, to_timestamp(collision_date_create / 1000) as assigned_at
),
pinned_post_id as (
    delete from collisions
    where collision_type = 20013 -- COLLISION_FANDOM_PINNED_POST
    returning owner_id, collision_id, value_1
)
insert into fandom_profiles (
    fandom_id,
    language_id,
    background_id,
    background_gif_id,
    root_chat_background_id,
    description,
    gallery,
    links,
    additional_names,
    viceroy_id,
    viceroy_assigned_at,
    pinned_post_id,
    subscribers_count,
    sub_chats_count,
    tags_count,
    categories_count,
    rubrics_count,
    relay_races_count
) select
    fandoms.id,
    gen.language_id,
    coalesce(min(background_id.value_1), fandoms.image_title_id),
    coalesce(min(background_id.value_3), 0),
    coalesce(min(root_chat_background_id.value_1), 0),
    coalesce(min(description.value_2), ''),
    coalesce(array_agg(distinct gallery.value_1), '{}'),
    coalesce(array_agg(distinct links.value), '{}'),
    coalesce(min(additional_names.value), '{}'),
    coalesce(min(viceroy.value_1), 0),
    coalesce(min(viceroy.assigned_at), now()),
    coalesce(min(pinned_post_id.value_1), 0),
    (
        select count(*) from collisions
        where collision_type = 20 -- COLLISION_FANDOM_SUBSCRIBE
            and collision_id = fandoms.id
            and collision_sub_id = gen.language_id
    ),
    (
        select count(*) from chats
        where type = 4 -- CHAT_TYPE_FANDOM_SUB
            and fandom_id = fandoms.id
            and language_id = gen.language_id
    ),
    (
        select count(*) from units
        where unit_type = 10 -- PUBLICATION_TYPE_TAG
            and status = 2 -- STATUS_PUBLIC
            and fandom_id = fandoms.id
            and language_id = gen.language_id
            and parent_unit_id != 0
    ),
    (
        select count(*) from units
        where unit_type = 10 -- PUBLICATION_TYPE_TAG
            and status = 2 -- STATUS_PUBLIC
            and fandom_id = fandoms.id
            and language_id = gen.language_id
            and parent_unit_id = 0
    ),
    (
        select count(*) from rubrics
        where status = 2 -- STATUS_PUBLIC
            and fandom_id = fandoms.id
            and language_id = gen.language_id
    ),
    (
        select count(*) from activities
        where type = 1 -- ACTIVITIES_TYPE_RELAY_RACE
            and fandom_id = fandoms.id
            and language_id = gen.language_id
    )
from fandoms
cross join generate_series(1, 8) as gen(language_id)
left join background_id on background_id.owner_id = fandoms.id
    and background_id.collision_id = gen.language_id
left join root_chat_background_id on root_chat_background_id.owner_id = fandoms.id
    and root_chat_background_id.collision_id = gen.language_id
left join description on description.owner_id = fandoms.id
    and description.collision_id = gen.language_id
left join gallery on gallery.owner_id = fandoms.id
    and gallery.collision_id = gen.language_id
left join links on links.owner_id = fandoms.id
    and links.collision_id = gen.language_id
left join additional_names on additional_names.owner_id = fandoms.id
    and additional_names.collision_id = gen.language_id
left join viceroy on viceroy.owner_id = fandoms.id
    and viceroy.collision_id = gen.language_id
left join pinned_post_id on pinned_post_id.owner_id = fandoms.id
    and pinned_post_id.collision_id = gen.language_id
where status = 2 -- STATUS_PUBLIC
group by fandoms.id, gen.language_id;

--
-- table fandoms
--

alter table fandoms
    add column params_1 bigint[] not null default '{}',
    add column params_2 bigint[] not null default '{}',
    add column params_3 bigint[] not null default '{}',
    add column params_4 bigint[] not null default '{}',
    add column wiki_articles_count bigint not null default 0;

update fandoms
set wiki_articles_count = (
    select count(*) from wiki_titles
    where fandom_id = fandoms.id
        and wiki_status = 2 -- STATUS_PUBLIC
        and type = 2 -- WIKI_TYPE_ARTICLE
);

update fandoms
set params_1 = (
    delete from collisions
    where collision_type = 20016 -- COLLISION_FANDOM_PARAMS_1
        and owner_id = fandoms.id
    returning array_agg(distinct collision_id)
);

update fandoms
set params_2 = (
    delete from collisions
    where collision_type = 20017 -- COLLISION_FANDOM_PARAMS_2
        and owner_id = fandoms.id
    returning array_agg(distinct collision_id)
);

update fandoms
set params_3 = (
    delete from collisions
    where collision_type = 20018 -- COLLISION_FANDOM_PARAMS_3
        and owner_id = fandoms.id
    returning array_agg(distinct collision_id)
);

update fandoms
set params_4 = (
    delete from collisions
    where collision_type = 20015 -- COLLISION_FANDOM_PARAMS_4
        and owner_id = fandoms.id
    returning array_agg(distinct collision_id)
);

create function update_wiki_articles_count(p_fandom_id bigint) returns bigint as $$
declare cnt bigint;
begin
    select count(*) into cnt from wiki_titles
    where fandom_id = p_fandom_id
        and wiki_status = 2 -- STATUS_PUBLIC
        and type = 2; -- WIKI_TYPE_ARTICLE

    update fandoms
    set wiki_articles_count = cnt
    where id = p_fandom_id;

    return cnt;
end;
$$ language plpgsql security definer;
