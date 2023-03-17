package qa.combined_approach;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

/**
 * Class for computing the transitive closure of the reflexive super property relation.
 *
 * @author Irina Dragoste
 *
 */
public class SuperProperties {

	private final Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> superProperties = new HashMap<>();

	/**
	 * Gets the map containing as keys each property (and its inverse) occurring in the ontology TBox axioms, and as value the super properties of the key
	 * property. Note that the super property relation is reflexive and transitive. It is inferred from axiom (6) - rules (6.1) and (6.2).
	 *
	 * @return
	 */
	public Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> getSuperProperties() {
		return this.superProperties;
	}

	/**
	 * Gets the super properties of given property. Note that the super property relation is reflexive and transitive. It is inferred from axiom (6) - rules
	 * (6.1) and (6.2).
	 *
	 * @param property
	 * @return
	 */
	public Set<OWLObjectPropertyExpression> getSuperProperties(final OWLObjectPropertyExpression property) {
		return superProperties.get(property.getSimplified());
	}

	/**
	 * Each property occurring in the ontology TBox axioms is collected, as well as its inverse.
	 *
	 * @param objectPropertyExpression
	 */
	public void addPropertyAndItsInverse(final OWLObjectPropertyExpression objectPropertyExpression) {
		superProperties.putIfAbsent(objectPropertyExpression.getSimplified(), new HashSet<>());
		superProperties.putIfAbsent(objectPropertyExpression.getInverseProperty().getSimplified(), new HashSet<>());
	}

	/**
	 * For each axiom of type (6), the sub and super properties are collected.
	 *
	 * @param subProperty
	 * @param superProperty
	 */
	public void addPropertySubsumptionAndItsInverse(final OWLObjectPropertyExpression subProperty, final OWLObjectPropertyExpression superProperty) {
		add(subProperty.getSimplified(), superProperty.getSimplified());
		add(subProperty.getInverseProperty().getSimplified(), superProperty.getInverseProperty().getSimplified());
	}

	/**
	 * Computes the super properties of all added properties. The superProperty relation is reflexive and transitive.
	 */
	public void computeSuperProperties() {
		computeTransitiveClosureOfPropertySubsumption();
		addReflexivePropertySubsumption();
	}

	private void computeTransitiveClosureOfPropertySubsumption() {
		boolean changed = true;
		while (changed) {
			boolean changedThisRound = false;
			for (final OWLObjectPropertyExpression key : superProperties.keySet()) {
				final Set<OWLObjectPropertyExpression> superPropertiesToAdd = new HashSet<>();
				for (final OWLObjectPropertyExpression superPropOfKey : superProperties.get(key)) {
					for (final OWLObjectPropertyExpression superPropOfSuperPropOfKey : superProperties.get(superPropOfKey)) {
						superPropertiesToAdd.add(superPropOfSuperPropOfKey);
					}
				}
				changedThisRound = changedThisRound || superProperties.get(key).addAll(superPropertiesToAdd);
			}
			changed = changed & changedThisRound;
		}
	}

	private void addReflexivePropertySubsumption() {
		for (final OWLObjectPropertyExpression key : superProperties.keySet()) {
			final Set<OWLObjectPropertyExpression> superPropsOfKey = superProperties.get(key);
			superPropsOfKey.add(key);
		}
	}

	private void add(final OWLObjectPropertyExpression subProperty, final OWLObjectPropertyExpression superProperty) {
		superProperties.get(subProperty).add(superProperty);
	}

}
