package qa.combined_approach;

import static qa.combined_approach.Util.VAR_X;
import static qa.combined_approach.Util.VAR_Y;
import static qa.combined_approach.Util.VAR_Z;
import static qa.combined_approach.rules.Converter.ConceptToAtom;
import static qa.combined_approach.rules.Converter.IndividualToString;
import static qa.combined_approach.rules.Converter.PropertytoAtom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.Converter;
import qa.combined_approach.rules.HornAlchoiqAxiomType;
import qa.combined_approach.rules.Rule;
import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

public class AxiomsToRules {

	/**
	 * Transforms given axiom to rules in the program. Some rules (2), (3.2), (4.2), (4.3) will be obtained iteratively during materialisation.
	 *
	 * @param axiom
	 * @param program
	 */
	public static void collectRules(final OWLAxiom axiom, final Program program) {
		/* transform some axioms to rules, some axioms we deal with later */
		switch (axiom.getAxiomType().getName()) {
			case "SubObjectPropertyOf": /* axiom (6): R sqs S */
				program.incrementAxiomCount(HornAlchoiqAxiomType.ROLE_SUBSUMPTION);

				final OWLSubObjectPropertyOfAxiom subPropertyOfAxiom = (OWLSubObjectPropertyOfAxiom) axiom;
				final OWLObjectPropertyExpression superProperty = subPropertyOfAxiom.getSuperProperty().getSimplified();
				final OWLObjectPropertyExpression subProperty = subPropertyOfAxiom.getSubProperty().getSimplified();

				program.addPropertyAndItsInverse(subProperty);
				program.addPropertyAndItsInverse(superProperty);

				/* prepare for rules (6.1), (6.2) */
				program.addPropertySubsumptionAndItsInverse(subProperty, superProperty);

				break;
			case "SubClassOf":
				final OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom).getSubClass();
				final OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom).getSuperClass();
				switch (superClass.getClassExpressionType()) {
					case OBJECT_ONE_OF: /* axiom (5): Nominal */
						final OWLObjectOneOf nominals = (OWLObjectOneOf) superClass;
						if (nominals.individuals().count() < 1) {
							throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
						}
						if (nominals.individuals().count() > 1) {
							throw new CombinedApproachException("Not Horn-ALCHOIQ :" + axiom.getAxiomType() + " :" + axiom); // non Horn
						}
						nominals.individuals().forEach(individual -> {
							program.incrementAxiomCount(HornAlchoiqAxiomType.NOMINAL);
							/* Rule (5): C(x) -> a = x. */
							program.getRules().add(new Rule(new Atom(OWLRDFVocabulary.OWL_SAME_AS.toString(), IndividualToString(individual), VAR_X),
									ConceptToAtom(subClass, VAR_X)));
							/* N(a). */
							program.getFacts().add(new Atom(Util.NAMED_PRED, IndividualToString(individual)));

						});
						break;
					case OBJECT_MAX_CARDINALITY: /* axiom (4): C subseteq <=1 S.D */
						final OWLObjectMaxCardinality maxCardinality = (OWLObjectMaxCardinality) superClass;
						final OWLClassExpression maxCardRange = maxCardinality.getFiller();
						if (maxCardinality.getCardinality() < 1) {
							throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
						}
						if (maxCardinality.getCardinality() > 1) {
							throw new CombinedApproachException("Not Horn-ALCHOIQ :" + axiom.getAxiomType() + " :" + axiom); // non Horn
						}
						program.incrementAxiomCount(HornAlchoiqAxiomType.AT_MOST_ONE);

						final OWLObjectPropertyExpression maxCardProperty = maxCardinality.getProperty().getSimplified();
						program.addPropertyAndItsInverse(maxCardProperty);

						/* Rule (4.1): D(y), R^-(y,x), C(x), R(x,z), D(z), N(z) -> y=z . */
						program.getRules()
								.add(new Rule(new Atom(OWLRDFVocabulary.OWL_SAME_AS.toString(), VAR_Y, VAR_Z),
										new Atom[] { ConceptToAtom(maxCardRange, VAR_Y), PropertytoAtom(maxCardProperty.getInverseProperty(), VAR_Y, VAR_X),
												ConceptToAtom(subClass, VAR_X), PropertytoAtom(maxCardProperty, VAR_X, VAR_Z),
												ConceptToAtom(maxCardRange, VAR_Z), new Atom(Util.NAMED_PRED, VAR_Z) }));
						// TODO optimisation: this rule is needed only if there are OBJECT_ONE_OF axioms in the TBox.
						/* Rule (4.4): D(y), R^-(y,x), C(x), N(x) -> N(y) . */
						program.getRules()
								.add(new Rule(new Atom(Util.NAMED_PRED, VAR_Y),
										new Atom[] { ConceptToAtom(maxCardRange, VAR_Y), PropertytoAtom(maxCardProperty.getInverseProperty(), VAR_Y, VAR_X),
												ConceptToAtom(subClass, VAR_X), new Atom(Util.NAMED_PRED, VAR_X) }));

						program.addUnprocessedAxiom(HornAlchoiqAxiomType.AT_MOST_ONE, axiom);
						break;
					case OBJECT_MIN_CARDINALITY: /* axiom (2): C subseteq exists S.D */
						final OWLObjectMinCardinality minCardinality = (OWLObjectMinCardinality) superClass;
						final int cardinality = minCardinality.getCardinality();
						if (cardinality > 1) {
							throw new CombinedApproachException("Unexpected min cardinality:" + cardinality + " axiom :" + axiom);
						} else if (cardinality == 1) {
							program.incrementAxiomCount(HornAlchoiqAxiomType.EXISTS);
							final OWLObjectPropertyExpression minCardProperty = minCardinality.getProperty().getSimplified();

							program.addPropertyAndItsInverse(minCardProperty);

							program.addUnprocessedAxiom(HornAlchoiqAxiomType.EXISTS, axiom);
						}
						/* ignore ontologies with axioms with cardinality <1 */
						break;
					case OBJECT_ALL_VALUES_FROM: /* axiom (3): (C subseteq forall S.D) equivalent with ( exists S.C subseteq D) */
						program.incrementAxiomCount(HornAlchoiqAxiomType.FOR_ALL_ROLES_FROM_DOMAIN);
						final OWLObjectAllValuesFrom allValsFrom = (OWLObjectAllValuesFrom) superClass;
						final OWLClassExpression allValsFromRange = allValsFrom.getFiller();
						final OWLObjectPropertyExpression allValsFromProperty = allValsFrom.getProperty().getSimplified();

						program.addPropertyAndItsInverse(allValsFromProperty);

						/* Rule (3.1): S^-(x,y), C(y) -> D(x) . */
						program.getRules().add(new Rule(ConceptToAtom(allValsFromRange, VAR_X),
								PropertytoAtom(allValsFromProperty.getInverseProperty(), VAR_X, VAR_Y), ConceptToAtom(subClass, VAR_Y)));

						program.addUnprocessedAxiom(HornAlchoiqAxiomType.FOR_ALL_ROLES_FROM_DOMAIN, axiom);
						break;
					case OWL_CLASS:
					case OBJECT_UNION_OF: // A1 cap ... cap An sqs B1 cup ... cup Bm
						final int superClassSize = superClass.asDisjunctSet().size();
						if (superClassSize < 1) {
							throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
						}
						if (superClassSize > 1) {
							throw new CombinedApproachException("Not Horn-ALCHOIQ :" + axiom.getAxiomType() + " :" + axiom); // non Horn
						}
						switch (subClass.getClassExpressionType()) {
							case OBJECT_INTERSECTION_OF:
							case OWL_CLASS: /* axiom (1): A1 cap ... cap An sqs B */
								program.incrementAxiomCount(HornAlchoiqAxiomType.SUBSUME_CONCEPT_CONJUNCTION);
								final List<OWLClassExpression> subClassConjuncts = new ArrayList<OWLClassExpression>(subClass.asConjunctSet());
								final Atom[] intersectionAxiomBody = new Atom[subClassConjuncts.size()];
								for (int i = 0; i < subClassConjuncts.size(); i++) {
									intersectionAxiomBody[i] = ConceptToAtom(subClassConjuncts.get(i), VAR_X);
								}
								/* Rule (1): A1(x) , A2(x), ... An(x) -> B(?x) */
								program.getRules().add(new Rule(ConceptToAtom(superClass, VAR_X), intersectionAxiomBody));

								break;
							default:
								throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
						}
						break;
					default:
						throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
				}
				break;
			default:
				throw new CombinedApproachException("Unexpected T-box axiom of type:" + axiom.getAxiomType() + " :" + axiom);
		}

	}

	public static void collectAboxAssertions(final OWLAxiom axiom, final Program program, final Set<String> namedIndividuals) {

		final AxiomType<?> axiomType = axiom.getAxiomType();

		if (AxiomType.ABoxAxiomTypes.contains(axiomType)) {
			if (axiomType.equals(AxiomType.CLASS_ASSERTION)) { /* axiom (7) */
				program.incrementAxiomCount(HornAlchoiqAxiomType.CONCEPT_ASSERTION);
				final OWLClassAssertionAxiom classAssertionAxiom = (OWLClassAssertionAxiom) axiom;
				/* Rule (7): class(individual) */
				program.getFacts().add(Converter.ConceptToAtom(classAssertionAxiom.getClassExpression(), classAssertionAxiom.getIndividual()));
				namedIndividuals.add(IndividualToString(classAssertionAxiom.getIndividual()));

			} else if (axiomType.equals(AxiomType.SAME_INDIVIDUAL)) {
				((OWLSameIndividualAxiom) axiom).asPairwiseAxioms().forEach(sameIndividualsAxiom -> {
					program.incrementAxiomCount(HornAlchoiqAxiomType.SAME_INDIVIDUAL_ASSERTION);
					final OWLIndividual firstIndividual = sameIndividualsAxiom.getIndividualsAsList().get(0);
					final OWLIndividual secondIndividual = sameIndividualsAxiom.getIndividualsAsList().get(1);
					/* sameAs (firstIndividual,secondIndividual) . */
					program.getFacts()
							.add(new Atom(OWLRDFVocabulary.OWL_SAME_AS.toString(), IndividualToString(firstIndividual), IndividualToString(secondIndividual)));
					namedIndividuals.add(IndividualToString(firstIndividual));
					namedIndividuals.add(IndividualToString(secondIndividual));
				});

			} else if (axiomType.equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
				((OWLDifferentIndividualsAxiom) axiom).asPairwiseAxioms().forEach(diffIndividualsAxiom -> {
					program.incrementAxiomCount(HornAlchoiqAxiomType.DIFFERENT_INDIVIDUALS_ASSERTION);
					final OWLIndividual firstIndividual = diffIndividualsAxiom.getIndividualsAsList().get(0);
					final OWLIndividual secondIndividual = diffIndividualsAxiom.getIndividualsAsList().get(1);
					/* differentFrom (firstIndividual,secondIndividual) . */
					program.getFacts().add(new Atom(OWLRDFVocabulary.OWL_DIFFERENT_FROM.toString(), IndividualToString(firstIndividual),
							IndividualToString(secondIndividual)));
					namedIndividuals.add(IndividualToString(firstIndividual));
					namedIndividuals.add(IndividualToString(secondIndividual));
				});
			} else if (axiomType.equals(AxiomType.OBJECT_PROPERTY_ASSERTION)) { /* axiom (*) */
				program.incrementAxiomCount(HornAlchoiqAxiomType.ROLE_ASSERTION);
				final OWLObjectPropertyAssertionAxiom propertyAssertionAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
				final OWLObjectPropertyExpression property = propertyAssertionAxiom.getProperty().getSimplified();
				/* Rule (8): property(subj,obj) . */
				program.getFacts().add(Converter.PropertytoAtom(property, propertyAssertionAxiom.getSubject(), propertyAssertionAxiom.getObject()));
				namedIndividuals.add(IndividualToString(propertyAssertionAxiom.getObject()));
				namedIndividuals.add(IndividualToString(propertyAssertionAxiom.getSubject()));
			} else {
				throw new CombinedApproachException("Untreated A-box axiom of type:" + axiomType + " :" + axiom);
			}
		}

	}

	/**
	 * Creates Atom owl:Thing(owlIndividual)
	 *
	 * @param owlIndividual
	 * @return
	 */
	public static Atom assertIndividualInTop(final OWLIndividual owlIndividual) {
		return Converter.ConceptToAtom(InternalizedEntities.OWL_THING, owlIndividual);
	}

	/**
	 * Creates Atom owl:Thing(termName)
	 *
	 * @param termName
	 * @return
	 */
	public static Atom assertIndividualInTop(final String termName) {
		return Converter.ConceptToAtom(InternalizedEntities.OWL_THING, termName);
	}

}
