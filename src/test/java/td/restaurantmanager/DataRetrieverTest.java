package td.restaurantmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DataRetrieverTest {
    private DataRetriever dataRetriever;

    //given
    @BeforeEach
    public void setUp() {
        dataRetriever = new DataRetriever();
    }

    @Test
    @DisplayName("Find dish by id=1")
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
    @DisplayName("Find dish by id=999")
    void findDishById_999() {
        //Then
        Assertions.assertThrows(
                RuntimeException.class,
                () -> {
                    Dish find_dish_999 = dataRetriever.findDishById(999);
                },
                "Should have thrown an exception"
        );
    }

    @Test
    @DisplayName("Find ingredients page=2, size=2")
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
    @DisplayName("Find ingredients page=3, size=5")
    void findIngredientsPage_3Size_5() {
        //when
        List<Ingredient> ingredients_p3_s5 = dataRetriever.findIngredients(3, 5);

        //then
        Assertions.assertEquals(0, ingredients_p3_s5.size(), "Should return empty list");
    }
}
