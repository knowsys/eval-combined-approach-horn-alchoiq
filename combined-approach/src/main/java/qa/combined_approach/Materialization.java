package qa.combined_approach;

import static qa.combined_approach.Util.VAR_T;
import static qa.combined_approach.Util.VAR_X;
import static qa.combined_approach.Util.VAR_Y;
import static qa.combined_approach.Util.VAR_Z;
import static qa.combined_approach.rules.Converter.ConceptToAtom;
import static qa.combined_approach.rules.Converter.PropertytoAtom;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import qa.combined_approach.dataStore.DataStoreConfiguration;
import qa.combined_approach.dataStore.DataStoreInterface;
import qa.combined_approach.dataStore.RDFoxDataStoreWrapper;
import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.Converter;
import qa.combined_approach.rules.HornAlchoiqAxiomType;
import qa.combined_approach.rules.Query;
import qa.combined_approach.rules.Rule;
import uk.ac.ox.cs.JRDFox.JRDFoxException;
import uk.ac.ox.cs.JRDFox.store.DataStore.Format;

/**
 * Class that implements the materialisation stage of the combined-approach to Horn-ALCHOIQ ontologies.
 *
 * @author Irina Dragoste
 *
 */
public class Materialization {

	private final File rdfoxInputLocation;
	private final File aboxFolderLocation;
	private final DataStoreInterface dataStore;
	private final Program program;

	private long generatedFactsCount;
	private long materializationDuration;
	private long startMaterialization;
	private final String exportToFolderLocation;

	public Materialization(final File rdfoxInputLocation, final File aboxFolderLocation, final DataStoreConfiguration dataStoreConfiguration,
			final Program program, final String exportToFolderLocation) {
		super();
		this.rdfoxInputLocation = rdfoxInputLocation;
		this.aboxFolderLocation = aboxFolderLocation;
		this.program = program;
		this.dataStore = new RDFoxDataStoreWrapper(dataStoreConfiguration);
		this.exportToFolderLocation = exportToFolderLocation;
	}

	/**
	 * Computes the materialisation, introducing rules when they are applicable (introducing unnamed individuals and role conjunctions on demand).
	 *
	 * @throws IOException
	 * @throws JRDFoxException
	 */
	public void materialize() throws IOException, JRDFoxException {
		// write Program to file
		System.out.println("  - Writing program to files; Time: " + LocalDate.now() + " " + LocalTime.now());
		final long startWrite = System.currentTimeMillis();
		final File factsFile = createFactsFile(0);
		final File rulesFile = createRulesFile(0);
		Util.appendFactsToFile(program.getFacts(), factsFile);
		Util.appendRulesToFile(program.getRules(), rulesFile);
		final long writeDuration = System.currentTimeMillis() - startWrite;
		System.out.println("    Done writing; Time: " + LocalDate.now() + " " + LocalTime.now() + "; writing duration: " + writeDuration + " ms");

		// import
		System.out.println("  - Importing program to store; Time: " + LocalDate.now() + " " + LocalTime.now());
		final long startImport = System.currentTimeMillis();
		this.dataStore.importFiles(new File[] { rulesFile, factsFile });
		if (this.aboxFolderLocation != null) {
			this.dataStore.importFiles(aboxFolderLocation.listFiles());
		}
		final long importDuration = System.currentTimeMillis() - startImport;
		System.out.println("    Done importing to store; Time: " + LocalDate.now() + " " + LocalTime.now() + "; import duration: " + importDuration + " ms");

		System.out.println("  - Starting materialization; Time: " + LocalDate.now() + " " + LocalTime.now() + ", initial #triples=" + dataStore.countTriples());
		this.startMaterialization = System.currentTimeMillis();
		this.dataStore.reason();

		boolean terminate = false;
		int iterationCount = 0;
		while (!terminate) {
			final long initialTriples = this.dataStore.countTriples();
			System.out.println("    - Iteration: " + iterationCount + "; Time: " + LocalDate.now() + " " + LocalTime.now() + ", #triples=" + initialTriples
					+ ", #unnamedIndiv=" + program.getUnnamedIndividualNames().size());
			iterationCount++;

			/* Apply rules */
			fireUntreatedAxioms(iterationCount);

			this.dataStore.reason();

			/* Terminate when no more facts are derived after an iteration. */
			if (dataStore.countTriples() == initialTriples) {
				System.out.println(" - Final #triples=" + initialTriples);
				terminate = true;
			}
		}
		this.materializationDuration = System.currentTimeMillis() - this.startMaterialization;
		System.out.println(
				"    Done materializing; Time: " + LocalDate.now() + " " + LocalTime.now() + "; materialization duration: " + materializationDuration + " ms");
		this.generatedFactsCount = dataStore.countTriples();

		// export results
		if (exportToFolderLocation != null) {
			dataStore.export(new File(exportToFolderLocation), Format.NTriples);
		}

		this.dataStore.dispose();
	}

