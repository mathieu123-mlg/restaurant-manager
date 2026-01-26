package td.restaurantmanager;

import java.util.Objects;

public class DishIngredient {
    private final Integer id;
    private final Dish dish;
    private final Ingredient ingredient;
    private final Double quantityRequired;
    private final UnitType unit;

    public DishIngredient(Integer id, Dish dish, Ingredient ingredient, Double quantityRequired, UnitType unit) {
        this.id = id;
        this.dish = dish;
        this.ingredient = ingredient;
        this.quantityRequired = quantityRequired;
        this.unit = unit;
    }

    public Integer getId() {
        return id;
    }

    public Dish getDish() {
        return dish;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public Double getQuantityRequired() {
        return quantityRequired;
    }

    public UnitType getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DishIngredient that = (DishIngredient) o;
        return Objects.equals(dish, that.dish) && Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dish, ingredient);
    }

    @Override
    public String toString() {
        return "DishIngredient{" +
               "id=" + id +
               ", dish=" + dish +
               ", ingredient=" + ingredient +
               ", quantityRequired=" + quantityRequired +
               ", unit=" + unit +
               '}';
    }
}
