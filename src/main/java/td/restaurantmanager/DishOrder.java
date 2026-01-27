package td.restaurantmanager;

import java.util.Objects;

public class DishOrder {
    private final Integer id;
    private final Dish dish;
    private final Integer quantity;

    public DishOrder(Integer id, Dish dish, Integer quantity) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
    }

    public Integer getId() {
        return id;
    }

    public Dish getDish() {
        return dish;
    }

    public Integer getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DishOrder dishOrder = (DishOrder) o;
        return Objects.equals(id, dishOrder.id) && Objects.equals(dish, dishOrder.dish) && Objects.equals(quantity, dishOrder.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dish, quantity);
    }

    @Override
    public String toString() {
        return "DishOrder{" +
               "id=" + id +
               ", dish=" + dish +
               ", quantity=" + quantity +
               '}';
    }
}
