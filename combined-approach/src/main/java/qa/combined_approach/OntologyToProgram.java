package qa.combined_approach;

import static org.junit.Assert.assertNotNull;
import static qa.combined_approach.Util.VAR_X;
import static qa.combined_approach.Util.VAR_Y;
import static qa.combined_approach.rules.Converter.PropertytoAtom;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.collect.Sets;

import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.Converter;
import qa.combined_approach.rules.HornAlchoiqAxiomType;
import qa.combined_approach.rules.Rule;
import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

/**
 * Class that transforms ontology axioms to program rules.
 *
 * @author Irina Dragoste
 *
 */
public class OntologyToProgram {

	private final Program program;

	/**
	 * Transforms ontology axioms to program rules set.
	 *
	 * @param ontology
	 */
	public OntologyToProgram(final OWLOntology ontology) {

		System.out.println("  - Creating program from ontology; Time: " + LocalDate.now() + " " + LocalTime.now());
		final long startCreateProgram = System.currentTimeMillis();
		this.program = new Program();
		for (final OWLAxiom axiom : ontology.logicalAxioms().collect(Collectors.toList())) {
			if (!AxiomType.ABoxAxiomTypes.contains(axiom.getAxiomType())) {
				AxiomsToRules.collectRules(axiom, program);
			} else {
				final Set<String> namedIndividuals = new HashSet<>();

				AxiomsToRules.collectAboxAssertions(axiom, program, namedIndividuals);

				namedIndividuals.forEach(namedIndividual -> {
					program.getFacts().add(new Atom(Util.NAMED_PRED, namedIndividual));
				});
			}
		}
		program.computeSuperProperties();

		/* (6.1) and (6.2) */
		addRulesForRoleToSuperRolesClosure();
		/* (Role.1) */
		propagateInverseRoleSets();
		/* (Role.2) */
		deriveRolesFromRoleSets();
		/* N(?x) ->T(?x) . */
		axiomatizeNamedIndividualToTop();
		/* owl:differentFrom(?x,?x) -> Bottom(?x) . */
		axiomatizeDifferentFrom();

		System.out.println("    #Tbox axioms: " + countTBoxAxioms(program.getCountAxiomsByType()));
		System.out.println("    #TBox rules: " + program.getRules().size());
		System.out.println("    #TBox facts: " + program.getFacts().size());
		System.out.println("    #initial active role conjunctions: " + program.getActiveRoleConjunctions().getRoleConjunctionNames().size());

		final long programDuration = System.currentTimeMillis() - startCreateProgram;
		System.out.println(
				"    Done creating program; Time: " + LocalDate.now() + " " + LocalTime.now() + "; creating program duration: " + programDuration + " ms");

	}

	public int countTBoxAxioms(final Map<HornAlchoiqAxiomType, Integer> countAxiomsByType) {
		int tboxCount = 0;
		final EnumSet<HornAlchoiqAxiomType> tBoxAxiomTypes = EnumSet.of(HornAlchoiqAxiomType.SUBSUME_CONCEPT_CONJUNCTION,
				HornAlchoiqAxiomType.FOR_ALL_ROLES_FROM_DOMAIN, HornAlchoiqAxiomType.EXISTS, HornAlchoiqAxiomType.AT_MOST_ONE, HornAlchoiqAxiomType.NOMINAL,
				HornAlchoiqAxiomType.ROLE_SUBSUMPTION);
		for (final HornAlchoiqAxiomType tboxAxiomType : tBoxAxiomTypes) {
			tboxCount += countAxiomsByType.getOrDefault(tboxAxiomType, 0);
		}
		return tboxCount;
	}

	/* Rule (Role.1) */
	private void propagateInverseRoleSets() {
		program.getActiveRoleConjunctions().getRoleConjunctionNames()
				.forEach(roleConjunctionName -> program.getRules().add(createRulePropagateInverseRoleConjunction(roleConjunctionName)));
	}

