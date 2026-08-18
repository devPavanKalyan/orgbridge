package org.verse.orgbridge.utils;

import reactor.core.publisher.Mono;

public final class AsyncResultParser {

    private AsyncResultParser() {
    }

    public static Mono<String> extractAsyncId(String xml) {
        int start = xml.indexOf("<id>") + 4;
        int end = xml.indexOf("</id>");

        if (start < 4 || end < 0) {
            return Mono.error(new IllegalStateException("Async process ID not found"));
        }
        return Mono.just(xml.substring(start, end).trim());
    }

    public static boolean isCompleted(String xml) {
        return xml.contains("<done>true</done>");
    }
}
