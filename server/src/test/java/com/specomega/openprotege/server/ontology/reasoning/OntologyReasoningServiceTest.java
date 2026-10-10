package com.specomega.openprotege.server.ontology.reasoning;

import com.specomega.openprotege.server.ontology.OntologyFormat;
import com.specomega.openprotege.server.ontology.OntologyParser;
import com.specomega.openprotege.server.ontology.OntologyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OntologyReasoningServiceTest {
    private static final String PREFIXES = """
            @prefix : <http://example.com/test#> .
            @prefix owl: <http://www.w3.org/2002/07/owl#> .
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            """;

    @Test
    void validatesProfileConsistencyAndUnsatisfiableClasses() {
        OntologyReasoningService service = service(PREFIXES + """
                :ontology a owl:Ontology .
                :A a owl:Class ; rdfs:subClassOf :B ; owl:disjointWith :B .
                :B a owl:Class .
                """);

        var result = service.validate(UUID.randomUUID(), UUID.randomUUID(), mock(Authentication.class));

        assertThat(result.inOwl2DlProfile()).isTrue();
        assertThat(result.consistent()).isTrue();
        assertThat(result.unsatisfiableClassIris()).contains("http://example.com/test#A");
        assertThat(result.classExplanations()).anySatisfy(explanation -> {
            assertThat(explanation.classIri()).isEqualTo("http://example.com/test#A");
            assertThat(explanation.axioms()).isNotEmpty();
        });
        assertThat(result.axiomCount()).isPositive();
        assertThat(result.elapsedMillis()).isPositive();
    }

    @Test
    void explainsAnInconsistentOntology() {
        OntologyReasoningService service = service(PREFIXES + """
                :ontology a owl:Ontology .
                :A a owl:Class ; owl:disjointWith :B .
                :B a owl:Class .
                :alice a owl:NamedIndividual, :A, :B .
                """);

        var result = service.validate(UUID.randomUUID(), UUID.randomUUID(), mock(Authentication.class));

        assertThat(result.consistent()).isFalse();
        assertThat(result.inconsistencyExplanationStatus()).isEqualTo("GENERATED");
        assertThat(result.inconsistencyExplanationAxioms()).hasSize(3)
                .anySatisfy(axiom -> assertThat(axiom)
                        .contains("http://example.com/test#A", "http://example.com/test#alice"))
                .anySatisfy(axiom -> assertThat(axiom)
                        .contains("http://example.com/test#B", "http://example.com/test#alice"))
                .anySatisfy(axiom -> assertThat(axiom)
                        .contains("http://example.com/test#A", "http://example.com/test#B"));
    }

    @Test
    void returnsInferredClassAncestorsAndDescendants() {
        OntologyReasoningService service = service(PREFIXES + """
                :ontology a owl:Ontology .
                :A a owl:Class ; rdfs:subClassOf :B .
                :B a owl:Class .
                :C a owl:Class ; rdfs:subClassOf :A .
                """);

        var result = service.hierarchy(UUID.randomUUID(), UUID.randomUUID(),
                "http://example.com/test#A", false, mock(Authentication.class));

        assertThat(result.consistent()).isTrue();
        assertThat(result.superClassIris()).contains("http://example.com/test#B");
        assertThat(result.subClassIris()).contains("http://example.com/test#C");
    }

    @Test
    void classifiesIndividualsAndAppliesTransientSwrlRules() {
        OntologyReasoningService service = service(PREFIXES + """
                :ontology a owl:Ontology .
                :Person a owl:Class .
                :Adult a owl:Class .
                :Student a owl:Class ; rdfs:subClassOf :Person .
                :alice a owl:NamedIndividual, :Student .
                """);
        UUID projectId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Authentication actor = mock(Authentication.class);
        String alice = "http://example.com/test#alice";

        var classification = service.classify(projectId, versionId, alice, actor);
        var ruleResult = service.applyRule(projectId, versionId,
                "Rule: <http://example.com/test#Student>(?x) -> <http://example.com/test#Adult>(?x)",
                alice, actor);

        assertThat(classification.allTypeIris()).contains("http://example.com/test#Person");
        assertThat(classification.inferredTypeIris()).contains("http://example.com/test#Person");
        assertThat(ruleResult.consistent()).isTrue();
        assertThat(ruleResult.inferredTypeIris()).contains("http://example.com/test#Adult");
    }

    @Test
    void reportsMissingClassesAndRejectsOntologiesOverTheAxiomLimit() {
        String turtle = PREFIXES + """
                :ontology a owl:Ontology .
                :A a owl:Class .
                """;
        OntologyReasoningService normalLimitService = service(turtle);
        OntologyReasoningService limitedService = service(turtle, 0);
        UUID projectId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Authentication actor = mock(Authentication.class);

        assertThatThrownBy(() -> normalLimitService.hierarchy(projectId, versionId,
                "http://example.com/test#Missing", false, actor))
                .isInstanceOf(ReasoningException.class)
                .satisfies(exception -> assertThat(((ReasoningException) exception).status().value()).isEqualTo(404));
        assertThatThrownBy(() -> limitedService.validate(projectId, versionId, actor))
                .isInstanceOf(ReasoningException.class)
                .satisfies(exception -> assertThat(((ReasoningException) exception).status().value()).isEqualTo(413));
    }

    @Test
    void rejectsUnavailableReasonerEngines() {
        OntologyReasoningService service = service(PREFIXES + """
                :ontology a owl:Ontology .
                """);

        assertThatThrownBy(() -> service.start(UUID.randomUUID(), UUID.randomUUID(),
                "PELLET", mock(Authentication.class)))
                .isInstanceOf(ReasoningException.class)
                .satisfies(exception -> assertThat(((ReasoningException) exception).errorCode())
                        .isEqualTo("REASONER_NOT_AVAILABLE"));
    }

    private static OntologyReasoningService service(String turtle) {
        return service(turtle, 10_000);
    }

    private static OntologyReasoningService service(String turtle, long maximumAxioms) {
        OntologyService ontologyService = mock(OntologyService.class);
        UUID versionId = UUID.randomUUID();
        when(ontologyService.getOntologyDocument(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OntologyService.OntologyDocument(
                        turtle.getBytes(java.nio.charset.StandardCharsets.UTF_8), OntologyFormat.TURTLE, versionId));
        return new OntologyReasoningService(ontologyService, new OntologyParser(),
                maximumAxioms, Duration.ofSeconds(30));
    }
}
