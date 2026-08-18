package org.verse.orgbridge.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class RetrieveZipExtractor {

    private RetrieveZipExtractor() {
    }

    public static Mono<String> extractZipFile(String xml) {
        return Mono.fromCallable(() -> {
                    Document doc = XmlParser.parse(xml);
                    NodeList zipNodes = doc.getElementsByTagNameNS("*", "zipFile");

                    if (zipNodes.getLength() == 0) {
                        throw new IllegalStateException("zipFile not found in retrieve response");
                    }

                    return zipNodes.item(0).getTextContent().trim();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

//    public static byte[] extract(String xml) {
//
//        int start = xml.indexOf("<zipFile>") + 9;
//        int end = xml.indexOf("</zipFile>");
//
//        if (start < 9 || end < 0) {
//            throw new IllegalStateException("zipFile not found");
//        }
//
//        String base64 = xml.substring(start, end).trim();
//        return Base64.getDecoder().decode(base64);
//    }
}
