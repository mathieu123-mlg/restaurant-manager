package td.restaurantmanager;

import java.time.Instant;
import java.util.Objects;

public class StockMovement {
    private final Integer id;
    private final Ingredient ingredient;
    private final MouvementTypeEnum type;
    private final Double quantity;
    private final Instant creationDatetime;


    public StockMovement(Integer id, Ingredient ingredient, MouvementTypeEnum type, Double quantity, Instant creationDatetime) {
        this.id = id;
        this.ingredient = ingredient;
        this.type = type;
        this.quantity = quantity;
        this.creationDatetime = creationDatetime;
    }

    public Integer getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public MouvementTypeEnum getType() {
        return type;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    @Override
    public String toString() {
        return "StockMovement{" +
               "id=" + id +
               ", ingredient=" + ingredient +
               ", unit=" + type +
               ", quantity=" + quantity +
               ", creationDatetime=" + creationDatetime +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id) && Objects.equals(ingredient, that.ingredient) && type == that.type && Objects.equals(quantity, that.quantity) && Objects.equals(creationDatetime, that.creationDatetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ingredient, type, quantity, creationDatetime);
    }
}