	private void fireUntreatedAxioms(final int iteration) throws IOException {
		final File rulesFile = createRulesFile(iteration);
		final File factsFile = createFactsFile(iteration);

		final Set<OWLAxiom> existAxiomsToRemove = new HashSet<>();

		/* Rule (2) */
		for (final OWLAxiom axiom : this.program.getUnprocessedAxioms(HornAlchoiqAxiomType.EXISTS)) {
			/* remove exist axiom after you added the rule */
			final boolean fired = fireExistentialAxiom((OWLSubClassOfAxiom) axiom, rulesFile, factsFile);
			if (fired) {
				existAxiomsToRemove.add(axiom);
			}
		}
		this.program.getUnprocessedAxioms(HornAlchoiqAxiomType.EXISTS).removeAll(existAxiomsToRemove);

		/* Rule (3.2) */
		for (final OWLAxiom axiom : this.program.getUnprocessedAxioms(HornAlchoiqAxiomType.FOR_ALL_ROLES_FROM_DOMAIN)) {
			fireAllRolesFromDomainAxiom((OWLSubClassOfAxiom) axiom, rulesFile, factsFile);
		}

		for (final OWLAxiom axiom : this.program.getUnprocessedAxioms(HornAlchoiqAxiomType.AT_MOST_ONE)) {

			final OWLObjectMaxCardinality maxCard = (OWLObjectMaxCardinality) ((OWLSubClassOfAxiom) axiom).getSuperClass();
			final OWLClassExpression domainC = ((OWLSubClassOfAxiom) axiom).getSubClass();
			final OWLObjectPropertyExpression property = maxCard.getProperty();
			final OWLClassExpression rangeD = maxCard.getFiller();
			final Set<OWLObjectPropertyExpression> roleConjunctionsContainingRole = this.program.getActiveRoleConjunctions()
					.getRoleConjunctionsContainingRole(property);

			/* (4.2) */
			fireAtMostNewIndividual(domainC, rangeD, roleConjunctionsContainingRole, rulesFile, factsFile);
			/* (4.3) */
			fireAtMostAxiomFlip(domainC, rangeD, roleConjunctionsContainingRole, rulesFile);
		}

		this.dataStore.importFiles(new File[] { rulesFile, factsFile });

	}

