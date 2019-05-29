drop table person;

create table person (
    id serial,
    firstName varchar(256), 
    lastName varchar(256), 
    address varchar(256),
    primary key(id)
);

insert into person(firstName, lastName, address)
    values('first 1', 'last 1', 'address 1');
insert into person(firstName, lastName, address)
    values('first 2', 'last 2', 'address 2');
insert into person(firstName, lastName, address)
    values('first 3', 'last 3', 'address 3');

