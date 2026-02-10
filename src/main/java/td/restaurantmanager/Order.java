package td.restaurantmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private final Integer id;
    private final String reference;
    private final Instant creationDatetime;
    private final List<DishOrder> dishOrders;
    
    public Order(Integer id, String reference, Instant creationDatetime, List<DishOrder> dishOrders) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime == null ? Instant.now() : creationDatetime;
        this.dishOrders = Objects.requireNonNullElseGet(dishOrders, ArrayList::new);
    }

    public Integer getId() { return id; }

    public String getReference() { return reference; }

    public Instant getCreationDatetime() { return creationDatetime; }

    public List<DishOrder> getDishOrders() { return Collections.unmodifiableList(dishOrders); }

//    public TableOrder getTable() { return table; }

    public Double getTotalAmountWithoutVat() {
        return dishOrders.stream()
                .mapToDouble(d_o -> d_o.getDish().getPrice() * d_o.getQuantity())
                .sum();
    }

    public Double getTotalAmountWithVat() {
        final double VAT_RATE = 0.20;
        return getTotalAmountWithoutVat() * (1 + VAT_RATE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) && Objects.equals(reference, order.reference)
                && Objects.equals(creationDatetime, order.creationDatetime) && Objects.equals(dishOrders, order.dishOrders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime, dishOrders);
    }

    @Override
    public String toString() {
        return "Order{" +
               "id=" + id +
                "totalAmountWithoutVat=" + getTotalAmountWithoutVat() +
                "totalAmountWithVat=" + getTotalAmountWithVat() +
                ", reference='" + reference + '\'' +
               ", creationDatetime=" + creationDatetime +
               ", dishOrders=" + dishOrders +
               '}';
    }
}
