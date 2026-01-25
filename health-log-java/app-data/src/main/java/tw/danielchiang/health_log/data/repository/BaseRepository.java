package tw.danielchiang.health_log.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import tw.danielchiang.health_log.model.obj.SearchObj;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    default Page<T> search(SearchObj<T> searchObj) {

        Specification<T> specification = searchObj.getSpecification();
        Pageable pageable = searchObj.getPageable();

        if (pageable == null) {
            throw new IllegalArgumentException("Pageable is required");
        }

        return findAll(specification, pageable);
    }
    
}
