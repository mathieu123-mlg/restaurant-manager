package td.restaurantmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private final Integer id;
    private final String name;
    private final DishTypeEnum dishType;
    private final Double sellingPrice;
    private final List<DishIngredient> dishIngredients;

    public Dish(Integer id, String name, DishTypeEnum dishType, Double sellingPrice) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.sellingPrice = sellingPrice;
        this.dishIngredients = null;
    }

    public Dish(Integer id, String name, DishTypeEnum dishType, Double sellingPrice, List<DishIngredient> dishIngredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.sellingPrice = sellingPrice;
        this.dishIngredients = dishIngredients;
    }

    public Dish(List<Ingredient> ingredients, Integer id, String name, DishTypeEnum dishType, Double sellingPrice) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.sellingPrice = sellingPrice;
        this.dishIngredients = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            DishIngredient d_i = new DishIngredient(
                    this,
                    ingredient,
                    ingredient.getQuantityRequired(),
                    ingredient.getUnit()
            );
            dishIngredients.add(d_i);
        }
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public List<Ingredient> getDishIngredients() {
        if (dishIngredients == null) {
            return List.of();
        }

        return dishIngredients.stream()
                .map(d_i -> {
                    Ingredient i = d_i.getIngredient();
                    return new Ingredient(
                            i.getId(),
                            i.getName(),
                            i.getPrice(),
                            i.getCategory(),
                            d_i.getQuantityRequired(),
                            d_i.getUnit()
                    );
                })
                .toList();
    }

    public Double getDishCost() throws Exception {
        if (dishIngredients == null) {
            throw new Exception("Price null");
        }

        return getDishIngredients().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantityRequired())
                .sum();
    }

    public Double getGrossMargin() throws Exception {
        if (sellingPrice == null) {
            throw new RuntimeException("Cannot calculate marge because sellingPrice is null");
        }
        return getSellingPrice() - getDishCost();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Dish{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", dishType=" + dishType +
               ", sellingPrice=" + sellingPrice +
               ", ingredients=" + getDishIngredients() +
               '}';
    }
}
