package td.restaurantmanager;

import java.time.Instant;
import java.util.Objects;

public class StockMovement {
    private final Integer id;
    private final StockValue value;
    private final MovementTypeEnum type;
    private final Instant creationDatetime;

    public StockMovement(Integer id, StockValue value, MovementTypeEnum type, Instant creationDatetime) {
        this.id = id;
        this.value = Objects.isNull(value) ? new StockValue(0.0) : value;
        this.type = type;
        this.creationDatetime = creationDatetime;
    }

    public StockMovement(Integer id, StockValue value, MovementTypeEnum type) {
        this.id = id;
        this.value = Objects.isNull(value) ? new StockValue(0.0) : value;
        this.type = type;
        this.creationDatetime = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public StockValue getValue() {
        return value;
    }

    public MovementTypeEnum getType() {
        return type;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id) && Objects.equals(value, that.value) && type == that.type && Objects.equals(creationDatetime, that.creationDatetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, type, creationDatetime);
    }

    @Override
    public String toString() {
        return "StockMovement{" +
               "id=" + id +
               ", value=" + value +
               ", type=" + type +
               ", creationDatetime=" + creationDatetime +
               '}';
    }
}
