package td.restaurantmanager;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

public class DataRetrieverTest {
    private DataRetriever dataRetriever;

    //given
    @BeforeEach
    public void setUp() {
        dataRetriever = new DataRetriever();
        dataRetriever.initialiseData(dataRetriever);
    }

    @AfterEach
    public void tearDown() {
        dataRetriever.initialiseData(dataRetriever);
    }

    @Test
    @DisplayName("a. Find dish by id=1")
    void findDishById_1() {
        //When
        Dish dish_1 = dataRetriever.findDishById(1);

        //Then
        Assertions.assertEquals(1, dish_1.getId(), "ID should be 1");
        Assertions.assertEquals(
                "Salade fraîche", dish_1.getName(),
                "'Salade fraîche' is the name of dish having id 1"
        );

        //When
        List<DishIngredient> dish_ingredient_of_dish_1 = dish_1.getDishIngredients();

        //Then
        Assertions.assertEquals(
                2, dish_ingredient_of_dish_1.size(),
                "Dish have 2 Ingredient")
        ;

        //When
        DishIngredient laitue = dish_ingredient_of_dish_1.get(0);
        DishIngredient tomate = dish_ingredient_of_dish_1.get(1);

        //Then
        Assertions.assertEquals("Salade fraîche", laitue.getDish().getName());
        Assertions.assertEquals("Salade fraîche", tomate.getDish().getName());
        Assertions.assertEquals(
                laitue.getDish().getName(),
                tomate.getDish().getName(),
                "'Laitue' and 'tomate' have the same dishName(Salade fraîche) for dish ID equal 1"
        );
        Assertions.assertEquals("Salade fraîche", laitue.getDish().getName());
        Assertions.assertEquals("Salade fraîche", tomate.getDish().getName());
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
    void findDishByIngredientsName_eur() {
        //when
        List<Dish> dish_list = dataRetriever.findDishByIngredientsName("eur");

        //then
        Assertions.assertEquals(1, dish_list.size(), "Should return 1");
        Assertions.assertEquals("Gâteau aux chocolat", dish_list.getFirst().getName());
        Assertions.assertEquals("Gâteau aux chocolat", dish_list.getFirst().getDishIngredients().getFirst().getDish().getName());
    }

    @Test
    @DisplayName("f. Find ingredient having category VEGETABLE at page 1 size 10 --> ['Laitue', 'Tomate'")
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
    @DisplayName("g. Find ingredient having name = 'cho', dish_name = 'Sal' at page 1 size 10 --> []")
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
    @DisplayName("h. Find ingredient having name = 'cho', dish_name = 'gâteau' at page 1 size 10 --> ['Chocolat']")
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
        var create_ingredients = dataRetriever.createIngredients(fromage_and_oignon);

        //then
        Assertions.assertEquals(fromage_and_oignon, create_ingredients, "Sould be have same value and return Fromage and Oignon");

        Assertions.assertEquals(2, create_ingredients.size(), "Sould be 2");
        Assertions.assertNull(dataRetriever.findDishByIngredientsName("fromage"), "Dish of Fromage is null");
        Assertions.assertNull(dataRetriever.findDishByIngredientsName("oignon"), "Dish of Oignon is null");
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

        //when
        List<Ingredient> ingredients = dataRetriever.findIngredients(1, 10);

        //then
        Assertions.assertFalse(ingredients.contains(carotte_and_laitue.get(0)), "Should return false");
        Assertions.assertFalse(ingredients.contains(carotte_and_laitue.get(1)), "Should return false");
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

        List<DishIngredient> dishIngredients = List.of(
                new DishIngredient(oignon, 0.2, UnitType.KG)
        );
        Dish soupe_legume = new Dish(
                8,
                "Soupe de légumes",
                DishTypeEnum.STARTER,
                2000.0,
                dishIngredients
        );

        soupe_legume = dataRetriever.saveDish(soupe_legume);

        //then
        Assertions.assertEquals(soupe_legume.getDishIngredients(), soupe_legume.getDishIngredients(), "Sould be true");
        Assertions.assertEquals(soupe_legume.getName(), soupe_legume.getDishIngredients().getFirst().getDish().getName(), "Sould have same name");
        Assertions.assertNotEquals(new ArrayList<>(), soupe_legume.getDishIngredients(), "Throw a error");
    }

    @Test
    @DisplayName("l. Salade fraîche ingredient updated with oignon and fromage")
    void saladeFraicheIngredientUpdatedWithOignonAndFromage() {
        //when
        Dish salade_fraiche = new Dish(
                1,
                "Salade fraiche",
                DishTypeEnum.STARTER,
                1500.0,
                List.of(
                        new DishIngredient(
                                new Ingredient(7, "Oignon", 500.0, CategoryEnum.VEGETABLE),
                                0.5, UnitType.KG),
                        new DishIngredient(
                                new Ingredient(1, "Laitue", 2000.0, CategoryEnum.VEGETABLE),
                                2.0, UnitType.KG),
                        new DishIngredient(
                                new Ingredient(2, "Tomate", 200.00, CategoryEnum.VEGETABLE),
                                1.0, UnitType.KG),
                        new DishIngredient(
                                new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY),
                                0.3, UnitType.KG)
                )
        );
        salade_fraiche = dataRetriever.saveDish(salade_fraiche);

        //then
        Assertions.assertEquals(
                salade_fraiche.getDishIngredients().stream().map(DishIngredient::getIngredient).toList(),
                List.of(
                        new Ingredient(7, "Oignon", 500.0, CategoryEnum.VEGETABLE),
                        new Ingredient(1, "Laitue", 2000.0, CategoryEnum.VEGETABLE),
                        new Ingredient(2, "Tomate", 200.00, CategoryEnum.VEGETABLE),
                        new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY)
                ));
        Assertions.assertNotEquals(2, salade_fraiche.getDishIngredients().size(), "Throw error");
        Assertions.assertEquals(4, salade_fraiche.getDishIngredients().size(), "Return 4 ingredients");
        Assertions.assertEquals(
                ((500.0 * 0.5) + (2000.0 * 2.0) + (200.00 * 1.0) + (3000.0 * 0.3)),
                salade_fraiche.getDishCost(),
                "Return 5350"
        );
        Assertions.assertEquals(
                salade_fraiche.getDishCost(),
                (dataRetriever.getDishCost(1)),
                "Return same value [3350]"
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
                1500.0,
                List.of(
                        new DishIngredient(
                                new Ingredient(9, "Fromage", 3000.0, CategoryEnum.DAIRY),
                                5000.0, UnitType.KG)
                )
        );
        dataRetriever.saveDish(salade_fraiche);

        //then
        Assertions.assertEquals(1, salade_fraiche.getDishIngredients().size(), "Return 1");
        Assertions.assertEquals(
                "Fromage",
                salade_fraiche.getDishIngredients().getFirst().getIngredient().getName(),
                "Return Fromage"
        );
        Assertions.assertNotEquals(
                5700,
                salade_fraiche.getDishCost(),
                "Have different cost"
        );
        Assertions.assertNotEquals(
                5700,
                dataRetriever.getDishCost(1),
                "Have different cost"
        );
        Assertions.assertNotEquals(
                5700,
                salade_fraiche.getDishIngredients().getFirst().getIngredient().getPrice(),
                "Have different price"
        );
    }
}
