package com.redis.infrastructure.config;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String changedBy;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private int version;
}
