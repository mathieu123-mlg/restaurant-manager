\c mini_dish_db

create table dish (
    id serial primary key,
    name varchar not null,
    dish_type varchar not null check
        (dish_type in ('STARTER', 'MAIN', 'DESSERT')) default 'STARTER'
);

create table ingredient (
    id serial primary key,
    name varchar unique not null,
    price numeric(10,2) default 0,
    category varchar NOT NULL
        CHECK (category IN
               ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER')),
    id_dish int references dish(id)
);
