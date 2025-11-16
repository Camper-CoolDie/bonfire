insert into collisions (collision_type, owner_id, collision_id, value_1, value_3)
select
    20004, -- COLLISION_FANDOM_TITLE_IMAGE
    fp.fandom_id,
    fp.language_id,
    fp.background_id,
    fp.background_gif_id
from fandom_profiles fp
join fandoms on fandoms.id = fp.fandom_id
where fp.background_id != fandoms.image_title_id;

insert into collisions (collision_type, owner_id, collision_id, value_1)
select
    20012, -- COLLISION_CHAT_BACKGROUND_IMAGE
    fp.fandom_id,
    fp.language_id,
    fp.root_chat_background_id
from fandom_profiles fp
where fp.root_chat_background_id != 0;

insert into collisions (collision_type, owner_id, collision_id, value_2)
select
    20000, -- COLLISION_FANDOM_DESCRIPTION
    fp.fandom_id,
    fp.language_id,
    fp.description
from fandom_profiles fp
where fp.description != '';

insert into collisions (collision_type, owner_id, collision_id, value_1)
select
    20001, -- COLLISION_FANDOM_GALLERY
    fp.fandom_id,
    fp.language_id,
    g
from fandom_profiles fp, unnest(fp.gallery) as g
where g is not null;

insert into collisions (collision_type, owner_id, collision_id, value_1, value_2)
select
    20002, -- COLLISION_FANDOM_LINK
    fp.fandom_id,
    fp.language_id,
    (l).icon,
    (l).title || 'zle!/---vxка' || (l).url
from fandom_profiles fp, unnest(fp.links) as l
where fp.links != '{}';

insert into collisions (collision_type, owner_id, collision_id, value_2)
select
    20005, -- COLLISION_FANDOM_NAMES
    fp.fandom_id,
    fp.language_id,
    array_to_string(fp.additional_names, '~~~')
from fandom_profiles fp
where fp.additional_names != '{}';

insert into collisions (collision_type, collision_date_create, owner_id, collision_id, value_1)
select
    20014, -- COLLISION_FANDOM_VICEROY
    extract(epoch from fp.viceroy_assigned_at) * 1000,
    fp.fandom_id,
    fp.language_id,
    fp.viceroy_id
from fandom_profiles fp
where fp.viceroy_id != 0;

insert into collisions (collision_type, owner_id, collision_id, value_1)
select
    20013, -- COLLISION_FANDOM_PINNED_POST
    fp.fandom_id,
    fp.language_id,
    fp.pinned_post_id
from fandom_profiles fp
where fp.pinned_post_id != 0;

insert into collisions (collision_type, owner_id, collision_id)
select
    20016, -- COLLISION_FANDOM_PARAMS_1
    f.id,
    p
from fandoms f, unnest(f.params_1) as p
where f.params_1 != '{}'

insert into collisions (collision_type, owner_id, collision_id)
select
    20017, -- COLLISION_FANDOM_PARAMS_2
    f.id,
    p
from fandoms f, unnest(f.params_2) as p
where f.params_2 != '{}'

insert into collisions (collision_type, owner_id, collision_id)
select
    20018, -- COLLISION_FANDOM_PARAMS_3
    f.id,
    p
from fandoms f, unnest(f.params_3) as p
where f.params_3 != '{}'

insert into collisions (collision_type, owner_id, collision_id)
select
    20015, -- COLLISION_FANDOM_PARAMS_4
    f.id,
    p
from fandoms f, unnest(f.params_4) as p
where f.params_4 != '{}'

drop function
    update_subscribers_count(bigint, bigint),
    update_sub_chats_count(bigint, bigint),
    update_tags_count(bigint, bigint),
    update_categories_count(bigint, bigint),
    update_rubrics_count(bigint, bigint),
    update_relay_races_count(bigint, bigint),
    update_wiki_articles_count(bigint);
drop table fandom_profiles cascade;
drop type fandom_link;
alter table fandoms
    drop column params_1,
    drop column params_2,
    drop column params_3,
    drop column params_4,
    drop column wiki_articles_count;
