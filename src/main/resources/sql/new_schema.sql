\c mini_dish_db

create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');

create table if not exists dish
(
    id            serial primary key,
    name          varchar   not null,
    dish_type     dish_type not null default 'STARTER',
    selling_price numeric(10, 2)
);

create type ingredient_category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table if not exists ingredient
(
    id       serial primary key,
    name     varchar unique      not null,
    price    numeric(10, 2) default 0,
    category ingredient_category NOT NULL
);

create type unit_type as enum ('PCS', 'KG', 'L');

create table if not exists dish_ingredient
(
    id                serial primary key,
    id_dish           int references dish (id),
    id_ingredient     int references ingredient (id),
    quantity_required numeric(10, 2),
    unit              unit_type,
    CONSTRAINT unique_dish_ingredient UNIQUE (id_dish, id_ingredient)
);

create type movement_type as enum ('IN', 'OUT');

create table if not exists stock_movement
(
    id                serial primary key,
    id_ingredient     int references ingredient (id),
    quantity          numeric(10, 2),
    type              movement_type default 'IN' not null,
    unit              unit_type,
    creation_datetime timestamp     default current_timestamp
);

create table if not exists "order"
(
    id                serial primary key,
    reference         varchar unique,
    creation_datetime timestamp default current_timestamp
);

create table if not exists dish_order
(
    id       serial primary key,
    id_order int references "order" (id),
    id_dish  int references dish (id),
    quantity int default 1
)
