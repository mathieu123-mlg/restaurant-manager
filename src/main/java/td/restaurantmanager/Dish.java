package td.restaurantmanager;

import java.util.List;
import java.util.Objects;

public class Dish {
    private final Integer id;
    private final String name;
    private final DishTypeEnum dishType;
    private final List<Ingredient> ingredients;

    public Dish(int id, String name, DishTypeEnum dishType, List<Ingredient> ingredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.ingredients = ingredients;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        if (this.ingredients != null) {
            for (Ingredient ingredient : ingredients) {
                ingredient.setDish(this);
            }
        }
    }

    public Double getDishCost() {
        return ingredients.stream().mapToDouble(Ingredient::getPrice).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return id == dish.id && Objects.equals(name, dish.name) && dishType == dish.dishType && Objects.equals(ingredients, dish.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, ingredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", ingredients=" + ingredients +
                '}';
    }
}
