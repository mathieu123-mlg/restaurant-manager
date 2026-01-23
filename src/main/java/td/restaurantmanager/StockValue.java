package td.restaurantmanager;

import java.util.Objects;

public class StockValue {
    private final Double quantity;
    private final UnitType unit;

    public StockValue(Double quantity, UnitType unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public Double getQuantity() {
        return quantity;
    }

    public UnitType getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "StockValue{" +
               "quantity=" + quantity +
               ", unit=" + unit +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StockValue that = (StockValue) o;
        return Objects.equals(quantity, that.quantity) && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, unit);
    }
}
