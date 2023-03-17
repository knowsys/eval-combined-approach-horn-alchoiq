package qa.combined_approach;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.Rule;

public class Util {
	public static final String VAR_X = "?x";
	public static final String VAR_Y = "?y";
	public static final String VAR_Z = "?z";
	public static final String VAR_T = "?t";

	public static final String NAMED_PRED = "http://www.w3.org/2002/07/owl#NamedIndividual";
	public static final String UNNAMED_PRED = "Unnamed";

	public static OWLOntology loadOntologyWithImports(final File inputOntologyPath) throws OWLOntologyCreationException {
		final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		final OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration()
				.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION);
		final OWLOntologyDocumentSource documentSource = new FileDocumentSource(inputOntologyPath);
		return manager.loadOntologyFromOntologyDocument(documentSource, config);
	}

	public static void appendRulesToFile(final Collection<Rule> rules, final File file) throws IOException {
		for (final Rule rule : rules) {
			appendRuleToFile(rule, file);
		}
	}

	public static void appendRuleToFile(final Rule rule, final File file) throws IOException {
		Util.writeToFile(file, rule.toRDFOxFormat(), true);
	}

	public static void appendFactsToFile(final Collection<Atom> facts, final File file) throws IOException {
		for (final Atom fact : facts) {
			appendFactToFile(fact, file);
		}
	}

	public static void appendFactToFile(final Atom fact, final File file) throws IOException {
		Util.writeToFile(file, fact.toTurtleAtom() + "\n", true);
	}

	public static void moveFileToFolder(final File ontologyFile, final String destinationFolder) {
		moveAndRenameFile(ontologyFile, destinationFolder, ontologyFile.getName());
	}

	public static void moveAndRenameFile(final File ontologyFile, final String destinationFolder, final String newName) {
		final String ontologyFileName = ontologyFile.getName();
		final boolean successfullyMoved = ontologyFile.renameTo(new File(destinationFolder + File.separator + newName));
		if (!successfullyMoved) {
			System.out.println("Error while moving file " + ontologyFileName + " to " + destinationFolder + " !!!");
		}
	}

	public static void initializeFile(final File file) throws IOException {
		Util.writeToFile(file, "", false);
	}

	public static void writeToFile(final File file, final String content, final boolean append) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, append))) {
			bw.write(content);
		}
	}
}
