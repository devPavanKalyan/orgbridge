package org.verse.orgbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

@Table("clients")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Client implements UserDetails {

    @Id
    private String id;

    private String fullName;
    private String username;
    private String password;
    private boolean emailVerified;

    @Builder.Default
    private String roles = "ROLE_USER";

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoleList().stream()
                .map(role -> (GrantedAuthority) () -> role)
                .toList();
    }

    public Collection<String> getRoleList() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}
