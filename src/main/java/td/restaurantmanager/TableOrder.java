package td.restaurantmanager;

import java.time.Instant;
import java.util.Objects;

public class TableOrder {
    private final Table table;
    private final Instant arrivalDatetime;
    private final Instant departureDatetime;

    public TableOrder(Table table, Instant arrivalDatetime, Instant departureDatetime) {
        this.table = table;
        this.arrivalDatetime = arrivalDatetime;
        this.departureDatetime = departureDatetime;
    }

    public Table getTalbe() {
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
               "table=" + table +
               ", arrivalDatetime=" + arrivalDatetime +
               ", departureDatetime=" + departureDatetime +
               '}';
    }
}
