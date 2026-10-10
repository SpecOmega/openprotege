package com.specomega.openprotege.server.ontology;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

@Component
final class OntologyParser {
    ParsedOntology parse(Path file, String originalFileName, String requestedFormat) {
        OntologyFormat format = OntologyFormat.parse(requestedFormat);
        if (format == null && originalFileName != null && originalFileName.lastIndexOf('.') > 0) {
            format = OntologyFormat.fromFileName(originalFileName);
            if (format == null) {
                throw OntologyException.unsupportedFormat(originalFileName.substring(originalFileName.lastIndexOf('.') + 1));
            }
        }
        if (format == null) {
            format = detectFormat(file);
        }

        if (format == null) {
            throw OntologyException.parsingFailure(null);
        }
        OWLOntology ontology = parseWithFormat(file, format);
        return new ParsedOntology(format, ontology.getOntologyID().getOntologyIRI()
                .map(IRI::toString).orElse(null), ontology.getAxiomCount());
    }

    byte[] convert(byte[] content, OntologyFormat sourceFormat, OntologyFormat targetFormat) {
        Path file = null;
        try {
            file = Files.createTempFile("openprotege-ontology-export-", sourceFormat.extension());
            Files.write(file, content);
            OWLOntology ontology = parseWithFormat(file, sourceFormat);
            var manager = ontology.getOWLOntologyManager();
            try (var output = new java.io.ByteArrayOutputStream()) {
                manager.saveOntology(ontology, targetFormat.documentFormat(), output);
                return output.toByteArray();
            }
        } catch (IOException | org.semanticweb.owlapi.model.OWLOntologyStorageException exception) {
            throw OntologyException.parsingFailure(exception);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to remove temporary ontology file", exception);
                }
            }
        }
    }

    private OntologyFormat detectFormat(Path file) {
        try {
            parseWithFormat(file, OntologyFormat.RDF_XML);
            return OntologyFormat.RDF_XML;
        } catch (OntologyException rdfXmlFailure) {
            try {
                parseWithFormat(file, OntologyFormat.TURTLE);
                return OntologyFormat.TURTLE;
            } catch (OntologyException turtleFailure) {
                throw OntologyException.parsingFailure(turtleFailure);
            }
        }
    }

    private OWLOntology parseWithFormat(Path file, OntologyFormat format) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        Path emptyImportDocument = null;
        try {
            emptyImportDocument = Files.createTempFile("openprotege-blocked-import-", ".rdf");
            Files.writeString(emptyImportDocument, """
                    <?xml version="1.0"?>
                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:owl="http://www.w3.org/2002/07/owl#">
                      <owl:Ontology rdf:about="urn:openprotege:blocked-import"/>
                    </rdf:RDF>
                    """);
            Path blockedImportDocument = emptyImportDocument;
            manager.addIRIMapper(ontologyIri -> IRI.create(blockedImportDocument.toUri()));
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
                    new FileDocumentSource(file.toFile()), configuration);
            String detectedFormat = manager.getOntologyFormat(ontology).getKey().toLowerCase(java.util.Locale.ROOT);
            boolean matches = format == OntologyFormat.TURTLE
                    ? detectedFormat.contains("turtle")
                    : detectedFormat.contains("rdf/xml");
            if (!matches) {
                throw new org.semanticweb.owlapi.model.OWLOntologyCreationException(
                        "The detected ontology format does not match the requested format");
            }
            return ontology;
        } catch (org.semanticweb.owlapi.model.OWLOntologyCreationException exception) {
            throw OntologyException.parsingFailure(exception);
        } catch (IOException exception) {
            throw OntologyException.parsingFailure(exception);
        } catch (RuntimeException exception) {
            throw OntologyException.parsingFailure(exception);
        } finally {
            if (emptyImportDocument != null) {
                try {
                    Files.deleteIfExists(emptyImportDocument);
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to remove temporary import document", exception);
                }
            }
        }
    }

    record ParsedOntology(OntologyFormat format, String ontologyIri, long axiomCount) {}
}
