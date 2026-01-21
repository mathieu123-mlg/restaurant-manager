package td.restaurantmanager;

import java.util.Objects;

public class DishIngredient {
    private final Integer id;
    private final Dish dish;
    private final Ingredient ingredient;
    private final Double quatity_required;
    private final UnitType unit;

    public DishIngredient(Integer id, Dish dish, Ingredient ingredient, Double quatity_required, UnitType unit) {
        this.id = id;
        this.dish = dish;
        this.ingredient = ingredient;
        this.quatity_required = quatity_required;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "DishIngredient{" +
               "id=" + id +
               ", dish=" + dish +
               ", ingredient=" + ingredient +
               ", quatity_required=" + quatity_required +
               ", unit=" + unit +
               '}';
    }
}
