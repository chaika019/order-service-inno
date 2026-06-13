package by.chaika19.orderservice.repository.specification;

import by.chaika19.orderservice.model.Order;
import by.chaika19.orderservice.model.OrderStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> hasStatus(List<OrderStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("orderItems", JoinType.LEFT);
            }

            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> createBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> {
            if (start == null && end == null) {
                return criteriaBuilder.conjunction();
            }
            if (start != null && end != null) {
                return criteriaBuilder.between(root.get("createdAt"), start, end);
            }
            if (start != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }
}
