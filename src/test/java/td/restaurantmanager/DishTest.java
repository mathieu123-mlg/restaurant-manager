package td.restaurantmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DishTest {
    private final DataRetriever data = new DataRetriever();

    private final Dish salade_fraiche = data.findDishById(1);
    private final Dish poulet_grille = data.findDishById(2);
    private final Dish riz_aux_legumes = data.findDishById(3);
    private final Dish gateau_aux_chocolat = data.findDishById(4);
    private final Dish salade_de_fruit = data.findDishById(5);

    @Test
    public void getDishCostTest() throws Exception {
        Assertions.assertEquals(250.0, salade_fraiche.getDishCost());
        Assertions.assertEquals(4500.0, poulet_grille.getDishCost());
        Assertions.assertEquals(0.0, riz_aux_legumes.getDishCost());
        Assertions.assertEquals(1400.0, gateau_aux_chocolat.getDishCost());
        Assertions.assertEquals(0.0, salade_de_fruit.getDishCost());
    }

    @Test
    public void getGrossMarginTest() throws Exception {
        Assertions.assertEquals(3250.0, salade_fraiche.getGrossMargin());
        Assertions.assertEquals(7500.0, poulet_grille.getGrossMargin());
        Assertions.assertThrows(
                RuntimeException.class, riz_aux_legumes::getGrossMargin
                , "prix null"
        );
        Assertions.assertEquals(6600.0, gateau_aux_chocolat.getGrossMargin());
        Assertions.assertThrows(
                RuntimeException.class, salade_de_fruit::getGrossMargin
                , "prix null"
        );
    }
}
