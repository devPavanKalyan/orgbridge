package org.verse.orgbridge.xml;

public final class SoapEnvelopeBuilder {

    private static final String SOAP_ENV =
            "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String METADATA_NS =
            "http://soap.sforce.com/2006/04/metadata";

    private SoapEnvelopeBuilder() {
    }

    public static String wrap(String sessionId, String body) {
        return """
                <soapenv:Envelope xmlns:soapenv="%s"
                                  xmlns:met="%s">
                    <soapenv:Header>
                        <met:SessionHeader>
                            <met:sessionId>%s</met:sessionId>
                        </met:SessionHeader>
                    </soapenv:Header>
                    <soapenv:Body>
                        %s
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                SOAP_ENV,
                METADATA_NS,
                XmlEscaper.escape(sessionId),
                body
        );
    }
}
