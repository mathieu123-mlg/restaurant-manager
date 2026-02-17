\c mini_dish_db

delete from ingredient;
delete from dish;
delete from "order";
delete from "table";
delete from stock_movement;
delete from dish_ingredient;
delete from dish_order;

insert into dish (id, name, dish_type, price)
VALUES (1, 'Salade fraîche', 'STARTER', 3500.00),
       (2, 'Poulet grillé', 'MAIN', 12000.00),
       (3, 'Riz au légume', 'MAIN', null),
       (4, 'Gâteau aux chocolat', 'DESSERT', 8000.00),
       (5, 'Salade de fruits', 'DESSERT', null)
ON CONFLICT (id) DO UPDATE
    set name      = EXCLUDED.name,
        dish_type = EXCLUDED.dish_type,
        price     = EXCLUDED.price;

insert into ingredient (id, name, price, category)
VALUES (1, 'Laitue', 800.00, 'VEGETABLE'),
       (2, 'Tomate', 600.00, 'VEGETABLE'),
       (3, 'Poulet', 4500.00, 'ANIMAL'),
       (4, 'Chocolat', 3000.00, 'OTHER'),
       (5, 'Beurre', 2500.00, 'DAIRY')
ON conflict (id) DO UPDATE
    set name     = EXCLUDED.name,
        price    = excluded.price,
        category = excluded.category;

insert into dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
VALUES (1, 1, 1, 0.20, 'KG'),
       (2, 1, 2, 0.15, 'KG'),
       (3, 2, 3, 1.00, 'KG'),
       (4, 4, 4, 0.30, 'KG'),
       (5, 4, 5, 0.20, 'KG')
on conflict (id_dish, id_ingredient) do update
    set quantity_required = excluded.quantity_required,
        unit              = excluded.unit;

INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
VALUES (1, 1, 5.00, 'IN', 'KG', '2024-01-05 08:00:00'),
       (2, 1, 0.20, 'OUT', 'KG', '2024-01-06 12:00:00'),
       (3, 2, 4.00, 'IN', 'KG', '2024-01-05 08:00:00'),
       (4, 2, 0.15, 'OUT', 'KG', '2024-01-06 12:00:00'),
       (5, 3, 10.00, 'IN', 'KG', '2024-01-04 09:00:00'),
       (6, 3, 1.00, 'OUT', 'KG', '2024-01-06 13:00:00'),
       (7, 4, 3.00, 'IN', 'KG', '2024-01-05 10:00:00'),
       (8, 4, 0.30, 'OUT', 'KG', '2024-01-06 14:00:00'),
       (9, 5, 2.50, 'IN', 'KG', '2024-01-05 10:00:00'),
       (10, 5, 0.20, 'OUT', 'KG', '2024-01-06 14:00:00'),
       (11, 1, 3.2, 'OUT', 'KG', '2026-02-17 00:00:00'),
       (12, 1, 9.5, 'IN', 'KG', '2026-02-17 00:00:00'),
       (13, 1, 4.5, 'OUT', 'KG', '2026-02-17 00:00:00'),
       (14, 1, 7.0, 'IN', 'KG', '2026-02-17 00:00:00'),
       (15, 1, 7.5, 'OUT', 'KG', '2026-02-17 00:00:00'),
       (16, 2, 4.15, 'IN', 'KG', '2026-02-17 00:00:00'),
       (17, 2, 0.5, 'IN', 'KG', '2026-02-17 00:00:00')
on conflict (id) do update
    set id_ingredient     = excluded.id_ingredient,
        quantity          = excluded.quantity,
        type              = excluded.type,
        unit              = excluded.unit,
        creation_datetime = excluded.creation_datetime;

insert into "order" (id, reference, creation_datetime)
values (1, 'ORD00001', '2026-02-09'),
       (2, 'ORD00002', current_timestamp),
       (3, 'ORD00003', current_timestamp)
on conflict (id) do update
    set reference         = excluded.reference,
        creation_datetime = excluded.creation_datetime;

insert into "table" (id, number)
values (1, 1),
       (2, 2),
       (3, 3)
on conflict (id) do update set number = excluded.number;

insert into table_order (id_table, id_order, arrival_datetime, departure_datetime)
values (1, 1, current_timestamp, current_timestamp),
       (2, 1, current_timestamp, current_timestamp),
       (1, 2, current_timestamp, current_timestamp)
on conflict (id_table, id_order) do nothing;
