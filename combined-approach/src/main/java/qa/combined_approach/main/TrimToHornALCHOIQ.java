package qa.combined_approach.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import qa.combined_approach.Util;

/**
 * Main class that removes non-Horn-ALCHOIQ axioms from an ontology in the normal form, and exports it in an OWL-XML format .
 *
 * @author Irina Dragoste
 *
 */
public class TrimToHornALCHOIQ {

	public static void main(final String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String ontologyFilePath = args[0];
		final String trimmedOntologyFolder = args[1];

		trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

	/**
	 * Removes non-Horn-ALCHOIQ axioms from an ontology in the normal form.
	 *
	 * @param ontologyFilePath
	 *            ontology to be trimmed.
	 * @param trimmedOntologyFolder
	 *            folder where the trimmed ontology will be saved, in OWL-XML format.
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws FileNotFoundException
	 */
	public static void trimToHornALCHOIQ(final String ontologyFilePath, final String trimmedOntologyFolder)
			throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final File ontologyFile = new File(ontologyFilePath);
		final String ontologyFileName = ontologyFile.getName();
		System.out.println("Trimming: " + ontologyFileName + "; Time: " + LocalDate.now() + " " + LocalTime.now());

		// load
		final OWLOntology ontology = Util.loadOntologyWithImports(ontologyFile);

		final Set<OWLAxiom> axioms = ontology.axioms().collect(Collectors.toSet());
		System.out.println("   Orig axiom count: " + axioms.size());
		final Set<OWLAxiom> toRemove = gatherNonHornALCHOIQ(axioms);
		System.out.println("   Non-HornALCHOIQ ontologies to remove:");
		for (final OWLAxiom owlAxiom : toRemove) {
			System.out.println("    - " + owlAxiom);
		}

		axioms.removeAll(toRemove);
		System.out.println("   Trimmed HornALCHOIQ axiom count: " + axioms.size());
		saveAxiomsToOWLOntology(axioms, trimmedOntologyFolder + File.separator + ontologyFileName);
		System.out.println("Trimmed: " + ontologyFileName + "; Time: " + LocalDate.now() + " " + LocalTime.now());
	}

	public static Set<OWLAxiom> gatherNonHornALCHOIQ(final Set<OWLAxiom> origAxioms) {
		final Set<OWLAxiom> nonHornALCHOIQ = new HashSet<>();

		for (final OWLAxiom axiom : origAxioms) {
			if (axiom.isLogicalAxiom() && !AxiomType.ABoxAxiomTypes.contains(axiom.getAxiomType())) {
				final String axiomType = axiom.getAxiomType().getName();
				if (axiomType.equals("SubPropertyChainOf") || axiomType.equals("Rule") || axiomType.equals("DisjointObjectProperties")
						|| axiomType.equals("AsymmetricObjectProperty")) {
					nonHornALCHOIQ.add(axiom);
				} else if (axiomType.equals("SubClassOf")) {
					final OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom).getSubClass();
					final OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom).getSuperClass();
					switch (superClass.getClassExpressionType()) {
						case OBJECT_ONE_OF: // Nominal
							final OWLObjectOneOf nominals = (OWLObjectOneOf) superClass;
							if (nominals.individuals().count() > 1) {
								nonHornALCHOIQ.add(axiom);// non Horn
							}
							break;
						case OBJECT_MAX_CARDINALITY:
							final int maxCardinality = ((OWLObjectMaxCardinality) superClass).getCardinality();
							if (maxCardinality > 1) {
								nonHornALCHOIQ.add(axiom); // non Horn
							}
							break;
						case OWL_CLASS:
						case OBJECT_UNION_OF: // A1 cap ... cap An sqs B1 cup ... cup Bm
							final int superClassSize = superClass.asDisjunctSet().size();
							if (superClassSize > 1) {
								nonHornALCHOIQ.add(axiom); // non Horn
							}
							if (ClassExpressionType.OBJECT_HAS_SELF.equals(subClass.getClassExpressionType())) {
								nonHornALCHOIQ.add(axiom); // non Horn
							}
							break;
						case OBJECT_HAS_SELF:
							nonHornALCHOIQ.add(axiom); // non Horn
							break;
						default:
							break;
					}
				}
			}
		}

		return nonHornALCHOIQ;
	}

	/**
	 * Save given normalizedAxs to an ontology in OWL XML FORMAT at given file path.
	 *
	 * @param normalizedAxs
	 *            axioms to be saved into an ontology
	 * @param filePath
	 *            ontology file path
	 * @throws OWLOntologyStorageException
	 * @throws FileNotFoundException
	 * @throws OWLOntologyCreationException
	 */
	public static void saveAxiomsToOWLOntology(final Set<OWLAxiom> normalizedAxs, final String filePath)
			throws OWLOntologyStorageException, FileNotFoundException, OWLOntologyCreationException {
		final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		final OWLOntology owlAPIOntology = manager.createOntology();
		for (final OWLAxiom axiom : normalizedAxs) {
			manager.addAxiom(owlAPIOntology, axiom);
		}

		manager.saveOntology(owlAPIOntology, new OWLXMLDocumentFormat(), new FileOutputStream(filePath));
	}

}