	/**
	 * Rule (Role.1) <br>
	 * RoleConjunction(?x, ?y), N(?y) -> RoleConjunction^-(?y, ?x)
	 *
	 * @param roleConjunction
	 * @return
	 */
	public static Rule createRulePropagateInverseRoleConjunction(final OWLObjectPropertyExpression roleConjunction) {
		final OWLObjectPropertyExpression inverseRoleConjunction = roleConjunction.getInverseProperty();

		/* RoleConjunction(?x, ?y), N(?y) -> RoleConjunction^-(?y, ?x) */
		final Rule propagateInverseRule = new Rule(PropertytoAtom(inverseRoleConjunction, VAR_Y, VAR_X), PropertytoAtom(roleConjunction, VAR_X, VAR_Y),
				new Atom(Util.NAMED_PRED, Util.VAR_Y));
		return propagateInverseRule;
	}

	/* Rule (Role.2) */
	private void deriveRolesFromRoleSets() {
		program.getActiveRoleConjunctions().getRoleConjunctionsByName().forEach((roleConjunctionName, roleConjunction) -> {
			program.getRules().addAll(createRulesDeriveRolesFromRoleConjunction(roleConjunctionName, roleConjunction));

		});
	}

	/**
	 * Rule (Role.2) <br>
	 * RoleConjunction(?x, ?y) -> Role^-(?x, ?y), forall Role in RoleConjunction
	 *
	 * @param roleConjunctionName
	 * @param roleConjunction
	 * @return
	 */
	public static Set<Rule> createRulesDeriveRolesFromRoleConjunction(final OWLObjectPropertyExpression roleConjunctionName,
			final RoleConjunction roleConjunction) {
		final Set<Rule> rules = new HashSet<>();
		if (!roleConjunction.isSingleRole()) {
			roleConjunction.getRoleSet()
					.forEach(role -> rules.add(new Rule(PropertytoAtom(role, VAR_X, VAR_Y), PropertytoAtom(roleConjunctionName, VAR_X, VAR_Y))));
		}
		return rules;
	}

	/* Rules (6.1) and (6.2) */
	private void addRulesForRoleToSuperRolesClosure() {
		/* for all properties in the TBox, and their inverses */
		program.getSuperProperties().getSuperProperties().forEach((property, superPropertiesSet) -> {
			/* only add rule if role conjunction differs from role */
			if (!superPropertiesSet.equals(Sets.newHashSet(property))) {
				final OWLObjectPropertyExpression superRoleConjunction = program.getActiveRoleConjunctions().getRoleConjunctionName(superPropertiesSet);
				assertNotNull(superRoleConjunction);

				/* Role(?x, ?y) -> SuperRolesSet(?x, ?y) . */
				final Rule roleToSuperRoles = new Rule(PropertytoAtom(superRoleConjunction, VAR_X, VAR_Y), PropertytoAtom(property, VAR_X, VAR_Y));
				program.getRules().add(roleToSuperRoles);
			}
		});
	}

	/**
	 * N(?x) ->T(?x) .<br>
	 * We need to add this rule because (most of) the facts will not be processed trough java, but sent directly to RDFox in TTL files. For each named
	 * individual a, the facts contain an assertion NamedIndividual(a).
	 *
	 * @param program
	 */
	private void axiomatizeNamedIndividualToTop() {
		/* NamedIndividual(?x) -> OWL_THING(?x). */
		final Rule namedToTop = new Rule(AxiomsToRules.assertIndividualInTop(VAR_X), new Atom(Util.NAMED_PRED, VAR_X));
		this.program.getRules().add(namedToTop);
	}

	/**
	 * owl:differentFrom(?x,?x) -> Bottom(?x) .
	 */
	private void axiomatizeDifferentFrom() {
		final Rule ireflexiveDifferentFrom = new Rule(Converter.ConceptToAtom(InternalizedEntities.OWL_NOTHING, VAR_X),
				new Atom(OWLRDFVocabulary.OWL_DIFFERENT_FROM.toString(), VAR_X, VAR_X));
		program.getRules().add(ireflexiveDifferentFrom);
	}

	public Program getProgram() {
		return this.program;
	}

}
