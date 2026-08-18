package org.verse.orgbridge.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class XmlElements {

    private XmlElements() {
    }

    public static Stream<Element> of(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        List<Element> elements = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements.stream();
    }
}
