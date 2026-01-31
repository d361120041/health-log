package tw.danielchiang.health_log.model.domain;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageableData<T> implements Data<T> {
    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int number;
    private final int size;
    private final boolean first;
    private final boolean last;
    private final int numberOfElements;

    public static <T> PageableData<T> of(Page<T> page) {
        return new PageableData<>(
            page.getContent(), 
            page.getTotalElements(), 
            page.getTotalPages(), 
            page.getNumber(), 
            page.getSize(), 
            page.isFirst(), 
            page.isLast(), 
            page.getNumberOfElements());
    }
}
