package org.verse.orgbridge.xml;

import org.verse.orgbridge.records.auth.Credentials;
import org.verse.orgbridge.records.retrieve.RetrieveType;
import org.verse.orgbridge.records.retrieve.RetrieveXMLPayload;

import java.util.List;

public final class RetrieveRequestFactory {

    private static final String API_VERSION = "67.0";

    private RetrieveRequestFactory() {
    }

    public static String create(
            Credentials credentials,
            List<RetrieveType> types
    ) {
        return RetrieveRequestBuilder.build(
                RetrieveXMLPayload.builder()
                        .sessionId(credentials.sessionId())
                        .apiVersion(API_VERSION)
                        .types(types)
                        .singlePackage(true)
                        .build()
        );
    }
}
