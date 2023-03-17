package qa.combined_approach;

import java.io.File;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class CompareFacts {

	/**
	 * Returns true, if all the facts in a are contained in b.
	 *
	 * @param a
	 * @param b
	 * @return
	 * @throws OWLOntologyCreationException
	 */
	public static void areAFactsContainedInBEquals(final File a, final File b) throws OWLOntologyCreationException {
		final OWLOntology ontA = Util.loadOntologyWithImports(a);
		final OWLOntology ontB = Util.loadOntologyWithImports(b);

		// boolean allFound = true;
		ontA.axioms().forEach(aAxiom -> {
			final boolean anyMatch = ontB.axioms().anyMatch(bAxiom -> bAxiom.equals(aAxiom));
			if (!anyMatch) {
				// allFound = false;
				System.out.println("Axiom not found: " + aAxiom);
			}
		});
		// return allFound;
	}

	/**
	 * Returns true, if all the facts in a are contained in b.
	 *
	 * @param a
	 * @param b
	 * @throws OWLOntologyCreationException
	 */
	public static void areAFactsContainedInB(final File a, final File b) throws OWLOntologyCreationException {
		final OWLOntology ontA = Util.loadOntologyWithImports(a);
		final OWLOntology ontB = Util.loadOntologyWithImports(b);
		System.out.println("B Axioms: " + ontA.axioms().count());
		// B Axioms: 383
		// boolean allFound = true;

		ontA.axioms(AxiomType.CLASS_ASSERTION).forEach(aAxiom -> {
			final boolean anyMatch = ontB.axioms(AxiomType.CLASS_ASSERTION).anyMatch(bAxiom -> isEqual(bAxiom, aAxiom));
			if (!anyMatch) {
				// allFound = false;
				System.out.println("Axiom not found: " + aAxiom);
			}
		});
		// return allFound;
	}

	private static boolean isEqual(final OWLClassAssertionAxiom bAxiom, final OWLClassAssertionAxiom aAxiom) {
		final OWLClass bAxiomClass = bAxiom.getClassExpression().asOWLClass();
		final OWLClass aAxiomClass = aAxiom.getClassExpression().asOWLClass();
		final OWLIndividual bIndividual = bAxiom.getIndividual();
		final OWLIndividual aIndividual = aAxiom.getIndividual();
		return bAxiomClass.toStringID().equals(aAxiomClass.toStringID()) && bIndividual.toStringID().equals(aIndividual.toStringID());
	}

}
