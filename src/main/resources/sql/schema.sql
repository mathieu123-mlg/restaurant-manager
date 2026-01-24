\c mini_dish_db

create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');

create table dish
(
    id        serial primary key,
    name      varchar   not null,
    dish_type dish_type not null default 'STARTER'
);

create type ingredient_category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table ingredient
(
    id       serial primary key,
    name     varchar unique      not null,
    price    numeric(10, 2) default 0,
    category ingredient_category NOT NULL
);

alter table dish
    add column price numeric(10, 2);

update dish set price = 2000 where dish.name like 'Salade fraîche';
update dish set price = 6000 where dish.name like 'Poulet grillé';

alter table dish
    rename price to selling_price;
