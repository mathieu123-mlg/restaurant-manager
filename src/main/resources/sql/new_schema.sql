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
