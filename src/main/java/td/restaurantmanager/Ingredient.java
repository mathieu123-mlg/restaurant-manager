package td.restaurantmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Ingredient {
    private final Integer id;
    private final String name;
    private final Double price;
    private final CategoryEnum category;
    private List<StockMovement> stockMovementList;

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Ingredient(Integer id, String name, Double price, CategoryEnum category, List<StockMovement> stockMovementList) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stockMovementList = stockMovementList;
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

    public List<StockMovement> getStockMovementList() {
        return stockMovementList == null ? new ArrayList<>() : stockMovementList;
    }

    public StockValue getStockValueAt(Instant instant) {
        if (instant == null || stockMovementList == null || stockMovementList.isEmpty()) {
            return new StockValue(0.0, UnitType.KG);
        }

        double total = 0.0;
        UnitType unit = UnitType.KG;

        for (StockMovement movement : stockMovementList) {
            if (movement.getCreationDatetime().isBefore(instant) ||
                movement.getCreationDatetime().equals(instant)) {

                if (movement.getValue() != null) {
                    double quantity = movement.getValue().getQuantity() != null
                            ? movement.getValue().getQuantity()
                            : 0.0;

                    if (movement.getType() == MouvementTypeEnum.IN) {
                        total += quantity;
                    } else if (movement.getType() == MouvementTypeEnum.OUT) {
                        total -= quantity;
                    }

                    if (movement.getValue().getUnit() != null) {
                        unit = movement.getValue().getUnit();
                    }
                }
            }
        }

        return new StockValue(total, unit);
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
               ", stockMovementList=" + stockMovementList +
               '}';
    }
}
