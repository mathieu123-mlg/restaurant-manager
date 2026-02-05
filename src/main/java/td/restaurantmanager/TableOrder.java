package td.restaurantmanager;

import java.time.Instant;
import java.util.Objects;

public class TableOrder {
    private final Table table;
    private final Instant arrivalDatetime;
    private final Instant departureDatetime;

    public TableOrder(Table table, Instant arrivalDatetime, Instant departureDatetime) {
        if (arrivalDatetime == null) {
            throw new IllegalArgumentException("Arrival datetime cannot be null");
        }
        if (departureDatetime != null && departureDatetime.isBefore(arrivalDatetime)) {
            throw new IllegalArgumentException("Departure datetime must be after arrival");
        }
        this.table = table;
        this.arrivalDatetime = arrivalDatetime;
        this.departureDatetime = departureDatetime;
    }

    public Table getTable() {
        return table;
    }

    public Instant getArrivalDatetime() {
        return arrivalDatetime;
    }

    public Instant getDepartureDatetime() {
        return departureDatetime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TableOrder that = (TableOrder) o;
        return Objects.equals(table, that.table) && Objects.equals(arrivalDatetime, that.arrivalDatetime) && Objects.equals(departureDatetime, that.departureDatetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, arrivalDatetime, departureDatetime);
    }

    @Override
    public String toString() {
        return "TableOrder{" +
               "tableNumber=" + table +
               ", arrivalDatetime=" + arrivalDatetime +
               ", departureDatetime=" + departureDatetime +
               '}';
    }
}
