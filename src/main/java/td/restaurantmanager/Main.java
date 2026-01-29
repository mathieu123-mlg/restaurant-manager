package td.restaurantmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever data = new DataRetriever();
//        Dish salade = data.findDishById(1);
        Dish poulet = data.findDishById(2);
        System.out.println(poulet);
//        List<DishOrder> dishOrders = List.of(
//                new DishOrder(null, salade, 2),  // 2 salades
//                new DishOrder(null, poulet, 1)   // 1 poulet
//        );
//
//        Order nouvelleCommande = new Order(
//                null,
//                null,
//                Instant.now(),
//                dishOrders
//        );
//
//        try {
//            Order commandeSauvegardee = data.saveOrder(nouvelleCommande);
//            System.out.println("Commande sauvegardée avec succès : " + commandeSauvegardee);
//            System.out.println("Total avec TVA : " + commandeSauvegardee.getTotalAmountWithVAt());
//            System.out.println("Total sans TVA : " + commandeSauvegardee.getTotalAmountWithoutVAt());
//        } catch (IllegalArgumentException e) {
//            System.out.println("Erreur de stock : " + e.getMessage());
//        }

//        try {
//            // ------------------------------------------------------
//            // 1. Créer quelques ingrédients de test (si pas déjà en base)
//            // ------------------------------------------------------
//            List<Ingredient> ingredientsToCreate = new ArrayList<>();
//
//            Ingredient tomate = new Ingredient(null, "Tomate", 2.5, CategoryEnum.VEGETABLE);
//            Ingredient fromage = new Ingredient(null, "Fromage", 4.0, CategoryEnum.DAIRY);
//            Ingredient pate = new Ingredient(null, "Pâte à pizza", 1.8, CategoryEnum.OTHER);
//
//            ingredientsToCreate.add(tomate);
//            ingredientsToCreate.add(fromage);
//            ingredientsToCreate.add(pate);
//
//            List<Ingredient> savedIngredients = data.createIngredients(ingredientsToCreate);
//            System.out.println("Ingrédients créés : " + savedIngredients.size());
//
//            // On récupère les vrais objets avec ID
//            tomate = savedIngredients.get(0);
//            fromage = savedIngredients.get(1);
//            pate = savedIngredients.get(2);
//
//            // ------------------------------------------------------
//            // 2. Créer un plat (Pizza Margherita par ex.)
//            // ------------------------------------------------------
//            List<DishIngredient> pizzaIngredients = new ArrayList<>();
//            pizzaIngredients.add(new DishIngredient(null, null, tomate, 0.2, UnitType.KG));
//            pizzaIngredients.add(new DishIngredient(null, null, fromage, 0.15, UnitType.KG));
//            pizzaIngredients.add(new DishIngredient(null, null, pate, 0.25, UnitType.KG));
//
//            Dish pizza = new Dish(
//                    null,
//                    "Pizza Margherita",
//                    DishTypeEnum.MAIN,
//                    12.5,
//                    pizzaIngredients
//            );
//
//            Dish savedPizza = data.saveDish(pizza);
//            System.out.println("Pizza sauvegardée → ID = " + savedPizza.getId());
//
//            // ------------------------------------------------------
//            // 3. Créer une commande avec 3 pizzas
//            // ------------------------------------------------------
//            List<DishOrder> dishOrders = new ArrayList<>();
//            dishOrders.add(new DishOrder(null, savedPizza, 3));  // 3 pizzas
//
//            Order newOrder = new Order(
//                    null,
//                    null,                        // référence générée auto
//                    Instant.now(),
//                    dishOrders
//            );
//
//            System.out.println("Tentative de sauvegarde de la commande...");
//
//            Order savedOrder = data.saveOrder(newOrder);
//
//            System.out.println("Commande sauvegardée avec succès !");
//            System.out.println("ID commande     : " + savedOrder.getId());
//            System.out.println("Référence       : " + savedOrder.getReference());
//            System.out.println("Date création   : " + savedOrder.getCreationDatetime());
//            System.out.println("Nombre de plats : " + savedOrder.getDishOrders().size());
//
//            // ------------------------------------------------------
//            // 4. Vérifier le stock restant (optionnel)
//            // ------------------------------------------------------
//            for (DishIngredient di : savedPizza.getDishIngredients()) {
//                Ingredient ing = di.getIngredient();
//                double stockAfter = ing.getStockValueAt(Instant.now()).getQuantity();
//                System.out.printf("Stock restant %s : %.2f %s%n",
//                        ing.getName(), stockAfter, di.getUnit());
//            }
//
//        } catch (Exception e) {
//            System.err.println("Erreur pendant les tests :");
//            throw new RuntimeException(e);
//        }
//        System.out.println(data.findDishById(1));//[2]
//        System.out.println(data.findDishById(4)); //[2]
//        System.out.println(data.findDishById(5)); // 0 ingredient

