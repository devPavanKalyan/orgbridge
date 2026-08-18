package org.verse.orgbridge.records.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OAuthCredRequest implements Serializable {


    private String name;
    
    private String orgType;

    private String username;

    private String password;

    private String securityToken;

}
