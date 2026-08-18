package org.verse.orgbridge.exceptions.parser;

import org.verse.orgbridge.exceptions.SalesforceAuthException;

public final class SalesforceSoapFaultParser {

    public static void throwIfFault(String xml) {
        if (!xml.contains("<soapenv:Fault>")) {
            return;
        }

        String faultCode = extract(xml, "<faultcode>", "</faultcode>");
        String faultMessage = extract(xml, "<faultstring>", "</faultstring>");

        if (faultCode != null && faultCode.contains(":")) {
            faultCode = faultCode.substring(faultCode.indexOf(':') + 1);
        }

        throw new SalesforceAuthException(
                faultCode != null ? faultCode : "AUTHENTICATION_FAILED",
                cleanMessage(faultMessage)
        );
    }

    private static String extract(String xml, String start, String end) {
        int s = xml.indexOf(start);
        int e = xml.indexOf(end);
        if (s == -1 || e == -1) return null;
        return xml.substring(s + start.length(), e).trim();
    }

    private static String cleanMessage(String msg) {
        if (msg == null) return "Authentication failed";
        if (msg.contains(":")) {
            return msg.substring(msg.indexOf(":") + 1).trim();
        }
        return msg;
    }
}
