package org.verse.orgbridge.records.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceSession implements Serializable {

    private String userId;
    private String organizationId;

    private String sessionId;
    private String serverUrl;
    private String metadataServerUrl;

    private boolean sandbox;
    private boolean passwordExpired;

    private String userFullName;
    private String userEmail;
    private String organizationName;

    private int sessionSecondsValid;

    public boolean isAuthenticated() {
        return sessionId != null && !sessionId.isBlank()
                && serverUrl != null && !serverUrl.isBlank()
                && userId != null && !userId.isBlank()
                && organizationId != null && !organizationId.isBlank();
    }
}
