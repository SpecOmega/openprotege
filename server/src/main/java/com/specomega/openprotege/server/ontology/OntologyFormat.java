package com.specomega.openprotege.server.ontology;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

import java.util.Locale;

public enum OntologyFormat {
    RDF_XML("RDF/XML", "application/rdf+xml", ".owl", ".rdf", ".xml"),
    TURTLE("Turtle", "text/turtle", ".ttl");

    private final String displayName;
    private final String mediaType;
    private final String[] extensions;

    OntologyFormat(String displayName, String mediaType, String... extensions) {
        this.displayName = displayName;
        this.mediaType = mediaType;
        this.extensions = extensions;
    }

    String displayName() {
        return displayName;
    }

    String mediaType() {
        return mediaType;
    }

    String extension() {
        return extensions[0];
    }

    OWLDocumentFormat documentFormat() {
        return switch (this) {
            case RDF_XML -> new RDFXMLDocumentFormat();
            case TURTLE -> new TurtleDocumentFormat();
        };
    }

    static OntologyFormat parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "rdf/xml", "rdfxml", "rdf_xml" -> RDF_XML;
            case "turtle", "ttl" -> TURTLE;
            default -> throw OntologyException.unsupportedFormat(value);
        };
    }

    static OntologyFormat fromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        for (OntologyFormat format : values()) {
            for (String extension : format.extensions) {
                if (normalized.endsWith(extension)) {
                    return format;
                }
            }
        }
        return null;
    }
}
