package com.specomega.openprotege.server.ontology;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OntologyParserTest {
    private final OntologyParser parser = new OntologyParser();

    @Test
    void detectsTurtleWithoutLoadingRemoteImports() throws Exception {
        var file = Files.createTempFile("ontology-import-test-", ".ttl");
        try {
            Files.writeString(file, """
                    @prefix owl: <http://www.w3.org/2002/07/owl#> .
                    <http://example.com/test> a owl:Ontology ;
                        owl:imports <http://127.0.0.1:1/should-not-be-requested> .
                    <http://example.com/test#Thing> a owl:Class .
                    """);

            OntologyParser.ParsedOntology parsed = parser.parse(file, "imports.ttl", null);

            assertThat(parsed.format()).isEqualTo(OntologyFormat.TURTLE);
            assertThat(parsed.ontologyIri()).isEqualTo("http://example.com/test");
            assertThat(parsed.axiomCount()).isPositive();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsUnsupportedExtensionsAndMalformedDocuments() throws Exception {
        var file = Files.createTempFile("ontology-invalid-test-", ".jsonld");
        try {
            Files.writeString(file, "{}");

            assertThatThrownBy(() -> parser.parse(file, "ontology.jsonld", null))
                    .isInstanceOf(OntologyException.class)
                    .extracting(exception -> ((OntologyException) exception).errorCode())
                    .isEqualTo("FORMAT_NOT_SUPPORTED");
            assertThatThrownBy(() -> parser.parse(file, "ontology.owl", "RDF/XML"))
                    .isInstanceOf(OntologyException.class)
                    .extracting(exception -> ((OntologyException) exception).errorCode())
                    .isEqualTo("PARSING_ERROR");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
