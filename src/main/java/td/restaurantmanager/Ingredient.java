package td.restaurantmanager;

import java.util.Objects;

public class Ingredient {
    private final Integer id;
    private final String name;
    private final Double price;
    private final CategoryEnum category;
    private final Double quantityRequired;
    private final UnitType unit;

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantityRequired = null;
        this.unit = UnitType.KG;
    }

    public Ingredient(Integer id, String name, Double price, CategoryEnum category, Double quantityRequired, UnitType unit) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantityRequired = quantityRequired;
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

    public Double getQuantityRequired() {
        return quantityRequired;
    }

    public UnitType getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", category=" + category +
               ", quantityRequired=" + quantityRequired +
               ", unit=" + unit +
               '}';
    }
}
