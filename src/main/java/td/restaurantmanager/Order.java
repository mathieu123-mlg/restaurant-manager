package td.restaurantmanager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private final Integer id;
    private final String reference;
    private final Instant creationDatetime;
    private final List<Order> dishOrders;

    public Order(Integer id, String reference, Instant creationDatetime, List<Order> dishOrders) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
        this.dishOrders = dishOrders;
    }

    public Integer getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public List<Order> getDishOrders() {
        return dishOrders;
    }

    public Double getTotalAmountWithoutVAt() {
        return null;
    }

    public Double getTotalAmountWithVAt() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) && Objects.equals(reference, order.reference) && Objects.equals(creationDatetime, order.creationDatetime) && Objects.equals(dishOrders, order.dishOrders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime, dishOrders);
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
