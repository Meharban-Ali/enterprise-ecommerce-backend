package com.redis.common.base;

import lombok.NoArgsConstructor;

import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @lombok.Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    @lombok.Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    @lombok.Builder.Default
    private String createdBy = "system";

    @LastModifiedBy
    @Column(name = "updated_by")
    @lombok.Builder.Default
    private String updatedBy = "system";

    @Version
    @lombok.Builder.Default
    private Integer version = 0;
}
