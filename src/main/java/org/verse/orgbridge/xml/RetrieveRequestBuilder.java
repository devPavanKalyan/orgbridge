package org.verse.orgbridge.xml;

import org.verse.orgbridge.records.retrieve.RetrieveType;
import org.verse.orgbridge.records.retrieve.RetrieveXMLPayload;

public final class RetrieveRequestBuilder {

    private RetrieveRequestBuilder() {
    }

    public static String build(RetrieveXMLPayload payload) {

        StringBuilder types = new StringBuilder();

        for (RetrieveType type : payload.types()) {
            types.append("<met:types>");
            for (String m : type.getMembers()) {
                types.append("<met:members>")
                        .append(XmlEscaper.escape(m))
                        .append("</met:members>");
            }
            types.append("<met:name>")
                    .append(XmlEscaper.escape(type.getName()))
                    .append("</met:name>")
                    .append("</met:types>");
        }

        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                    <soapenv:Header>
                        <met:SessionHeader>
                            <met:sessionId>%s</met:sessionId>
                        </met:SessionHeader>
                    </soapenv:Header>
                    <soapenv:Body>
                        <met:retrieve>
                            <met:retrieveRequest>
                                <met:apiVersion>%s</met:apiVersion>
                                <met:singlePackage>%s</met:singlePackage>
                                <met:unpackaged>
                                    %s
                                    <met:version>%s</met:version>
                                </met:unpackaged>
                            </met:retrieveRequest>
                        </met:retrieve>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                XmlEscaper.escape(payload.sessionId()),
                XmlEscaper.escape(payload.apiVersion()),
                payload.singlePackage(),
                types,
                XmlEscaper.escape(payload.apiVersion())
        );
    }
}
