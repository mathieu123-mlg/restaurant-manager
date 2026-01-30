package td.restaurantmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private final Integer id;
    private final String name;
    private final DishTypeEnum dishType;
    private final Double sellingPrice;
    private List<DishIngredient> dishIngredients;

    public Dish(Integer id, String name, DishTypeEnum dishType, Double sellingPrice, List<DishIngredient> dishIngredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.sellingPrice = sellingPrice;
        this.dishIngredients = dishIngredients;
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

    public List<DishIngredient> getDishIngredients() {
        return dishIngredients == null ? new ArrayList<>() : dishIngredients;
    }

    public void setDishIngredients(List<DishIngredient> dishIngredients) {
        if (dishIngredients == null) {
            this.dishIngredients = new ArrayList<>();
        } else {
            for (DishIngredient di : dishIngredients) {
                if (di != null && di.getDish() != this && di.getDish() != null) {
                    throw new IllegalArgumentException(
                            "DishIngredient references a different dish. Expected: " +
                            this.id + ", Found: " + di.getDish().getId()
                    );
                }
            }
            this.dishIngredients = new ArrayList<>(dishIngredients);
        }
    }

    public Double getDishCost() throws Exception {
        if (dishIngredients == null) {
            throw new Exception("Price null");
        }

        return getDishIngredients().stream()
                .mapToDouble(d_i -> d_i.getIngredient().getPrice() * d_i.getQuantityRequired())
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
               ", ingredients=" + getDishIngredients().stream().map(DishIngredient::getIngredient).toList() +
               '}';
    }
}
