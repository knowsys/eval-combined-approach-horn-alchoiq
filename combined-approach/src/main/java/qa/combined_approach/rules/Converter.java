package qa.combined_approach.rules;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class Converter {

	public static Atom ConceptToAtom(OWLClassExpression classExpression, OWLIndividual individual) {
		return ConceptToAtom(classExpression, IndividualToString(individual));
	}


	public static Atom ConceptToAtom(OWLClassExpression classExpression, String variable) {
		return new Atom(getConceptName(classExpression), variable);
	}

	public static Atom PropertytoAtom(OWLObjectPropertyExpression propertyExpression, OWLIndividual subject, OWLIndividual object) {
		return PropertytoAtom(propertyExpression, IndividualToString(subject), IndividualToString(object));
	}

	public static Atom PropertytoAtom(OWLObjectPropertyExpression propertyExpression, String subjectVariable, String objectVariable) {
		return new Atom(getRoleName(propertyExpression), subjectVariable, objectVariable);
	}

	public static String getRoleName(OWLObjectPropertyExpression propertyExpression) {
		OWLObjectPropertyExpression simplified = propertyExpression.getSimplified();
		if (simplified.isOWLObjectProperty()) {
			return simplified.asOWLObjectProperty().toStringID();
		} else {
			OWLObjectPropertyExpression inverseProperty = simplified.getInverseProperty().getSimplified();
			return "INV_" + inverseProperty.asOWLObjectProperty().toStringID();
		}
	}
	
	public static String getConceptName(OWLClassExpression classExpression) {
		return classExpression.asOWLClass().toStringID();
	}

	public static String IndividualToString(OWLIndividual individual) {
		return "<" + individual.toStringID() + ">";
	}
}
