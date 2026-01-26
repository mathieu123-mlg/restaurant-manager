package td.restaurantmanager;

import java.time.Instant;
import java.util.Objects;

public class StockMovement {
    private final Integer id;
    private final StockValue value;
    private final MouvementTypeEnum type;
    private final Instant creationDatetime;

    public StockMovement(Integer id, StockValue value, MouvementTypeEnum type, Instant creationDatetime) {
        this.id = id;
        this.value = value;
        this.type = type;
        this.creationDatetime = creationDatetime;
    }

    public Integer getId() {
        return id;
    }

    public StockValue getValue() {
        return value;
    }

    public MouvementTypeEnum getType() {
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
