package org.verse.orgbridge.xml;

import org.verse.orgbridge.records.types.ComponentItem;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MetadataResponseParser {

    private MetadataResponseParser() {
    }

    public static List<String> parseTypes(String xml) {
        var doc = XmlParser.parse(xml);
        var nodes = doc.getElementsByTagNameNS("*", "metadataObjects");

        List<String> result = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);

            var parent = XmlParser.text(el, "xmlName");
            if (parent != null) result.add(parent);

            var children = el.getElementsByTagNameNS("*", "childXmlNames");
            for (int j = 0; j < children.getLength(); j++) {
                var c = children.item(j).getTextContent();
                if (!c.isBlank()) result.add(c);
            }
        }
        return result;
    }

    public static List<ComponentItem> parseComponents(String xml) {
        var doc = XmlParser.parse(xml);
        var results = doc.getElementsByTagNameNS("*", "result");

        List<ComponentItem> items = new ArrayList<>();

        for (int i = 0; i < results.getLength(); i++) {
            Element el = (Element) results.item(i);

            String full = XmlParser.text(el, "fullName");
            String type = XmlParser.text(el, "type");
            String manageableState = XmlParser.text(el, "manageableState");

            if (!"unmanaged".equalsIgnoreCase(manageableState)) {
                continue;
            }

            String parent = null;
            String name = full;

            if ("CustomField".equals(type) && full != null && full.contains(".")) {
                var p = full.split("\\.", 2);
                parent = p[0];
                name = p[1];
            }

            items.add(ComponentItem.builder()
                    .id(UUID.randomUUID().toString())
                    .name(name)
                    .type(type)
                    .parent(parent)
                    .modifiedBy(XmlParser.text(el, "createdByName"))
                    .modifiedDate(XmlParser.text(el, "lastModifiedDate"))
                    .manageableState(XmlParser.text(el, "manageableState"))
                    .selected(false)
                    .build());
        }
        return items;
    }
}
