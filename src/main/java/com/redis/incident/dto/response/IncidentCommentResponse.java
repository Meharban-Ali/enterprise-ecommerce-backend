package com.redis.incident.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCommentResponse {
    private Long id;
    private Long incidentId;
    private String comment;
    private String createdBy;
    private LocalDateTime createdAt;
}
