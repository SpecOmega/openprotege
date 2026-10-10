package com.specomega.openprotege.server.ontology.reasoning;

import com.specomega.openprotege.server.ontology.OntologyParser;
import com.specomega.openprotege.server.ontology.OntologyService;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class OntologyReasoningService {
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
                1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), task -> {
                    Thread thread = new Thread(task, "openprotege-ontology-reasoner");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public ValidationResult validate(UUID projectId, UUID versionId, Authentication actor) {
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> analyze(document));
    }

    public ClassHierarchy hierarchy(UUID projectId, UUID versionId, String classIri, boolean direct,
                                   Authentication actor) {
        OntologyService.OntologyDocument document = ontologyService.getOntologyDocument(projectId, versionId, actor);
        return execute(() -> classify(document, classIri, direct));
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
                return new ValidationResult(document.versionId(), false, violations,
                        null, List.of(), axiomCount, elapsedMillis(startedAt));
            }

            try (ReasonerHandle handle = new ReasonerHandle(createReasoner(ontology))) {
                OWLReasoner reasoner = handle.reasoner();
                reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
                List<String> unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntities().stream()
                        .filter(cls -> !cls.isOWLNothing())
                        .map(cls -> cls.getIRI().toString())
                        .sorted()
                        .limit(1000)
                        .toList();
                return new ValidationResult(document.versionId(), true, violations, reasoner.isConsistent(),
                        unsatisfiableClasses, axiomCount, elapsedMillis(startedAt));
            }
        } catch (ReasoningException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ReasoningException.failed(exception);
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
        Future<T> task;
        try {
            task = reasoningExecutor.submit(action);
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            throw ReasoningException.busy();
        }
        try {
            return task.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException exception) {
            task.cancel(true);
            throw ReasoningException.timedOut(timeout.toSeconds());
        } catch (InterruptedException exception) {
            task.cancel(true);
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

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record ReasonerHandle(OWLReasoner reasoner) implements AutoCloseable {
        @Override
        public void close() {
            reasoner.dispose();
        }
    }

    public record ValidationResult(UUID versionId, boolean inOwl2DlProfile, List<String> profileViolations,
                                   Boolean consistent, List<String> unsatisfiableClassIris, long axiomCount,
                                   long elapsedMillis) {}

    public record ClassHierarchy(UUID versionId, String classIri, boolean consistent, boolean direct,
                                 List<String> superClassIris, List<String> subClassIris, long elapsedMillis) {}
}
