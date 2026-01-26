package td.restaurantmanager;

import java.util.Objects;

public class Ingredient {
    private final Integer id;
    private final String name;
    private final Double price;
    private final CategoryEnum category;

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", category=" + category +
               '}';
    }
}
