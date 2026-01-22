\c mini_dish_db

create type unit_type as enum ('PCS', 'KG', 'L');

create table dish_ingredient
(
    id                serial primary key,
    id_dish           int,
    id_ingredient     int,
    quantity_required numeric(10, 2),
    unit              unit_type
);


create type movement_type as enum ('IN', 'OUT');

create table stockmovement
(
    id                serial primary key,
    id_ingredient int references ingredient(id),
    quantity          numeric(10, 2),
    type              movement_type default 'IN' not null,
    unit              unit_type,
    creation_datetime timestamp     default current_timestamp
);