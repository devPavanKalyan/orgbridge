package org.verse.orgbridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("operation_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class History {

    @Id
    private String id;

    @NotBlank(message = "Email must not be empty")
    private String email;

    @NotNull(message = "Action must not be null")
    private Action action;

    @NotBlank(message = "Details must not be empty")
    private String details;

    @NotBlank(message = "Org must not be empty")
    @Column("organization_name")
    private String org;

    private boolean success;
    private Instant createdAt;
}
