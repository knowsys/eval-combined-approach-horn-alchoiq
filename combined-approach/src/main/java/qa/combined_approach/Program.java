package qa.combined_approach;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.HornAlchoiqAxiomType;
import qa.combined_approach.rules.Rule;

public class Program {

	/*
	 * for checking if an unnamed individual with the same classes exists, using the
	 * hash
	 */
	private final Map<Set<String>, String> unnamedIndividualNames = new HashMap<>();
	/* for retrieving an unnamed individual by name */
	private final Map<String, Set<String>> unnamedIndividualClassConjunctions = new HashMap<>();

	/**
	 * Associates every property (and inverse of property) in the TBox with its
	 * super properties (including itself).
	 */
	private final SuperProperties superProperties = new SuperProperties();

	private final ActiveRoleConjunctions activeRoleConjunctions = new ActiveRoleConjunctions();

	private final Map<HornAlchoiqAxiomType, Integer> countAxiomsByType = new HashMap<>();
	private final List<Rule> rules = new ArrayList<>();
	private final List<Atom> facts = new ArrayList<>();
	private final Map<HornAlchoiqAxiomType, List<OWLAxiom>> unprocessedAxioms = new HashMap<>();

	/**
	 * Asserts T(ts), U(ts), C(ts) for each C in S (S = classConjunction)
	 *
	 * @param classConjunction
	 * @param resultingFactsFile
	 * @return
	 * @throws IOException
	 */
	public String createNewUnnamedIndividual(final Set<String> classConjunction, final File resultingFactsFile)
			throws IOException {

		final int id = unnamedIndividualClassConjunctions.size();
		final String newUnnamedIndividual = "<Unnamed_" + id + ">";
		unnamedIndividualClassConjunctions.put(newUnnamedIndividual, classConjunction);
		unnamedIndividualNames.put(classConjunction, newUnnamedIndividual);

		assertFactsForNewUnnamedIndividual(newUnnamedIndividual, classConjunction, resultingFactsFile);
		return newUnnamedIndividual;
	}

	/**
	 * Asserts T(ts), U(ts), C(ts) for each C in S (S = classConjunction)
	 *
	 * @param unnamedIndividual:
	 *            ts
	 * @param classConjunction
	 *            S
	 * @param resultingFactsFile
	 * @throws IOException
	 */
	private void assertFactsForNewUnnamedIndividual(final String unnamedIndividual, final Set<String> classConjunction,
			final File assertedFactsFile) throws IOException {
		/* T(ts) */
		Util.appendFactToFile(AxiomsToRules.assertIndividualInTop(unnamedIndividual), assertedFactsFile);
		/* U(ts) */
		Util.appendFactToFile(new Atom(Util.UNNAMED_PRED, unnamedIndividual), assertedFactsFile);
		/* C(ts) for each C in S */
		for (final String classConjunct : classConjunction) {
			Util.appendFactToFile(new Atom(classConjunct, unnamedIndividual), assertedFactsFile);
		}
	}

	public void computeSuperProperties() {
		superProperties.computeSuperProperties();
		activeRoleConjunctions.initializeActiveRoleConjunctions(superProperties);
	}

	/**
	 * Make sure properties are simplified
	 *
	 * @param objectPropertyExpression
	 */
	public void addPropertyAndItsInverse(final OWLObjectPropertyExpression objectPropertyExpression) {
		superProperties.addPropertyAndItsInverse(objectPropertyExpression);
	}

	/**
	 * Make sure properties are simplified
	 *
	 * @param subProperty
	 * @param superProperty
	 */
	public void addPropertySubsumptionAndItsInverse(final OWLObjectPropertyExpression subProperty,
			final OWLObjectPropertyExpression superProperty) {
		superProperties.addPropertySubsumptionAndItsInverse(subProperty, superProperty);
	}

	public void addUnprocessedAxiom(final HornAlchoiqAxiomType axiomType, final OWLAxiom axiom) {
		unprocessedAxioms.putIfAbsent(axiomType, new ArrayList<>());
		unprocessedAxioms.get(axiomType).add(axiom);
	}

	public void incrementAxiomCount(final HornAlchoiqAxiomType axiomType) {
		increaseAxiomCount(axiomType, 1);
	}

	public void increaseAxiomCount(final HornAlchoiqAxiomType axiomType, final int value) {
		countAxiomsByType.compute(axiomType, (k, v) -> (v == null) ? value : v + value);
	}

	/* Getters */

	public ActiveRoleConjunctions getActiveRoleConjunctions() {
		return this.activeRoleConjunctions;
	}

	public Map<Set<String>, String> getUnnamedIndividualNames() {
		return this.unnamedIndividualNames;
	}

	public Map<HornAlchoiqAxiomType, Integer> getCountAxiomsByType() {
		return this.countAxiomsByType;
	}

	public List<OWLAxiom> getUnprocessedAxioms(final HornAlchoiqAxiomType hornAlchoiqAxiomType) {
		return this.unprocessedAxioms.getOrDefault(hornAlchoiqAxiomType, new ArrayList<>());
	}

	public List<Rule> getRules() {
		return this.rules;
	}

	public List<Atom> getFacts() {
		return this.facts;
	}

	public SuperProperties getSuperProperties() {
		return superProperties;
	}

	public Set<String> getUnnamedIndividualClassConjunctions(final String unnamedIndividualName) {
		return unnamedIndividualClassConjunctions.get(unnamedIndividualName);
	}

}
