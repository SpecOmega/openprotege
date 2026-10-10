package com.specomega.openprotege.server.ontology.reasoning;

import com.specomega.openprotege.server.ontology.OntologyParser;
import com.specomega.openprotege.server.ontology.OntologyService;
import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.OntologyAxiomPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class OntologyReasoningService {
    private static final int MAX_AXIOMS_FOR_GLOBAL_EXPLANATION = 2_000;
    private final OntologyService ontologyService;
    private final OntologyParser ontologyParser;
    private final ThreadPoolExecutor reasoningExecutor;
    private final long maximumAxioms;
    private final Duration timeout;

    public OntologyReasoningService(OntologyService ontologyService,
                                    OntologyParser ontologyParser,
                                    @Value("${openprotege.reasoning.max-axioms:100000}") long maximumAxioms,
                                    @Value("${openprotege.reasoning.timeout:60s}") Duration timeout) {
        this.ontologyService = ontologyService;
        this.ontologyParser = ontologyParser;
        this.maximumAxioms = maximumAxioms;
        this.timeout = timeout;
        this.reasoningExecutor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), task -> {
                    Thread thread = new Thread(task, "openprotege-ontology-reasoner");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public ValidationResult validate(UUID projectId, UUID versionId, Authentication actor) {
        return start(projectId, versionId, "HERMIT", actor);
    }

    public ValidationResult start(UUID projectId, UUID versionId, String engine, Authentication actor) {
        if (!"HERMIT".equalsIgnoreCase(engine)) {
            throw ReasoningException.unsupportedEngine(engine);
        }
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> analyze(document));
    }

    public ClassHierarchy hierarchy(UUID projectId, UUID versionId, String classIri, boolean direct,
                                   Authentication actor) {
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> classify(document, classIri, direct));
    }

    public IndividualClassification classify(UUID projectId, UUID versionId, String individualIri,
                                             Authentication actor) {
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> classifyIndividual(document, individualIri));
    }

    public RuleApplication applyRule(UUID projectId, UUID versionId, String ruleText, String individualIri,
                                     Authentication actor) {
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> applyRule(document, ruleText, individualIri));
    }

    private ValidationResult analyze(OntologyService.OntologyDocument document) {
        long startedAt = System.nanoTime();
        try (OntologyParser.LoadedOntology loaded = ontologyParser.load(document.content(), document.format())) {
            var ontology = loaded.ontology();
            long axiomCount = ontology.getAxiomCount();
            if (axiomCount > maximumAxioms) {
                throw ReasoningException.tooLarge(axiomCount, maximumAxioms);
            }

            var profileReport = new OWL2DLProfile().checkOntology(ontology);
            List<String> violations = profileReport.getViolations().stream()
                    .limit(100)
                    .map(violation -> violation.toString())
                    .toList();
            if (!profileReport.isInProfile()) {
                return new ValidationResult(document.versionId(), "HERMIT", false, violations,
                        null, List.of(), List.of(), "NOT_APPLICABLE", List.of(),
                        axiomCount, elapsedMillis(startedAt));
            }

            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                OWLReasoner reasoner = handle.reasoner();
                boolean consistent = reasoner.isConsistent();
                if (consistent) {
                    reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
                }
                List<String> unsatisfiableClasses = consistent
                        ? reasoner.getUnsatisfiableClasses().getEntities().stream()
                                .filter(cls -> !cls.isOWLNothing())
                                .map(cls -> cls.getIRI().toString())
                                .sorted()
                                .limit(1000)
                                .toList()
                        : List.of();
                List<ClassExplanation> classExplanations = List.of();
                List<String> inconsistencyExplanationAxioms = List.of();
                String inconsistencyExplanationStatus = "NOT_APPLICABLE";
                if (consistent) {
                    DefaultExplanationGenerator explanations = new DefaultExplanationGenerator(
                            loaded.manager(), new ReasonerFactory(), ontology, reasoner, null);
                    classExplanations = unsatisfiableClasses.stream()
                            .limit(20)
                            .map(classIri -> {
                                OWLClass unsatisfiableClass = loaded.manager().getOWLDataFactory()
                                        .getOWLClass(IRI.create(classIri));
                                return new ClassExplanation(classIri,
                                        stringify(explanations.getExplanation(unsatisfiableClass)));
                            })
                            .toList();
                } else {
                    List<OWLAxiom> logicalAxioms = ontology.getLogicalAxioms(Imports.EXCLUDED).stream()
                            .map(axiom -> (OWLAxiom) axiom)
                            .sorted(java.util.Comparator.comparing(Object::toString))
                            .toList();
                    if (logicalAxioms.size() <= MAX_AXIOMS_FOR_GLOBAL_EXPLANATION) {
                        inconsistencyExplanationAxioms = quickXplain(Set.of(), logicalAxioms).stream()
                                .map(Object::toString)
                                .toList();
                        inconsistencyExplanationStatus = "GENERATED";
                    } else {
                        inconsistencyExplanationStatus = "AXIOM_LIMIT_EXCEEDED";
                    }
                }
                return new ValidationResult(document.versionId(), "HERMIT", true, violations, consistent,
                        unsatisfiableClasses, classExplanations, inconsistencyExplanationStatus,
                        inconsistencyExplanationAxioms,
                        axiomCount, elapsedMillis(startedAt));
            }
        } catch (ReasoningException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ReasoningException.failed(exception);
        }
    }

    private IndividualClassification classifyIndividual(OntologyService.OntologyDocument document,
                                                        String individualIri) {
        long startedAt = System.nanoTime();
        try (OntologyParser.LoadedOntology loaded = ontologyParser.load(document.content(), document.format())) {
            var ontology = loaded.ontology();
            checkReasoningLimits(ontology.getAxiomCount());
            requireOwl2DlProfile(ontology);
            IRI iri = parseIri(individualIri);
            if (!ontology.containsIndividualInSignature(iri)) {
                throw ReasoningException.individualNotFound(individualIri);
            }
            OWLNamedIndividual individual = loaded.manager().getOWLDataFactory().getOWLNamedIndividual(iri);
            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                OWLReasoner reasoner = handle.reasoner();
                if (!reasoner.isConsistent()) {
                    return new IndividualClassification(document.versionId(), individualIri, false,
                            List.of(), List.of(), elapsedMillis(startedAt));
                }
                reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
                Set<OWLClass> allTypes = reasoner.getTypes(individual, false).getFlattened();
                Set<OWLClass> assertedTypes = ontology.getClassAssertionAxioms(individual).stream()
                        .map(OWLClassAssertionAxiom::getClassExpression)
                        .filter(expression -> !expression.isAnonymous())
                        .map(expression -> expression.asOWLClass())
                        .collect(java.util.stream.Collectors.toSet());
                List<String> inferredTypes = allTypes.stream()
                        .filter(type -> !assertedTypes.contains(type) && !type.isOWLThing())
                        .map(type -> type.getIRI().toString())
                        .sorted()
                        .toList();
                return new IndividualClassification(document.versionId(), individualIri, true,
                        flatten(allTypes), inferredTypes, elapsedMillis(startedAt));
            }
        } catch (ReasoningException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ReasoningException.failed(exception);
        }
    }

    private RuleApplication applyRule(OntologyService.OntologyDocument document, String ruleText,
                                      String individualIri) {
        long startedAt = System.nanoTime();
        try (OntologyParser.LoadedOntology loaded = ontologyParser.load(document.content(), document.format())) {
            var ontology = loaded.ontology();
            checkReasoningLimits(ontology.getAxiomCount());
            requireOwl2DlProfile(ontology);
            IRI iri = parseIri(individualIri);
            if (!ontology.containsIndividualInSignature(iri)) {
                throw ReasoningException.individualNotFound(individualIri);
            }
            var parser = OWLManager.createManchesterParser();
            parser.setDefaultOntology(ontology);
            parser.setStringToParse(ruleText);
            Collection<OntologyAxiomPair> parsedRules;
            try {
                parsedRules = parser.parseRuleFrame();
            } catch (RuntimeException exception) {
                throw ReasoningException.invalidRule();
            }
            List<SWRLRule> rules = parsedRules.stream()
                    .map(OntologyAxiomPair::getAxiom)
                    .filter(SWRLRule.class::isInstance)
                    .map(SWRLRule.class::cast)
                    .toList();
            if (rules.size() != 1) {
                throw ReasoningException.invalidRule();
            }
            loaded.manager().addAxioms(ontology, Set.copyOf(rules));
            OWLNamedIndividual individual = loaded.manager().getOWLDataFactory().getOWLNamedIndividual(iri);
            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                OWLReasoner reasoner = handle.reasoner();
                if (!reasoner.isConsistent()) {
                    return new RuleApplication(document.versionId(), individualIri, false,
                            List.of(), elapsedMillis(startedAt));
                }
                reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
                List<String> inferredTypes = reasoner.getTypes(individual, false).getFlattened().stream()
                        .filter(type -> !type.isOWLThing())
                        .map(type -> type.getIRI().toString())
                        .sorted()
                        .toList();
                return new RuleApplication(document.versionId(), individualIri, true,
                        inferredTypes, elapsedMillis(startedAt));
            }
        } catch (ReasoningException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ReasoningException.failed(exception);
        }
    }

    private void checkReasoningLimits(long axiomCount) {
        if (axiomCount > maximumAxioms) {
            throw ReasoningException.tooLarge(axiomCount, maximumAxioms);
        }
    }

    private Set<OWLAxiom> quickXplain(Set<OWLAxiom> background, List<OWLAxiom> candidates) {
        if (candidates.isEmpty() || !isConsistent(background)) {
            return Set.of();
        }
        if (candidates.size() == 1) {
            Set<OWLAxiom> trial = new java.util.HashSet<>(background);
            trial.add(candidates.getFirst());
            return isConsistent(trial) ? Set.of() : Set.of(candidates.getFirst());
        }

        int midpoint = candidates.size() / 2;
        List<OWLAxiom> first = candidates.subList(0, midpoint);
        List<OWLAxiom> second = candidates.subList(midpoint, candidates.size());
        Set<OWLAxiom> firstBackground = new java.util.HashSet<>(background);
        firstBackground.addAll(second);
        Set<OWLAxiom> firstConflict = quickXplain(firstBackground, first);

        Set<OWLAxiom> secondBackground = new java.util.HashSet<>(background);
        secondBackground.addAll(firstConflict);
        Set<OWLAxiom> secondConflict = quickXplain(secondBackground, second);

        Set<OWLAxiom> conflict = new java.util.HashSet<>(firstConflict);
        conflict.addAll(secondConflict);
        return Set.copyOf(conflict);
    }

    private boolean isConsistent(Set<OWLAxiom> axioms) {
        var manager = OWLManager.createOWLOntologyManager();
        org.semanticweb.owlapi.model.OWLOntology ontology = null;
        try {
            ontology = manager.createOntology(axioms);
            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                return handle.reasoner().isConsistent();
            }
        } catch (org.semanticweb.owlapi.model.OWLOntologyCreationException exception) {
            throw ReasoningException.failed(exception);
        } finally {
            if (ontology != null) {
                manager.removeOntology(ontology);
            }
        }
    }

    private static void requireOwl2DlProfile(org.semanticweb.owlapi.model.OWLOntology ontology) {
        var profile = new OWL2DLProfile().checkOntology(ontology);
        if (!profile.isInProfile()) {
            throw ReasoningException.profileRejected(profile.getViolations().size());
        }
    }

    private static IRI parseIri(String value) {
        try {
            return IRI.create(value);
        } catch (IllegalArgumentException exception) {
            throw ReasoningException.invalidClassIri();
        }
    }

    private ClassHierarchy classify(OntologyService.OntologyDocument document, String classIri, boolean direct) {
        long startedAt = System.nanoTime();
        try (OntologyParser.LoadedOntology loaded = ontologyParser.load(document.content(), document.format())) {
            var ontology = loaded.ontology();
            long axiomCount = ontology.getAxiomCount();
            if (axiomCount > maximumAxioms) {
                throw ReasoningException.tooLarge(axiomCount, maximumAxioms);
            }
            var profile = new OWL2DLProfile().checkOntology(ontology);
            if (!profile.isInProfile()) {
                throw ReasoningException.profileRejected(profile.getViolations().size());
            }
            IRI iri;
            try {
                iri = IRI.create(classIri);
            } catch (IllegalArgumentException exception) {
                throw ReasoningException.invalidClassIri();
            }
            OWLClass owlClass = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
            if (!ontology.containsClassInSignature(iri)) {
                throw ReasoningException.classNotFound(classIri);
            }
            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                OWLReasoner reasoner = handle.reasoner();
                if (!reasoner.isConsistent()) {
                    return new ClassHierarchy(document.versionId(), classIri, false, direct,
                            List.of(), List.of(), elapsedMillis(startedAt));
                }
                reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
                List<String> superClasses = flatten(reasoner.getSuperClasses(owlClass, direct).getFlattened());
                List<String> subClasses = flatten(reasoner.getSubClasses(owlClass, direct).getFlattened());
                return new ClassHierarchy(document.versionId(), classIri, true, direct,
                        superClasses, subClasses, elapsedMillis(startedAt));
            }
        } catch (ReasoningException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ReasoningException.failed(exception);
        }
    }

    private OWLReasoner createReasoner(org.semanticweb.owlapi.model.OWLOntology ontology) {
        SimpleConfiguration configuration = new SimpleConfiguration((ReasonerProgressMonitor) null, timeout.toMillis());
        return new ReasonerFactory().createReasoner(ontology, configuration);
    }

    private <T> T execute(java.util.concurrent.Callable<T> action) {
        FutureTask<T> task = new FutureTask<>(action);
        try {
            reasoningExecutor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            throw ReasoningException.busy();
        }
        try {
            return task.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException exception) {
            task.cancel(true);
            reasoningExecutor.remove(task);
            throw ReasoningException.timedOut(timeout.toSeconds());
        } catch (InterruptedException exception) {
            task.cancel(true);
            reasoningExecutor.remove(task);
            Thread.currentThread().interrupt();
            throw ReasoningException.timedOut(timeout.toSeconds());
        } catch (java.util.concurrent.ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReasoningException reasoningException) {
                throw reasoningException;
            }
            throw ReasoningException.failed(cause);
        }
    }

    private static List<String> flatten(java.util.Set<OWLClass> classes) {
        List<String> iris = new ArrayList<>(classes.size());
        classes.stream().map(cls -> cls.getIRI().toString()).sorted().forEach(iris::add);
        return List.copyOf(iris);
    }

    private static List<String> stringify(Collection<? extends OWLAxiom> axioms) {
        return axioms.stream().limit(50).map(Object::toString).toList();
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record ReasonerHandle(OWLReasoner reasoner) implements AutoCloseable {
        @Override
        public void close() {
            reasoner.dispose();
        }
    }

    public record ValidationResult(UUID versionId, String engine, boolean inOwl2DlProfile,
                                   List<String> profileViolations, Boolean consistent,
                                   List<String> unsatisfiableClassIris, List<ClassExplanation> classExplanations,
                                   String inconsistencyExplanationStatus, List<String> inconsistencyExplanationAxioms,
                                   long axiomCount, long elapsedMillis) {}

    public record ClassHierarchy(UUID versionId, String classIri, boolean consistent, boolean direct,
                                 List<String> superClassIris, List<String> subClassIris, long elapsedMillis) {}

    public record IndividualClassification(UUID versionId, String individualIri, boolean consistent,
                                           List<String> allTypeIris, List<String> inferredTypeIris,
                                           long elapsedMillis) {}

    public record RuleApplication(UUID versionId, String individualIri, boolean consistent,
                                  List<String> inferredTypeIris, long elapsedMillis) {}

    public record ClassExplanation(String classIri, List<String> axioms) {}
}