//        System.out.println(data.findIngredients(1, 10)); //[5 element]
//        System.out.println();
//        System.out.println(data.findIngredients(4, 1)); //id: [4]
//        System.out.println();
//        System.out.println(data.findIngredients(3, 2)); //id: [5]
//        System.out.println();
//        System.out.println(data.findIngredients(2, 2)); //id: [3, 4]

//        List<Ingredient> list_ingredient = List.of(
//                new Ingredient(8, "Citrouille", 2_000.0, CategoryEnum.VEGETABLE),
//                new Ingredient(6, "Oeuf", 800.0, CategoryEnum.ANIMAL),
//                new Ingredient(7, "Sel", 200.0, CategoryEnum.DAIRY)
//        );
//        System.out.println(data.createIngredients(list_ingredient));

//        Ingredient oignon = new Ingredient(
//                10, "Oignon",
//                500.00,
//                CategoryEnum.VEGETABLE
//        );
//        Dish soupe_legume = new Dish(
//                8,
//                "Soupe de légumes",
//                DishTypeEnum.STARTER,
//                2000.0,
//                List.of(
//                        new DishIngredient(
//                                1,
//                                new Dish(8, "Soupe de légumes", DishTypeEnum.STARTER, 200.0, new ArrayList<>()),
//                                oignon,
//                                0.1,
//                                UnitType.KG
//                        )
//                )
//        );
//        System.out.println(data.saveDish(soupe_legume));

//        Dish salade_fraiche = data.findDishById(1);
//        System.out.println(salade_fraiche);
//        try {
//            System.out.println(salade_fraiche.getDishCost());
//            System.out.println(salade_fraiche.getGrossMargin());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        System.out.println(data.findIngredientsByCriteria(null, null, "salade", 1, 10));
//        System.out.println(data.findDishByIngredientsName("chocolat"));
//        System.out.println(data.findDishByIngredientsName("ou"));
        //Trow Erreur
//        Dish riz_au_legume = data.findDishById(3);
//        try {
//            System.out.println(riz_au_legume.getGrossMargin());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println(
//                data.saveDish(
//                        new Dish(
//                                8,
//                                "Test",
//                                DishTypeEnum.STARTER,
//                                2000.0,
//                                new ArrayList<>()
//                        )
//                ));

//        Ingredient ingredient = data.saveIngredient(
//                new Ingredient(1, "Laitue", 800.00, CategoryEnum.VEGETABLE, List.of(
//                        new StockMovement(
//                                1,
//                                new StockValue(
//                                        2000.0,
//                                        UnitType.KG
//                                ),
//                                MovementTypeEnum.IN,
//                                Instant.parse("2024-01-05T08:00:00.000000Z")
//                        )))
//        );
//        System.out.println(ingredient);

//        Instant t = Instant.parse("2024-01-06T12:00:00.000000Z");
//        System.out.println(data.findIngredientsByCriteria("beurre", null, null,
//                1,10).getFirst().getStockValueAt(t));
//        StockMovement stockMovement = new StockMovement(
//                20,
//                new StockValue(1000.0, UnitType.KG),
//                MovementTypeEnum.IN
//        );
//        Ingredient sel = new Ingredient(15, "Sel", 2000.0, CategoryEnum.DAIRY, List.of(stockMovement));
//        System.out.println(sel);
//        System.out.println(data.saveIngredient(sel));
    }
}
