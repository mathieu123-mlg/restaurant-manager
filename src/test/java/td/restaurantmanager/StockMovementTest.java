package td.restaurantmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

public class StockMovementTest {
    private final DataRetriever data = new DataRetriever();
    private final Instant t = Instant.parse("2024-01-06T12:00:00.000000Z");

    private Ingredient laitue;
    private Ingredient tomate;
    private Ingredient poulet;
    private Ingredient chocolat;
    private Ingredient beurre;

    @BeforeEach
    public void setup() {
        laitue = data.findIngredientsByCriteria("laitue", null, null, 1, 1).getFirst();
        tomate = data.findIngredientsByCriteria("tomate", null, null, 1, 1).getFirst();
        poulet = data.findIngredientsByCriteria("poulet", null, null, 1, 1).getFirst();
        chocolat = data.findIngredientsByCriteria("chocolat", null, null, 1, 1).getFirst();
        beurre = data.findIngredientsByCriteria("beurre", null, null, 1, 1).getFirst();
    }

    @Test
    public void StockMovementTest() {
        Assertions.assertEquals(4.8, laitue.getStockValueAt(t).getQuantity());
        Assertions.assertEquals(3.85, tomate.getStockValueAt(t).getQuantity());
        Assertions.assertEquals(9.0, poulet.getStockValueAt(t).getQuantity());
        Assertions.assertEquals(2.7, chocolat.getStockValueAt(t).getQuantity());
        Assertions.assertEquals(2.3, beurre.getStockValueAt(t).getQuantity());
    }
}
