package org.verse.orgbridge.xml;

import org.verse.orgbridge.records.session.SalesforceSession;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

public final class LoginRequestBuilder {

    private static final String SOAP_ENV =
            "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String PARTNER_NS =
            "urn:partner.soap.sforce.com";

    private LoginRequestBuilder() {
    }


    public static String builder(String username, String password, String token) {
        return """
                <?xml version="1.0" encoding="utf-8" ?>
                <env:Envelope xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:env="http://schemas.xmlsoap.org/soap/envelope/">
                  <env:Body>
                    <n1:login xmlns:n1="urn:partner.soap.sforce.com">
                      <n1:username>%s</n1:username>
                      <n1:password>%s</n1:password>
                    </n1:login>
                  </env:Body>
                </env:Envelope>
                """.formatted(username, password + token);
    }

    public static String build(
            String username,
            String password,
            String securityToken
    ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element envelope = doc.createElementNS(SOAP_ENV, "env:Envelope");
            envelope.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            envelope.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(envelope);

            Element body = doc.createElementNS(SOAP_ENV, "env:Body");
            envelope.appendChild(body);

            Element login = doc.createElementNS(PARTNER_NS, "n1:login");
            body.appendChild(login);

            Element user = doc.createElementNS(PARTNER_NS, "n1:username");
            user.setTextContent(username);
            login.appendChild(user);

            Element pass = doc.createElementNS(PARTNER_NS, "n1:password");
            pass.setTextContent(password + securityToken);
            login.appendChild(pass);

            return toPrettyString(doc);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SOAP login request", e);
        }
    }

    private static String toPrettyString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8"); // lowercase
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2"
        );

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString()
                .replace(" standalone=\"no\"", "");
    }


    public static SalesforceSession parse(String xml) {

        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            XPath xpath = XPathFactory.newInstance().newXPath();

            String sessionId = (String) xpath.evaluate("//*[local-name()='sessionId']", doc, XPathConstants.STRING);
            String serverUrl = (String) xpath.evaluate("//*[local-name()='serverUrl']", doc, XPathConstants.STRING);
            String metadataServerUrl = (String) xpath.evaluate("//*[local-name()='metadataServerUrl']", doc, XPathConstants.STRING);
            String userId = (String) xpath.evaluate("//*[local-name()='userId'][1]", doc, XPathConstants.STRING);
            String organizationId = (String) xpath.evaluate("//*[local-name()='organizationId']", doc, XPathConstants.STRING);
            String userFullName = (String) xpath.evaluate("//*[local-name()='userFullName']", doc, XPathConstants.STRING);
            String userEmail = (String) xpath.evaluate("//*[local-name()='userEmail']", doc, XPathConstants.STRING);
            String organizationName = (String) xpath.evaluate("//*[local-name()='organizationName']", doc, XPathConstants.STRING);
            String sessionSecondsValidStr = (String) xpath.evaluate("//*[local-name()='sessionSecondsValid']", doc, XPathConstants.STRING);

            boolean sandbox = Boolean.parseBoolean(
                    (String) xpath.evaluate("//*[local-name()='sandbox']", doc, XPathConstants.STRING));

            boolean passwordExpired = Boolean.parseBoolean(
                    (String) xpath.evaluate("//*[local-name()='passwordExpired']", doc, XPathConstants.STRING));

            int sessionSecondsValid = Integer.parseInt(sessionSecondsValidStr);

            return SalesforceSession.builder()
                    .userId(userId)
                    .organizationId(organizationId)
                    .sessionId(sessionId)
                    .serverUrl(serverUrl)
                    .metadataServerUrl(metadataServerUrl)
                    .sandbox(sandbox)
                    .passwordExpired(passwordExpired)
                    .userFullName(userFullName)
                    .userEmail(userEmail)
                    .organizationName(organizationName)
                    .sessionSecondsValid(sessionSecondsValid)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Salesforce login response", e);
        }
    }
}
