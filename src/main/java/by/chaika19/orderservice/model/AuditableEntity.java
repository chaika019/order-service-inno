package by.chaika19.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
public class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    LocalDateTime updatedAt;

}
