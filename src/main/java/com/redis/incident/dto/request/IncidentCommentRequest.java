package com.redis.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCommentRequest {
    @NotBlank(message = "Comment text cannot be blank")
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;
}
