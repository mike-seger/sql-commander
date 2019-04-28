drop table message;
create table message (
    id_ serial,
    created_ timestamp,
    context_id_ varchar(64),
    type_ char(3) default 'TXT',
    text_ varchar(4096),
    status_ char(3) default 'SNT',
    primary key(id_)
);