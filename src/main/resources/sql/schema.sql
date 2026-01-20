\c mini_dish_db

create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');

create table dish
(
    id        serial primary key,
    name      varchar   not null,
    dish_type dish_type not null default 'STARTER'
);

create type category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table ingredient
(
    id       serial primary key,
    name     varchar unique not null,
    price    numeric(10, 2) default 0,
    category category       NOT NULL,
    id_dish  int references dish (id)
);


alter table dish
    add price numeric(10, 2) default null;

update dish
set price = 2000
where name = 'Salade fraîche';

update dish
set price = 6000
where name = 'Poulet grillé';

update dish
set price = null
where name = 'Riz au légume';

update dish
set price = null
where name = 'Gâteau au chocolat';

update dish
set price = null
where name = 'Salade de fruit';
