\c mini_dish_db

insert into dish (id, name, dish_type, price)
VALUES (1, 'Salade fraîche', 'STARTER', 3500.00),
       (2, 'Poulet grillé', 'MAIN', 12000.00),
       (3, 'Riz au légume', 'MAIN', null),
       (4, 'Gâteau aux chocolat', 'DESSERT', 8000.00),
       (5, 'Salade de fruits', 'DESSERT', null)
ON CONFLICT (id) DO UPDATE
set price = EXCLUDED.price;

insert into ingredient (id, name, price, category, id_dish)
VALUES (1, 'Laitue', 800.00, 'VEGETABLE'),
       (2, 'Tomate', 600.00, 'VEGETABLE'),
       (3, 'Poulet', 4500.00, 'ANIMAL'),
       (4, 'Chocolat', 3000.00, 'OTHER'),
       (5, 'Beurre', 2500.00, 'DAIRY');

insert into dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
VALUES (1, 1, 1, 0.20, 'KG'),
       (2, 1, 2, 0.15, 'KG'),
       (3, 2, 3, 1.00, 'KG'),
       (4, 4, 4, 0.30, 'KG'),
       (5, 4, 5, 0.20, 'KG');