	/**
	 * Rule (2): C(x) -> R(x,tD), D(tD), T(tD), U(tD)<br>
	 * if there exists ?x in C(?x) :
	 * <ul>
	 * <li>create tD</li>
	 * <li>the first time tD appears in the program, assert T(tD), U(tD), D(tD).</li>
	 * <li>add rule C(x) -> R(x, tD)</li>
	 * </ul>
	 * Once the existential axiom was fired, it can be removed, it does not need to fire twice.
	 *
	 * @param axiom
	 *            C subclass >=1 R.D
	 * @param rulesFile
	 * @param factsFile
	 * @return true, if rule was fired (if class C is not empty); false, otherwise
	 * @throws IOException
	 */
	private boolean fireExistentialAxiom(final OWLSubClassOfAxiom axiom, final File rulesFile, final File factsFile) throws IOException {

		final OWLClassExpression domainC = axiom.getSubClass(); // C

		final String askQueryExistsSubstitution = "ASK { " + ConceptToAtom(domainC, VAR_X).toTurtleAtom() + " }";
		final boolean existsSubstitution = dataStore.answerASKQuery(askQueryExistsSubstitution);
		if (existsSubstitution || domainC.isOWLThing()) {
			final OWLObjectMinCardinality minCard = (OWLObjectMinCardinality) axiom.getSuperClass();
			final OWLObjectPropertyExpression property = minCard.getProperty().getSimplified();

			/* add facts T(ts), U(ts), E(ts) for all E in set s. */
			final String unnamedIndividual = createUnnamedIndividual(minCard, factsFile);

			/* add rule C(x) -> R(x, tD) */
			final Rule rule = new Rule(PropertytoAtom(property, VAR_X, unnamedIndividual), ConceptToAtom(domainC, VAR_X));
			Util.appendRuleToFile(rule, rulesFile);
			return true;
		}
		return false;
	}

	/* create tD; assert T(tD), U(tD), D(tD). */
	private String createUnnamedIndividual(final OWLObjectMinCardinality minCardinality, final File factsFile) throws IOException {
		final Set<String> unnamedIndivConcepts = new HashSet<>(1);
		final OWLClassExpression rangeD = minCardinality.getFiller();
		/* Never add Top to the concept conjunction; Empty concept conjunction means top. */
		if (!rangeD.isOWLThing()) {
			unnamedIndivConcepts.add(Converter.getConceptName(rangeD));
		}

		return createUnnamedIndividual(unnamedIndivConcepts, factsFile);
	}

	/**
	 * axiom: C subseteq forall S.D <br>
	 * Rule (3.2): C(x), SS(x, ts) -> SS(x, t{s,D}), T(t{s,D}), U(t{s,D}), X(t{s,D}) for all X in {s,D} <br>
	 * for all role conjunctions SS containing property S.
	 *
	 * <br>
	 * For all SS (S in SS):
	 * <ul>
	 * <li>We query ?y, such that C(?x), SS(?x, ?y), U(?y) and not D(?y) .</li>
	 * <li>for each answer ts :</li>
	 * <ul>
	 * <li>- the first time t{s,D} appears in the program, assert T(t{s,D}), U(t{s,D}), C(t{s,D}) for all C in {s,D}.</li>
	 * <li>- add rule C(x), SS(x, ts) -> SS(x, t{s,D}) .</li>
	 * </ul>
	 * </ul>
	 *
	 * @param axiom
	 *            C subseteq forall S.D
	 * @param rulesFile
	 * @param factsFile
	 * @throws IOException
	 */
	private void fireAllRolesFromDomainAxiom(final OWLSubClassOfAxiom axiom, final File rulesFile, final File factsFile) throws IOException {
		final OWLClassExpression domainC = axiom.getSubClass();
		final OWLObjectAllValuesFrom maxCard = (OWLObjectAllValuesFrom) axiom.getSuperClass();
		final OWLClassExpression rangeD = maxCard.getFiller();

		final Set<OWLObjectPropertyExpression> roleConjunctionsContainingRole = this.program.getActiveRoleConjunctions()
				.getRoleConjunctionsContainingRole(maxCard.getProperty());

		/* For all role conjunctions containing role */
		for (final OWLObjectPropertyExpression roleConjunction : roleConjunctionsContainingRole) {

			/* We query ?y, such that U(?y), SS(?x, ?y), C(?x) and not D(?y) */
			final Query existsUnnamed = new Query(VAR_Y, new Atom(Util.UNNAMED_PRED, VAR_Y), PropertytoAtom(roleConjunction, VAR_X, VAR_Y),
					ConceptToAtom(domainC, VAR_X));
			String existsSubstFilterQuery = existsUnnamed.toSPARQLQuery().replace("}", "");
			// TODO avoid adding the same rule repeatedly
			existsSubstFilterQuery += " FILTER NOT EXISTS {" + new Atom(rangeD.asOWLClass().toStringID(), VAR_Y).toTurtleAtom() + " }\n}";

			final Set<String> oldUnnamedIndividuals = dataStore.answerSPARQLUnaryQuery(existsSubstFilterQuery);
			for (final String oldUnnamedIndividual : oldUnnamedIndividuals) {
				final Set<String> oldUnnamedIndividualClassConjunctions = this.program.getUnnamedIndividualClassConjunctions(oldUnnamedIndividual);

				final Set<String> newUnnamedIndividualClassConjunctions = new HashSet<>(oldUnnamedIndividualClassConjunctions);
				if (!rangeD.isOWLThing()) {
					newUnnamedIndividualClassConjunctions.add(Converter.getConceptName(rangeD));
				}
				final String newUnnamedIndividual = createUnnamedIndividual(newUnnamedIndividualClassConjunctions, factsFile);
				/* C(x), SS(x, ts) -> SS(x, t{s,D}) . */
				final Rule rule = new Rule(PropertytoAtom(roleConjunction, VAR_X, newUnnamedIndividual), ConceptToAtom(domainC, VAR_X),
						PropertytoAtom(roleConjunction, VAR_X, oldUnnamedIndividual));
				Util.appendRuleToFile(rule, rulesFile);
			}
		}
	}

