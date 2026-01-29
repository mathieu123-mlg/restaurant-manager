package td.restaurantmanager;

import java.util.*;
import java.util.Objects;

public class Table {
    private final Integer id;
    private final int number;
    private final List<Order> orders;

    public Table(Integer id, int number, List<Order> orders) {
        this.id = id;
        this.number = number;
        this.orders = orders;
    }

    public Table(Integer id, int number) {
        this.id = id;
        this.number = number;
        this.orders = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public List<Order> getOrders() {
        return orders;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return number == table.number && Objects.equals(id, table.id) && Objects.equals(orders, table.orders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, orders);
    }

    @Override
    public String toString() {
        return "Table{" +
               "id=" + id +
               ", number=" + number +
               ", orders=" + orders +
               '}';
    }
}
