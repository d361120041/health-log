package tw.danielchiang.health_log.model.dto.request;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;
import tw.danielchiang.health_log.model.obj.PageObj;
import tw.danielchiang.health_log.model.obj.SearchObj;
import tw.danielchiang.health_log.model.obj.SpecObj;

@Data
public class SearchRequestDTO<T> {

    private final SpecObj specObj;
    private final PageObj pageObj;

    public SearchObj<T> toSearchObj() {

        SearchObj<T> searchObj;
        
        if (pageObj == null) {
            throw new IllegalArgumentException("Page object is required");
        }
        
        if (specObj == null) {
            searchObj = SearchObj.<T>builder()
                    .isSpec(false)
                    .specification(Specification.allOf())
                    .isPaged(pageObj.isPaged())
                    .pageable(pageObj.toPageable())
                    .build();
        } else {
            searchObj = SearchObj.<T>builder()
                    .isSpec(specObj.isSpec())
                    .specification(specObj.toSpec())
                    .isPaged(pageObj.isPaged())
                    .pageable(pageObj.toPageable())
                    .build();
        }
        return searchObj;
    }
}
