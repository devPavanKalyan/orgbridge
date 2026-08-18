package org.verse.orgbridge.csv;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.operation.contract.CsvFieldDefinition;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class CsvMetadataZipBuilder {

    private final VaultService vaultService;

    public Mono<String> build(List<CsvFieldDefinition> fields) {
        return Mono.fromCallable(() ->
                        Base64.getEncoder().encodeToString(buildZip(fields))
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] buildZip(List<CsvFieldDefinition> fields)
            throws Exception {
        Map<String, List<CsvFieldDefinition>> byObject =
                fields.stream().collect(
                        java.util.stream.Collectors.groupingBy(
                                CsvFieldDefinition::sobject,
                                TreeMap::new,
                                java.util.stream.Collectors.toList()
                        )
                );

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {

            for (Map.Entry<String, List<CsvFieldDefinition>> entry
                    : byObject.entrySet()) {
                addEntry(
                        zip,
                        "objects/" + entry.getKey() + ".object",
                        objectXml(entry.getValue())
                );
            }

            addEntry(zip, "package.xml", packageXml(byObject.keySet()));
            zip.finish();
            return output.toByteArray();
        }
    }

    private void addEntry(
            ZipOutputStream zip,
            String path,
            String content
    ) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String objectXml(List<CsvFieldDefinition> fields) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <CustomObject xmlns="http://soap.sforce.com/2006/04/metadata">
                """);

        fields.forEach(field -> xml.append(fieldXml(field)));
        xml.append("</CustomObject>");
        return xml.toString();
    }

    private String fieldXml(CsvFieldDefinition field) {
        String type = normalizeType(field.type());
        StringBuilder xml = new StringBuilder()
                .append("  <fields>\n")
                .append("    <fullName>")
                .append(xml(field.fieldName()))
                .append("</fullName>\n")
                .append("    <label>")
                .append(xml(field.label()))
                .append("</label>\n")
                .append("    <type>")
                .append(xml(type))
                .append("</type>\n");

        if ("Text".equals(type)) {
            xml.append("    <length>")
                    .append(field.length())
                    .append("</length>\n");
        }

        xml.append("    <required>")
                .append(Boolean.TRUE.equals(field.required()))
                .append("</required>\n");

        if ("Picklist".equals(type)) {
            xml.append("    <valueSet>\n")
                    .append("      <restricted>false</restricted>\n")
                    .append("      <valueSetDefinition>\n");

            for (String value : field.values().split(";")) {
                if (!value.isBlank()) {
                    xml.append("        <value>\n")
                            .append("          <fullName>")
                            .append(xml(value.trim()))
                            .append("</fullName>\n")
                            .append("          <default>false</default>\n")
                            .append("          <label>")
                            .append(xml(value.trim()))
                            .append("</label>\n")
                            .append("        </value>\n");
                }
            }

            xml.append("      </valueSetDefinition>\n")
                    .append("    </valueSet>\n");
        }

        return xml.append("  </fields>\n").toString();
    }

    private String packageXml(java.util.Set<String> objects) {
        StringBuilder members = new StringBuilder();
        objects.forEach(object -> members
                .append("    <members>")
                .append(xml(object))
                .append("</members>\n"));

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Package xmlns="http://soap.sforce.com/2006/04/metadata">
                  <types>
                %s    <name>CustomObject</name>
                  </types>
                  <version>%s</version>
                </Package>
                """.formatted(
                members,
                vaultService.salesforceApiVersion()
        );
    }

    private String normalizeType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "textarea" -> "TextArea";
            case "datetime" -> "DateTime";
            case "url" -> "Url";
            default -> Character.toUpperCase(value.charAt(0))
                    + value.substring(1).toLowerCase(Locale.ROOT);
        };
    }

    private String xml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
