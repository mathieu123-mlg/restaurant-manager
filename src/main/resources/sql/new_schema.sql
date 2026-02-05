\c mini_dish_db

create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');
create type ingredient_category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');
create type unit_type as enum ('PCS', 'KG', 'L');
create type movement_type as enum ('IN', 'OUT');

create table if not exists dish
(
    id        serial primary key,
    name      varchar(120) unique not null,
    dish_type dish_type           not null default 'STARTER',
    price     numeric(10, 2) check (price >= 0)
);

create table if not exists ingredient
(
    id       serial primary key,
    name     varchar(120) unique not null,
    price    numeric(10, 2) default 0 check (price >= 0),
    category ingredient_category not null
);

create table if not exists dish_ingredient
(
    id                serial primary key,
    id_dish           int references dish (id) on delete cascade,
    id_ingredient     int references ingredient (id) on delete restrict,
    quantity_required numeric(10, 2) not null check (quantity_required > 0),
    unit              unit_type      not null default 'KG',
    CONSTRAINT unique_dish_ingredient unique (id_dish, id_ingredient)
);

create table if not exists stock_movement
(
    id                serial primary key,
    id_ingredient     int references ingredient (id) on delete restrict,
    quantity          numeric(10, 2),
    type              movement_type default 'IN' not null,
    unit              unit_type,
    creation_datetime timestamptz   default current_timestamp
);

create table if not exists "order"
(
    id                serial primary key,
    reference         varchar(40) unique not null,
    creation_datetime timestamptz        not null default current_timestamp
);

create table if not exists dish_order
(
    id       serial primary key,
    id_order int references "order" (id) on delete cascade,
    id_dish  int references dish (id) on delete restrict,
    quantity int not null check (quantity >= 1),
    CONSTRAINT unique_dish_order unique (id_order, id_dish)
);

create table if not exists "table"
(
    id     serial primary key,
    number int not null unique check (number > 0)
);

create table if not exists table_order
(
    id_table           int         not null references "table" (id) on delete restrict,
    id_order           int         not null references "order" (id) on delete cascade,
    arrival_datetime   timestamptz not null,
    departure_datetime timestamptz
        check ( departure_datetime is null or departure_datetime > arrival_datetime),
    primary key (id_order, id_table)
);