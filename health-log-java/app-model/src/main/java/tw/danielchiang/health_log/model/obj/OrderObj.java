package tw.danielchiang.health_log.model.obj;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderObj {
    private final String field;
    private final String order;

    public Order toOrder() {
        return new Order(Direction.fromString(order), field);
    }
}
