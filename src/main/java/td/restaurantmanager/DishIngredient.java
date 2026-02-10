package td.restaurantmanager;

public class DishIngredient {
    private Dish dish;
    private final Ingredient ingredient;
    private final Double quantityRequired;
    private final UnitType unit;

    public DishIngredient(Ingredient ingredient, Double quantityRequired, UnitType unit) {
        this.ingredient = ingredient;
        this.quantityRequired = quantityRequired;
        this.unit = unit;
    }

    public Dish getDish() { return dish; }

    public Ingredient getIngredient() { return ingredient; }

    public Double getQuantityRequired() { return quantityRequired; }

    public UnitType getUnit() { return unit; }

    public void setDish(Dish dish) { this.dish = dish; }

    @Override
    public String toString() {
        return "DishIngredient{" +
               ", ingredient=" + ingredient +
               ", quantityRequired=" + quantityRequired +
               ", unit=" + unit +
               '}';
    }
}