	/**
	 * axiom: C subseteq <1 R.D<br>
	 * Rule (4.3): D(y), RR^-(y,x), C(x), SS(x,ts), D(ts) -> X(y), {RR^-,SS^-}(y,x) for all X in s. <br>
	 * for all role conjunctions RR and SS containing property R.<br>
	 * <br>
	 * For all RR, SS (R in RR and SS):
	 * <ul>
	 * <li>Query: ?z such that U(?z), D(?z), SS(?x,?z), C(?x), RR^-(?y,?x), D(?y) .</li>
	 * <li>if there is at least one answer to the query, add rule U(?z), D(?z), SS(?x,?z), C(?x), RR^-(?y,?x), D(?y) -> {RR^-,SS^-}(y,x).</li>
	 * <li>for each query answer ts:</li>
	 * <ul>
	 * <li>- add rule D(y), RR^-(y,x), C(x), SS(x,ts), D(ts) -> X(y) for all X in s.</li>
	 * </ul>
	 * </ul>
	 *
	 * @param domainC
	 *            C
	 * @param rangeD
	 *            D
	 * @param roleConjunctionsContainingRole
	 *            RR, SS
	 * @param rulesFile
	 * @throws IOException
	 */
	private void fireAtMostAxiomFlip(final OWLClassExpression domainC, final OWLClassExpression rangeD,
			final Set<OWLObjectPropertyExpression> roleConjunctionsContainingRole, final File rulesFile) throws IOException {

		for (final OWLObjectPropertyExpression rRoleConjunction : roleConjunctionsContainingRole) {
			for (final OWLObjectPropertyExpression sRoleConjunction : roleConjunctionsContainingRole) {

				/* Query: ?z such that U(?z), D(?z), SS(?x,?z), C(?x), RR^-(?y,?x), D(?y) . */
				final Query existsSubstitution = new Query(VAR_Z, new Atom(Util.UNNAMED_PRED, VAR_Z), ConceptToAtom(rangeD, VAR_Z),
						PropertytoAtom(sRoleConjunction, VAR_X, VAR_Z), ConceptToAtom(domainC, VAR_X),
						PropertytoAtom(rRoleConjunction.getInverseProperty(), VAR_Y, VAR_X), ConceptToAtom(rangeD, VAR_Y));

				final Set<String> unnamedIndividuals = dataStore.answerUnaryQuery(existsSubstitution);
				if (!unnamedIndividuals.isEmpty()) {

					final RoleConjunction rInverseRoleConjunction = program.getActiveRoleConjunctions().getRoleConjunctionsByName()
							.get(rRoleConjunction.getInverseProperty());
					final RoleConjunction sInverseRoleConjunction = program.getActiveRoleConjunctions().getRoleConjunctionsByName()
							.get(sRoleConjunction.getInverseProperty());
					final Set<OWLObjectPropertyExpression> rsInverseRoleSet = new HashSet<>();
					rsInverseRoleSet.addAll(rInverseRoleConjunction.getRoleSet());
					rsInverseRoleSet.addAll(sInverseRoleConjunction.getRoleSet());

					final OWLObjectPropertyExpression rsInverseRoleConjunction = obtainRoleConjunction(rulesFile, rsInverseRoleSet);
					/* U(?z), D(?z), SS(?x,?z), C(?x), RR^-(?y,?x), D(?y) -> {RR^-,SS^-}(y,x) . */
					final Rule rolesRule = new Rule(PropertytoAtom(rsInverseRoleConjunction, VAR_Y, VAR_X),
							new Atom[] { new Atom(Util.UNNAMED_PRED, VAR_Z), ConceptToAtom(rangeD, VAR_Z), PropertytoAtom(sRoleConjunction, VAR_X, VAR_Z),
									ConceptToAtom(domainC, VAR_X), PropertytoAtom(rRoleConjunction.getInverseProperty(), VAR_Y, VAR_X),
									ConceptToAtom(rangeD, VAR_Y) });
					Util.appendRuleToFile(rolesRule, rulesFile);

					for (final String unnamedIndividual : unnamedIndividuals) {
						// TODO prevent adding this rule again and again for the same RR,SS and ts

						/* D(y), RR^-(y,x), C(x), SS(x,ts), D(ts) -> */
						final Atom[] ruleBody = { ConceptToAtom(rangeD, VAR_Y), PropertytoAtom(rRoleConjunction.getInverseProperty(), VAR_Y, VAR_X),
								ConceptToAtom(domainC, VAR_X), PropertytoAtom(sRoleConjunction, VAR_X, unnamedIndividual),
								ConceptToAtom(rangeD, unnamedIndividual) };

						final Set<String> classConjunction = program.getUnnamedIndividualClassConjunctions(unnamedIndividual);
						for (final String classConjunct : classConjunction) {
							/* -> X(y) for all X in s. */
							final Rule classesRule = new Rule(new Atom(classConjunct, VAR_Y), ruleBody);
							Util.appendRuleToFile(classesRule, rulesFile);
						}
					}
				}
			}
		}
	}

