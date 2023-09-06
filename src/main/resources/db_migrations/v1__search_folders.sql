create table search_folders(
    id bigint auto_increment primary key ,
    parent_id bigint references search_folders(id) on delete cascade,
    name varchar not null
);

create index ix_folders_parent on search_folders(parent_id);

create unique index ix_folder_pos on search_folders(parent_id, name);

create index ix_folders_name on search_folders(name);

create table file_docs(
    id bigint auto_increment primary key,
    search_folder_id bigint not null references search_folders(id) on delete cascade,
    file_name varchar not null,
    detected_size bigint not null,
    doc_type tinyint not null,
    processed_size bigint,
    processed_mtime timestamp,
    is_bad BOOLEAN not null default false
);

create unique index ix_file_folder on file_docs(search_folder_id,file_name);

create index ix_file_processed on file_docs(processed_mtime,is_bad);