package org.verse.orgbridge.xml;

import org.verse.orgbridge.records.deploy.DeployResult;
import org.w3c.dom.Element;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public final class DeployResponseParser {

    private DeployResponseParser() {
    }

    public static Mono<DeployResult> parse(String xml) {
        return Mono.fromCallable(() -> {

            var doc = XmlParser.parse(xml);
            Element result = (Element) doc
                    .getElementsByTagNameNS("*", "result")
                    .item(0);

            var summary = DeployResult.Summary.builder()
                    .totalComponents(intVal(result, "numberComponentsTotal"))
                    .deployed(intVal(result, "numberComponentsDeployed"))
                    .errors(intVal(result, "numberComponentErrors"))
                    .testsRun(intVal(result, "numberTestsCompleted"))
                    .testErrors(intVal(result, "numberTestErrors"))
                    .build();

            List<DeployResult.ComponentFailure> failures =
                    XmlElements.of(result, "componentFailures")
                            .map(e -> DeployResult.ComponentFailure.builder()
                                    .fullName(XmlParser.text(e, "fullName"))
                                    .type(defaultType(XmlParser.text(e, "componentType")))
                                    .fileName(XmlParser.text(e, "fileName"))
                                    .problemType(XmlParser.text(e, "problemType"))
                                    .message(XmlParser.text(e, "problem"))
                                    .build())
                            .toList();

            List<DeployResult.ComponentSuccess> successes =
                    XmlElements.of(result, "componentSuccesses")
                            .map(e -> DeployResult.ComponentSuccess.builder()
                                    .fullName(XmlParser.text(e, "fullName"))
                                    .type(defaultType(XmlParser.text(e, "componentType")))
                                    .fileName(XmlParser.text(e, "fileName"))
                                    .id(XmlParser.text(e, "id"))
                                    .build())
                            .toList();

            return DeployResult.builder()
                    .deploymentId(XmlParser.text(result, "id"))
                    .status(XmlParser.text(result, "status"))
                    .success(Boolean.parseBoolean(XmlParser.text(result, "success")))
                    .done(Boolean.parseBoolean(XmlParser.text(result, "done")))
                    .summary(summary)
                    .failures(failures)
                    .successes(successes)
                    .build();

        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static int intVal(Element e, String tag) {
        var v = XmlParser.text(e, tag);
        return v == null ? 0 : Integer.parseInt(v);
    }

    private static String defaultType(String t) {
        return (t == null || t.isBlank()) ? "Package" : t;
    }
}
