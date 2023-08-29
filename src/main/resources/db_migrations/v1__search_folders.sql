create table search_folders(
    id bigint auto_increment primary key ,
    parent_id bigint references search_folders(id) on delete cascade,
    name varchar not null,
    checked_mtime timestamp
);

create index ix_folders_parent on search_folders(parent_id);

create index ix_folders_name on search_folders(name);
