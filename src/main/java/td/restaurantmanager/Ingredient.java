package td.restaurantmanager;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Ingredient {
    private final Integer id;
    private final String name;
    private final Double price;
    private final CategoryEnum category;
    private final List<StockMovement> stockMovementList;

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Price cannot be null or negative");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stockMovementList = new ArrayList<>();
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

    public List<StockMovement> getStockMovementList() { return Collections.unmodifiableList(stockMovementList); }

    public StockValue getStockValueAt(Instant instant) {
        if (stockMovementList == null || stockMovementList.isEmpty()) {
            return new StockValue(0.0, UnitType.KG);
        }

        Set<UnitType> units = stockMovementList.stream()
                .map(m -> m.getValue().getUnit())
                .collect(Collectors.toSet());
        if (units.size() > 1) {
            throw new IllegalStateException("Multiple units detected for ingredient " + name);
        }

        Map<UnitType, List<StockMovement>> unitSet = stockMovementList.stream()
                .collect(Collectors.groupingBy(stockMovement -> stockMovement.getValue().getUnit()));

        if (unitSet.size() > 1) {
            throw new RuntimeException("Multiple unit found and not handle for conversion");
        }

        UnitType unit = unitSet.keySet().stream().findFirst().orElse(UnitType.KG);

        List<StockMovement> stockMovements = stockMovementList.stream()
                .filter(stockMovement -> !stockMovement.getCreationDatetime().isAfter(instant))
                .toList();

        double movementIn = stockMovements.stream()
                .filter(stockMovement -> stockMovement.getType().equals(MovementTypeEnum.IN))
                .flatMapToDouble(stockMovement -> DoubleStream.of(stockMovement.getValue().getQuantity()))
                .sum();

        double movementOut = stockMovements.stream()
                .filter(stockMovement -> stockMovement.getType().equals(MovementTypeEnum.OUT))
                .flatMapToDouble(stockMovement -> DoubleStream.of(stockMovement.getValue().getQuantity()))
                .sum();

        return new StockValue((movementIn - movementOut), unit);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name)
               && Objects.equals(price, that.price) && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", category=" + category +
               ", actualStock=" + getStockValueAt(Instant.now()) +
               '}';
    }
}
