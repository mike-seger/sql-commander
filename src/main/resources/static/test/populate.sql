drop table person;

create table person (
    id bigint,
    firstName varchar(256), 
    lastName varchar(256), 
    address varchar(256),
    primary key(id)
);

insert into person(id, firstName, lastName, address)
    values(1, 'first 1', 'last 1', 'address 1');
insert into person(id, firstName, lastName, address)
    values(2, 'first 2', 'last 2', 'address 2');
insert into person(id, firstName, lastName, address)
    values(3, 'first 3', 'last 3', 'address 3');

