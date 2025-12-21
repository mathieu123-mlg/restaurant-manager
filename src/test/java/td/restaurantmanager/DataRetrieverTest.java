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
}
