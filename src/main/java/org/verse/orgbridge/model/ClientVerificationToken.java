package org.verse.orgbridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("client_verification_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientVerificationToken {

    @Id
    private String id;

    @NotBlank(message = "User id is required")
    private String userId;

    @NotBlank(message = "Verification token is required")
    @Size(min = 32, max = 128, message = "Invalid token length")
    private String token;

    @NotNull(message = "Expiry time is required")
    private Instant expiresAt;

    @Builder.Default
    private boolean used = false;

    @NotNull
    private Instant createdAt;

    private Instant usedAt;
}
