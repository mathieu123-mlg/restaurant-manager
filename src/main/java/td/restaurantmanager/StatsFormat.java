package td.restaurantmanager;

import java.time.Instant;

public class StatsFormat {
    private final Instant date;
    private final StockValue value;

    public StatsFormat(Instant date, StockValue value) {
        this.date = date;
        this.value = value;
    }

    @Override
    public String toString() {
        return "\n[date='" + date + "', " + value + ']';
    }
}