	/**
	 * axiom: C subseteq <1 R.D<br>
	 * Rule (4.2): C(x), RR(x, ta), D(ta), SS(x, tb), D(tb) -> {RR,SS}(x, t{a,b}), X(t{a,b}) for all X in {a,b}, T(t{a,b}), U(t{a,b}). <br>
	 * for all role conjunctions RR and SS containing property R.<br>
	 * <br>
	 * For all RR, SS (R in RR and SS):
	 * <ul>
	 * <li>Query ?z, ?t such that C(x), RR(x, ?z), D(?z), U(?z), SS(x, ?t), D(?t), U(?t) . (?z for ta, ?t for tb).</li>
	 * <li>if there are answers, add new active role {RR,SS} (and its inverse).</li>
	 * <li>for every ta, tb in the answer:</li>
	 * <ul>
	 * <li>- create individual t{a,b} and, if new, assert T(t{a,b}), U(t{a,b}), X(t{a,b}) for all X in {a,b} .</li>
	 * <li>- add rule C(x), RR(x, ta), D(ta), SS(x, tb), D(tb) -> {RR,SS}(x, t{a,b}) .</li>
	 * </ul>
	 * </ul>
	 *
	 * @param domainC
	 *            C
	 * @param rangeD
	 *            D
	 * @param roleConjunctionsContainingRole
	 *            RR, SS
	 * @param rulesFile
	 * @param factsFile
	 * @throws IOException
	 */
	private void fireAtMostNewIndividual(final OWLClassExpression domainC, final OWLClassExpression rangeD,
			final Set<OWLObjectPropertyExpression> roleConjunctionsContainingRole, final File rulesFile, final File factsFile) throws IOException {
		// TODO prevent adding this rule again and again for the same RR,SS, ta and tb

		for (final OWLObjectPropertyExpression rRoleConjunction : roleConjunctionsContainingRole) {
			for (final OWLObjectPropertyExpression sRoleConjunction : roleConjunctionsContainingRole) {
				/* Query ?z, ?t such that D(?z), U(?z), RR(x, ?z), C(x), SS(x, ?t), D(?t), U(?t) . (?z for ta, ?t for tb). */
				final Query existsSubstitution = new Query(VAR_Z, VAR_T, ConceptToAtom(rangeD, VAR_Z), new Atom(Util.UNNAMED_PRED, VAR_Z),
						PropertytoAtom(rRoleConjunction, VAR_X, VAR_Z), ConceptToAtom(domainC, VAR_X), PropertytoAtom(sRoleConjunction, VAR_X, VAR_T),
						ConceptToAtom(rangeD, VAR_T), new Atom(Util.UNNAMED_PRED, VAR_T));

				final Set<Map<String, String>> unnamedIndividuals = dataStore.answerQuery(existsSubstitution);
				if (!unnamedIndividuals.isEmpty()) {

					final Set<OWLObjectPropertyExpression> rsRoleSet = new HashSet<>();
					rsRoleSet.addAll(program.getActiveRoleConjunctions().getRoleConjunctionByName(rRoleConjunction));
					rsRoleSet.addAll(program.getActiveRoleConjunctions().getRoleConjunctionByName(sRoleConjunction));
					final OWLObjectPropertyExpression rsRoleConjunction = obtainRoleConjunction(rulesFile, rsRoleSet);

					for (final Map<String, String> unnamedPair : unnamedIndividuals) {
						final String unnamedA = unnamedPair.get(VAR_Z);
						final String unnamedB = unnamedPair.get(VAR_T);
						final Set<String> classConjunction = new HashSet<>();
						classConjunction.addAll(program.getUnnamedIndividualClassConjunctions(unnamedA));
						classConjunction.addAll(program.getUnnamedIndividualClassConjunctions(unnamedB));

						/* the first time t{a,b} appears in the program, assert T(t{a,b}), U(t{a,b}), X(t{a,b}) for all X in {a,b}. */
						final String unnamedIndividualAB = createUnnamedIndividual(classConjunction, factsFile);

						/* C(x), RR(x, ta), D(ta), SS(x, tb), D(tb) -> {RR,SS}(x, t{a,b}) */
						final Rule rule = new Rule(PropertytoAtom(rsRoleConjunction, VAR_X, unnamedIndividualAB), ConceptToAtom(domainC, VAR_X),
								PropertytoAtom(rRoleConjunction, VAR_X, unnamedA), ConceptToAtom(rangeD, unnamedA),
								PropertytoAtom(sRoleConjunction, VAR_X, unnamedB), ConceptToAtom(rangeD, unnamedB));
						Util.appendRuleToFile(rule, rulesFile);
					}
				}
			}
		}
	}

