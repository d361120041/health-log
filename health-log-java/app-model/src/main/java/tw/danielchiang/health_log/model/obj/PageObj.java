package tw.danielchiang.health_log.model.obj;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageObj {
    private final boolean isPaged;
    private final Integer page;
    private final Integer size;

    private final boolean isSorted;
    private final List<OrderObj> orders;

    public Pageable toPageable() {
        return PageRequest.of(
            page, 
            size, 
            Sort.by(orders.stream().map(OrderObj::toOrder).toList()));
    }
}
