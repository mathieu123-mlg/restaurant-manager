package td.restaurantmanager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Table {
    private final Integer id;
    private final int number;
    private final List<Order> orders;

    public Table(Integer id, int number, List<Order> orders) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be positive");
        }
        this.id = id;
        this.number = number;
        this.orders = orders;
    }

    public Table(Integer id, int number) {
        this.id = id;
        this.number = number;
        this.orders = new ArrayList<>();
    }

    public Integer getId() { return id; }

    public int getNumber() { return number; }

    public List<Order> getOrders() { return Collections.unmodifiableList(orders); }

    public boolean isAvailable(Instant t) {
        if (t == null) {
            throw new IllegalArgumentException("Instant t cannot be null");
        }
        if (orders.isEmpty()) {
            return true;
        }

        return orders.stream().noneMatch(order -> {
            TableOrder tableOrder = order.getTable();
            Instant start = tableOrder.getArrivalDatetime();
            Instant end   = tableOrder.getDepartureDatetime();

            if (start == null) {
                return false;
            }
            return !t.isBefore(start) && (end == null || t.isBefore(end));
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return number == table.number && Objects.equals(id, table.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number);
    }

    @Override
    public String toString() {
        return "Table{" +
               "id=" + id +
               ", number=" + number +
               '}';
    }
}
