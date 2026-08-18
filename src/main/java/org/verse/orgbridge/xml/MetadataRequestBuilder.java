package org.verse.orgbridge.xml;

public final class MetadataRequestBuilder {

    private static final String API_VERSION = "67.0";

    private MetadataRequestBuilder() {
    }

    public static String describe(String sessionId) {
        return SoapEnvelopeBuilder.wrap(sessionId, """
                <met:describeMetadata>
                    <met:asOfVersion>%s</met:asOfVersion>
                </met:describeMetadata>
                """.formatted(API_VERSION));
    }

    public static String list(String sessionId, String type) {
        return SoapEnvelopeBuilder.wrap(sessionId, """
                <met:listMetadata>
                    <met:queries>
                        <met:type>%s</met:type>
                    </met:queries>
                    <met:asOfVersion>%s</met:asOfVersion>
                </met:listMetadata>
                """.formatted(XmlEscaper.escape(type), API_VERSION));
    }

    public static String checkRetrieve(String sessionId, String id) {
        return SoapEnvelopeBuilder.wrap(sessionId, """
                <met:checkRetrieveStatus>
                    <met:asyncProcessId>%s</met:asyncProcessId>
                </met:checkRetrieveStatus>
                """.formatted(XmlEscaper.escape(id)));
    }

    public static String deploy(String sessionId, String zip, boolean checkOnly) {
        return SoapEnvelopeBuilder.wrap(sessionId, """
                <met:deploy>
                    <met:ZipFile>%s</met:ZipFile>
                    <met:DeployOptions>
                        <met:checkOnly>%s</met:checkOnly>
                        <met:rollbackOnError>true</met:rollbackOnError>
                        <met:singlePackage>true</met:singlePackage>
                    </met:DeployOptions>
                </met:deploy>
                """.formatted(XmlEscaper.escape(zip), checkOnly));
    }

    public static String checkDeploy(String sessionId, String id) {
        return SoapEnvelopeBuilder.wrap(sessionId, """
                <met:checkDeployStatus>
                    <met:asyncProcessId>%s</met:asyncProcessId>
                    <met:includeDetails>true</met:includeDetails>
                </met:checkDeployStatus>
                """.formatted(XmlEscaper.escape(id)));
    }
}