	/**
	 * If role set already existed in the active role conjunctions, it is returned. <br>
	 * Else, a new role conjunction and its inverse are created and added to the active roles. Rules (Role.1) and (Role.2) are created for the new new
	 * roleConjunction and for its inverse.
	 *
	 * @param rulesFile
	 * @param roleSet
	 * @return the role conjunction corresponding to given {@code roleSet}.
	 * @throws IOException
	 */
	private OWLObjectPropertyExpression obtainRoleConjunction(final File rulesFile, final Set<OWLObjectPropertyExpression> roleSet) throws IOException {
		final OWLObjectPropertyExpression existingRoleConjunction = program.getActiveRoleConjunctions().getRoleConjunctionName(roleSet);
		if (existingRoleConjunction == null) {

			final RoleConjunction newRoleConjunction = addNewActiveRoleConjunction(rulesFile, roleSet);

			/* when a new role conjunction is added, its inverse must be added, too, to the active role conjunctions. */
			final Set<OWLObjectPropertyExpression> newInverseRoleConjunction = ActiveRoleConjunctions.invert(newRoleConjunction.getRoleSet());
			addNewActiveRoleConjunction(rulesFile, newInverseRoleConjunction);

			return newRoleConjunction.getRoleName();

		} else {
			/* role conjunction already exists. */
			return existingRoleConjunction;
		}
	}

