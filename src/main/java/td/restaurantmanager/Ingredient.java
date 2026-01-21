package td.restaurantmanager;

import java.util.Objects;

public class Ingredient {
    private final Integer id;
    private final String name;
    private final Double price;
    private final CategoryEnum category;
    private Double quantity_required;
    private UnitType unit;

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Ingredient(Integer id, String name, Double price, CategoryEnum category, Double quantity_required, UnitType unit) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantity_required = quantity_required;
        this.unit = unit;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public Double getQuantity_required() {
        return quantity_required;
    }

    public UnitType getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(price, that.price) && category == that.category && Objects.equals(quantity_required, that.quantity_required) && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category, quantity_required, unit);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", quantity_required=" + quantity_required +
               ", unit=" + unit +
               ", category=" + category +
               '}';
    }
}
