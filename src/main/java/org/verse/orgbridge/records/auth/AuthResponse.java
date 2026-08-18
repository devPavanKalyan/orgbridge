package org.verse.orgbridge.records.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse implements Serializable {
    private String accessToken;
    private String refreshToken;
    private User user;

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String fullName;
        private String username;
    }
}
