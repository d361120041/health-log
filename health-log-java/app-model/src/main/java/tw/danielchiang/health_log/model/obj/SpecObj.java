package tw.danielchiang.health_log.model.obj;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import lombok.Value;

@Value
public class SpecObj {
    private final boolean isSpec;
    private final List<JoinObj> joins;
    private final List<WhereObj> wheres;

    public <T> Specification<T> toSpec() {
        return (root, query, criteriaBuilder) -> {
            
            Predicate finalPredicate = criteriaBuilder.and();

            /**
             * 1. Join
             */
            if (joins != null && !joins.isEmpty()) {
                for (JoinObj join : joins) {
                    if (join.isJoin()) {
                        root.join(join.getJoin());
                    }
                }
            }

            /**
             * 2. Where
             */
            if (wheres != null && !wheres.isEmpty()) {
                for (WhereObj where : wheres) {
                    if (where.isWhere()) {
                        Predicate predicate = null;
                        switch (where.getOperator()) {
                            case EQ:
                                predicate = criteriaBuilder.equal(root.get(where.getColumn()), where.getValue());
                                break;
                            case NE:
                                predicate = criteriaBuilder.notEqual(root.get(where.getColumn()), where.getValue());
                                break;
                            case GT:
                                predicate = criteriaBuilder.greaterThan(root.get(where.getColumn()), where.getValue());
                                break;
                            case GTE:
                                predicate = criteriaBuilder.greaterThanOrEqualTo(root.get(where.getColumn()), where.getValue());
                                break;
                            case LT:
                                predicate = criteriaBuilder.lessThan(root.get(where.getColumn()), where.getValue());
                                break;
                            case LTE:
                                predicate = criteriaBuilder.lessThanOrEqualTo(root.get(where.getColumn()), where.getValue());
                                break;
                            case LIKE:
                                predicate = criteriaBuilder.like(root.get(where.getColumn()), where.getValue());
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid operator: " + where.getOperator());
                        }

                        if (predicate != null) {
                            if (finalPredicate == null) {
                                finalPredicate = predicate;
                            } else {
                                switch (where.getLogic()) {
                                    case AND:
                                        finalPredicate = criteriaBuilder.and(finalPredicate, predicate);
                                        break;
                                    case OR:
                                        finalPredicate = criteriaBuilder.or(finalPredicate, predicate);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Invalid logic: " + where.getLogic());
                                }
                            }
                        }
                    }
                }
            }
            return finalPredicate;    
        };
    }
}
