\c mini_dish_db

insert into dish (id, name, dish_type, selling_price)
VALUES (1, 'Salade fraîche', 'STARTER', 3500.00),
       (2, 'Poulet grillé', 'MAIN', 12000.00),
       (3, 'Riz au légume', 'MAIN', null),
       (4, 'Gâteau aux chocolat', 'DESSERT', 8000.00),
       (5, 'Salade de fruits', 'DESSERT', null)
ON CONFLICT (id) DO UPDATE
    set name          = EXCLUDED.name,
        dish_type     = EXCLUDED.dish_type,
        selling_price = EXCLUDED.selling_price;

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
on conflict (id) do update
    set id_dish           = excluded.id_dish,
        id_ingredient     = excluded.id_ingredient,
        quantity_required = excluded.quantity_required,
        unit              = excluded.unit;

INSERT INTO stockmovement (id, id_ingredient, quantity, type, unit, creation_datetime)
VALUES (1, 1, 5.00, 'IN', 'KG', '2024-01-05 08:00:00'),
       (2, 1, 0.20, 'OUT', 'KG', '2024-01-06 12:00:00'),
       (3, 2, 4.00, 'IN', 'KG', '2024-01-06 12:00:00'),
       (4, 2, 0.15, 'OUT', 'KG', '2024-01-06 12:00:00'),
       (5, 3, 10.00, 'IN', 'KG', '2024-01-04 09:00:00'),
       (6, 3, 1.00, 'OUT', 'KG', '2024-01-06 13:30:00'),
       (7, 4, 3.00, 'IN', 'KG', '2024-01-05 10:00:00'),
       (8, 4, 0.30, 'OUT', 'KG', '2024-01-06 14:00:00'),
       (9, 5, 2.50, 'IN', 'KG', '2024-01-05 10:00:00'),
       (10, 5, 0.20, 'OUT', 'KG', '2024-01-06 14:00:00')
on conflict (id) do update
    set id_ingredient     = excluded.id_ingredient,
        quantity          = excluded.quantity,
        type              = excluded.type,
        unit              = excluded.unit,
        creation_datetime = excluded.creation_datetime;