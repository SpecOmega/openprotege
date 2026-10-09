import java.io.File;
import java.util.HashSet;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

public class OwlFormatRoundTrip {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected input RDF/XML, output Turtle, and output RDF/XML paths");
        }

        File input = new File(args[0]);
        File turtle = new File(args[1]);
        File rdfxml = new File(args[2]);

        OWLOntologyManager sourceManager = OWLManager.createOWLOntologyManager();
        OWLOntology source = sourceManager.loadOntologyFromOntologyDocument(input);
        OWLDocumentFormat sourceFormat = sourceManager.getOntologyFormat(source);
        var sourceAxioms = new HashSet<>(source.getAxioms());
        var sourceAnnotations = new HashSet<>(source.getAnnotations());
        var sourceImports = new HashSet<>(source.getImportsDeclarations());
        var sourceId = source.getOntologyID();

        sourceManager.saveOntology(source, new TurtleDocumentFormat(), IRI.create(turtle));
        OWLOntologyManager turtleManager = OWLManager.createOWLOntologyManager();
        OWLOntology fromTurtle = turtleManager.loadOntologyFromOntologyDocument(turtle);
        var turtleAxioms = new HashSet<>(fromTurtle.getAxioms());
        var turtleAnnotations = new HashSet<>(fromTurtle.getAnnotations());
        var turtleImports = new HashSet<>(fromTurtle.getImportsDeclarations());

        turtleManager.saveOntology(fromTurtle, new RDFXMLDocumentFormat(), IRI.create(rdfxml));
        OWLOntologyManager rdfxmlManager = OWLManager.createOWLOntologyManager();
        OWLOntology fromRdfxml = rdfxmlManager.loadOntologyFromOntologyDocument(rdfxml);
        var rdfxmlAxioms = new HashSet<>(fromRdfxml.getAxioms());
        var rdfxmlAnnotations = new HashSet<>(fromRdfxml.getAnnotations());
        var rdfxmlImports = new HashSet<>(fromRdfxml.getImportsDeclarations());

        boolean passed = sourceFormat instanceof RDFXMLDocumentFormat
                && sourceId.equals(fromTurtle.getOntologyID())
                && sourceId.equals(fromRdfxml.getOntologyID())
                && sourceAxioms.equals(turtleAxioms)
                && sourceAxioms.equals(rdfxmlAxioms)
                && sourceAnnotations.equals(turtleAnnotations)
                && sourceAnnotations.equals(rdfxmlAnnotations)
                && sourceImports.equals(turtleImports)
                && sourceImports.equals(rdfxmlImports);

        System.out.println("Source syntax: " + sourceFormat.getClass().getSimpleName());
        System.out.println("Axiom counts: RDF/XML input=" + sourceAxioms.size()
                + ", Turtle reload=" + turtleAxioms.size()
                + ", RDF/XML reload=" + rdfxmlAxioms.size());
        System.out.println("Ontology ID, axioms, annotations, and imports preserved: " + passed);
        System.out.println("Serialized file sizes: Turtle=" + turtle.length()
                + " bytes; RDF/XML=" + rdfxml.length() + " bytes");
        if (!passed) {
            throw new AssertionError("OWL document round trip changed ontology content");
        }
    }
}
