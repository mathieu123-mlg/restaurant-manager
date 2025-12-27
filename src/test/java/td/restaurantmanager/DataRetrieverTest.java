package td.restaurantmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class DataRetrieverTest {
    private DataRetriever dataRetriever;

    //given
    @BeforeEach
    public void setUp() {
        dataRetriever = new DataRetriever();
    }

    @Test
    @DisplayName("a. Find dish by id=1")
    void findDishById_1() {
        //When
        Dish find_dish_1 = dataRetriever.findDishById(1);

        //Then
        Assertions.assertEquals(1, find_dish_1.getId(), "ID should be 1");
        Assertions.assertEquals(
                "Salade fraîche", find_dish_1.getName(),
                "'Salade fraîche' is the name of dish having id 1"
        );

        //When
        List<Ingredient> ingredients_of_dish_1 = find_dish_1.getIngredients();

        //Then
        Assertions.assertEquals(
                2, ingredients_of_dish_1.size(),
                "Dish have 2 Ingredient")
        ;

        //When
        Ingredient laitue = ingredients_of_dish_1.get(0);
        Ingredient tomate = ingredients_of_dish_1.get(1);

        //Then
        Assertions.assertEquals("Salade fraîche", laitue.getDishName());
        Assertions.assertEquals("Salade fraîche", tomate.getDishName());
        Assertions.assertEquals(
                laitue.getDishName(),
                tomate.getDishName(),
                "'Laitue' and 'tomate' have the same dishName(Salade fraîche) for dish ID equal 1"
        );
        Assertions.assertEquals("Laitue", laitue.getName());
        Assertions.assertEquals("Tomate", tomate.getName());
    }

    @Test
    @DisplayName("b. Find dish by id=999")
    void findDishById_999() {
        //Then
        Assertions.assertThrows(
                RuntimeException.class, () -> {
                    Dish find_dish_999 = dataRetriever.findDishById(999);
                },
                "Should throw an exception"
        );
    }

    @Test
    @DisplayName("c. Find ingredients page=2, size=2")
    void findIngredientsPage_2Size_2() {
        //when
        List<Ingredient> ingredients_p2_s2 = dataRetriever.findIngredients(2, 2);
        Ingredient poulet = ingredients_p2_s2.get(0);
        Ingredient chocolat = ingredients_p2_s2.get(1);

        //then
        Assertions.assertEquals(2, ingredients_p2_s2.size());

        Assertions.assertEquals("Poulet", poulet.getName());
        Assertions.assertEquals("Chocolat", chocolat.getName());
    }

    @Test
    @DisplayName("d. Find ingredients page=3, size=5")
    void findIngredientsPage_3Size_5() {
        //when
        List<Ingredient> ingredients_p3_s5 = dataRetriever.findIngredients(3, 5);

        //then
        Assertions.assertEquals(0, ingredients_p3_s5.size(), "Should return empty list");
        Assertions.assertEquals(new ArrayList<>(), ingredients_p3_s5, "Should return empty list");
    }

    @Test
    @DisplayName("e. 'eur' --> Gâteau aux chocolat')")
    void findDishByIngredietsName_eur() {
        //when
        List<Dish> dish_list = dataRetriever.findDishByIngredientsName("eur");

        //then
        Assertions.assertEquals(1, dish_list.size(), "Should return 1");
        Assertions.assertEquals("Gâteau aux chocolat", dish_list.get(0).getName());
        Assertions.assertEquals("Gâteau aux chocolat", dish_list.get(0).getIngredients().getFirst().getDishName());
    }

    @Test
    @DisplayName("f. ")
    void findIngredientsByCriteria_a() {
        //when
        List<Ingredient> ingredientList = dataRetriever.findIngredientsByCriteria(
                null,
                CategoryEnum.VEGETABLE,
                null,
                1,
                10
        );

        List<String> result = ingredientList.stream()
                .map(Ingredient::getName)
                .toList();
        //then
        Assertions.assertEquals(List.of("Laitue", "Tomate") , result);
    }

    @Test
    @DisplayName("g. ")
    void findIngredientsByCriteria_b() {
        //when
        List<Ingredient> ingredientList = dataRetriever.findIngredientsByCriteria(
                "cho",
                null,
                "Sal",
                1,
                10
        );

        //then
        Assertions.assertEquals(new ArrayList<>(), ingredientList, "Should return empty list");
        Assertions.assertEquals(0, ingredientList.size(), "Should return 0");
    }

    @Test
    @DisplayName("h. ")
    void findIngredientsByCriteria_c() {
        //when
        List<Ingredient> ingredientList = dataRetriever.findIngredientsByCriteria(
                "cho",
                null,
                "gâteau",
                1,
                10
        );

        Ingredient chocolat = ingredientList.getFirst();
        //then
        Assertions.assertEquals("Chocolat", chocolat.getName());
        Assertions.assertEquals(1, ingredientList.size());
    }

    @Test
    @DisplayName("i. Create ingredients fromage and oignon")

    void createIngredientsFromageAndOignon() {
        //when
        List<Ingredient> fromage_and_oignon = List.of(
                new Ingredient(
                        6,
                        "Fromage",
                        1200.0,
                        CategoryEnum.DAIRY
                ),
                new Ingredient(
                        7,
                        "Oignon",
                        500.0,
                        CategoryEnum.VEGETABLE
                )
        );
        Ingredient fromage = fromage_and_oignon.get(0);
        Ingredient oignon = fromage_and_oignon.get(1);

        var ingredient_cree = dataRetriever.createIngredients(fromage_and_oignon);

        //then
        Assertions.assertEquals(fromage_and_oignon, ingredient_cree, "Sould be have same value and return Fromage and Oignon");

        Assertions.assertEquals(2, ingredient_cree.size(), "Sould be 2");
        Assertions.assertNull(fromage.getDish(), "Dish of Fromage is null");
        Assertions.assertNull(oignon.getDish(), "Dish of Oignon is null");
        Assertions.assertEquals(fromage.getDishName(), oignon.getDishName());
        Assertions.assertEquals(fromage.getDishName(), ingredient_cree.get(0).getDishName());
        Assertions.assertEquals(oignon.getDishName(), ingredient_cree.get(1).getDishName());
    }

    @Test
    @DisplayName("j. Create ingredients Carotte and laitue")
    void createIngredientsCarotteAndLaitue() {
        //when
        List<Ingredient> carotte_and_laitue = List.of(
                new Ingredient(
                        8,
                        "Carotte",
                        2000.0,
                        CategoryEnum.VEGETABLE
                ),
                new Ingredient(
                        9,
                        "Laitue",
                        2000.0,
                        CategoryEnum.VEGETABLE
                )
        );

        //then
        Assertions.assertThrows(
                RuntimeException.class, () -> {
                    dataRetriever.createIngredients(carotte_and_laitue);
                },
                "Should throw an exception"
        );
    }

    @Test
    @DisplayName("k. Soupe légume with ingredient oignon")
    void soupeLegumeWithIngredientOignon() {
        //when
        Ingredient oignon = new Ingredient(
                7, "Oignon",
                500.00,
                CategoryEnum.VEGETABLE
        );
        Dish soupe_legume = new Dish(
                8,
                "Soupe de légumes",
                DishTypeEnum.STARTER,
                List.of(oignon)
        );

        dataRetriever.saveDish(soupe_legume);

        //then
        Assertions.assertEquals(soupe_legume.getIngredients(), List.of(oignon), "Sould be true");
        Assertions.assertEquals(soupe_legume.getName(), oignon.getDishName(), "Sould have same name");
        Assertions.assertNotEquals(new ArrayList<>(), soupe_legume.getIngredients(), "Throw a erroer");
    }

    @Test
    @DisplayName("l. Salade fraîche ingredient updated with oignon and fromage")
    void saladeFraicheIngredientUpdatedWithOignonAndFromage() {
        //when
        Dish salade_fraiche = new Dish(
                1,
                "Salade fraiche",
                DishTypeEnum.STARTER,
                List.of(
                        new Ingredient(7, "Oignon", 500.0, CategoryEnum.VEGETABLE),
                        new Ingredient(1, "Laitue", 2000.0, CategoryEnum.VEGETABLE),
                        new Ingredient(2, "Tomate", 200.00, CategoryEnum.VEGETABLE),
                        new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY)
                )
        );
        dataRetriever.saveDish(salade_fraiche);

        //then
        Assertions.assertEquals(
                salade_fraiche.getIngredients(),
                List.of(
                        new Ingredient(7, "Oignon", 500.0, CategoryEnum.VEGETABLE),
                        new Ingredient(1, "Laitue", 2000.0, CategoryEnum.VEGETABLE),
                        new Ingredient(2, "Tomate", 200.00, CategoryEnum.VEGETABLE),
                        new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY)
                ));
        Assertions.assertNotEquals(2, salade_fraiche.getIngredients().size(), "Throw error");
        Assertions.assertEquals(4, salade_fraiche.getIngredients().size(), "Return 4 ingredients");
        Assertions.assertEquals(
                (500
                 + 2000
                 + 200
                 + 3000),
                salade_fraiche.getDishCost(),
                "Return 5700"
        );
    }

    @Test
    @DisplayName("m. Salade fraîche ingredient only fromage")
    void saladeFraicheIngredientOnlyFromage() {
        //when
        Dish salade_fraiche = new Dish(
                1,
                "Salade fraiche",
                DishTypeEnum.STARTER,
                List.of(
                        new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY)
                )
        );
        dataRetriever.saveDish(salade_fraiche);

        //then
        Assertions.assertEquals(1, salade_fraiche.getIngredients().size(), "Return 1");
        Assertions.assertEquals(
                "Fromage",
                salade_fraiche.getIngredients().get(0).getName(),
                "Return Fromage"
        );
        Assertions.assertNotEquals(
                5700,
                salade_fraiche.getIngredients().get(0).getPrice(),
                "Have different cost"
        );
    }
}