	/**
	 * Creates a new role conjunction (that did not exist before in the active role set) and adds it to the active role set. <br>
	 * (Role.1) and (Role.2) are created for the new roleConjunction.
	 *
	 * @param rulesFile
	 * @param roleSet
	 * @return the new role conjunction.
	 * @throws IOException
	 */
	private RoleConjunction addNewActiveRoleConjunction(final File rulesFile, final Set<OWLObjectPropertyExpression> roleSet) throws IOException {
		/* new role conjunction is added. */
		final RoleConjunction newRoleConjunction = program.getActiveRoleConjunctions().addRoleConjunction(roleSet);
		/* (Role.1) */
		final Rule rulePropagateInverseRoleConjunction = OntologyToProgram.createRulePropagateInverseRoleConjunction(newRoleConjunction.getRoleName());
		/* (Role.2) */
		final Set<Rule> rulesDeriveRolesFromRoleConjunction = OntologyToProgram.createRulesDeriveRolesFromRoleConjunction(newRoleConjunction.getRoleName(),
				newRoleConjunction);

		Util.appendRuleToFile(rulePropagateInverseRoleConjunction, rulesFile);
		Util.appendRulesToFile(rulesDeriveRolesFromRoleConjunction, rulesFile);
		return newRoleConjunction;
	}

	/**
	 * the first time ts appears in the program, assert T(ts), U(ts), C(ts) for all C in S. Else, reuse ts.
	 *
	 * @param classConjunction
	 * @param factsFile
	 * @return
	 * @throws IOException
	 */
	private String createUnnamedIndividual(final Set<String> classConjunction, final File factsFile) throws IOException {
		final String existingUnnamedIndividual = program.getUnnamedIndividualNames().get(classConjunction);
		if (existingUnnamedIndividual != null) {
			return existingUnnamedIndividual;
		} else {
			return program.createNewUnnamedIndividual(classConjunction, factsFile);
		}
	}

	private File createFactsFile(final int iteration) throws IOException {
		final File factsFile = new File(this.rdfoxInputLocation + File.separator + "Iteration" + iteration + "_" + UUID.randomUUID().toString() + ".ttl");
		Util.initializeFile(factsFile);
		return factsFile;
	}

	private File createRulesFile(final int iteration) throws IOException {
		final File rulesFile = new File(this.rdfoxInputLocation + File.separator + "Iteration" + iteration + "_" + UUID.randomUUID().toString() + ".txt");
		Util.initializeFile(rulesFile);
		return rulesFile;
	}

	public Program getProgram() {
		return this.program;
	}

	public long getGeneratedFactsCount() {
		return this.generatedFactsCount;
	}

	public long getMaterializationDuration() {
		return this.materializationDuration;
	}

	public long getStartMaterialization() {
		return this.startMaterialization;
	}

}
