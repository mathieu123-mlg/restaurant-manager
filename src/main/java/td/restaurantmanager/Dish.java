package td.restaurantmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private final Integer id;
    private final String name;
    private final DishTypeEnum dishType;
    private final Double price;
    private final List<DishIngredient> dishIngredients;

    public Dish(Integer id, String name, DishTypeEnum dishType, Double price, List<DishIngredient> dishIngredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.price = price;
        this.dishIngredients = Objects.requireNonNullElseGet(dishIngredients, ArrayList::new);
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

    public Double getPrice() {
        return price;
    }

    public List<DishIngredient> getDishIngredients() { return dishIngredients; }

    public Double getDishCost() {
        if (price == null) {
            throw new RuntimeException("Price is null");
        }
        return getDishIngredients().stream()
                .mapToDouble(d_i -> d_i.getIngredient().getPrice() * d_i.getQuantityRequired())
                .sum();
    }

    public Double getGrossMargin() {
        if (price == null) {
            throw new RuntimeException("Cannot calculate marge because price is null");
        }
        return getPrice() - getDishCost();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id) && Objects.equals(name, dish.name) && dishType == dish.dishType
               && Objects.equals(price, dish.price) && Objects.equals(dishIngredients, dish.dishIngredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, price, dishIngredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", dishType=" + dishType +
               ", price=" + price +
               ", ingredients=" + getDishIngredients().stream().map(DishIngredient::getIngredient).toList() +
               '}';
    }
}
