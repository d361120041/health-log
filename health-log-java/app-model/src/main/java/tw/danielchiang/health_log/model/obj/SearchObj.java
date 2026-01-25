package tw.danielchiang.health_log.model.obj;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchObj<T> {

    /**
     * 是否使用 Specification
     */
    private final boolean isSpec;
    private final Specification<T> specification;

    /**
     * 是否使用 Pageable
     */
    private final boolean isPaged;
    /**
     * 是否使用 Sort
     */
    private final boolean isSorted;
    private final Pageable pageable;

    public SearchObj<T> addSpecification(Specification<T> specification, WhereObj.Logic logic) {
        Specification<T> baseSpec = Specification.allOf(this.specification);
        Specification<T> combinedSpec = switch (logic) {
            case AND -> baseSpec.and(specification);
            case OR -> baseSpec.or(specification);
            default -> baseSpec;
        };

        return SearchObj.<T>builder()
            .isSpec(this.isSpec)
            .specification(combinedSpec)
            .isPaged(this.isPaged)
            .isSorted(this.isSorted)
            .pageable(this.pageable)
            .build();
    }
}