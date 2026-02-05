package td.restaurantmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    private final Integer id;
    private final String reference;
    private final Instant creationDatetime;
    private final List<DishOrder> dishOrders;
    private final TableOrder table;

    public Order(Integer id, String reference, Instant creationDatetime, List<DishOrder> dishOrders, TableOrder table) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("Reference cannot be null or empty");
        }
        if (table == null) {
            throw new IllegalArgumentException("Table of order cannot be null");
        }
        this.id = id;
        this.reference = reference;
        if (creationDatetime == null) {
            this.creationDatetime = Instant.now();
        } else {
            this.creationDatetime = creationDatetime;
        }
        this.dishOrders = Objects.requireNonNullElseGet(dishOrders, ArrayList::new);
        this.table = table;
    }

    public Integer getId() { return id; }

    public String getReference() { return reference; }

    public Instant getCreationDatetime() { return creationDatetime; }

    public List<DishOrder> getDishOrders() { return dishOrders; }

    public TableOrder getTable() { return table; }

    public Double getTotalAmountWithoutVAT() {
        return dishOrders.stream()
                .mapToDouble(d_o -> d_o.getDish().getPrice() * d_o.getQuantity())
                .sum();
    }

    public Double getTotalAmountWithVAt() {
        final double VAT_RATE = 0.20;
        return getTotalAmountWithoutVAT() * (1 + VAT_RATE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) && Objects.equals(reference, order.reference) && Objects.equals(creationDatetime, order.creationDatetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime);
    }

    @Override
    public String toString() {
        return "Order{" +
               "id=" + id +
               ", reference='" + reference + '\'' +
               ", creationDatetime=" + creationDatetime +
               ", dishOrders=" + dishOrders +
               '}';
    }
}
