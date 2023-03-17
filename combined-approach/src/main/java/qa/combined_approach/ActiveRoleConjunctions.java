package qa.combined_approach;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * Collection of the active role conjunctions that are necessary to satisfy restrictions in the current state of the combined-approach materialisation. We add
 * role conjunctions on demand. Each role conjunction, represented as a set of roles ({@link OWLObjectPropertyExpression}s) is identified by a (in some cases,
 * fresh) role ({@link OWLObjectPropertyExpression}), which we call the role conjunction name.
 *
 * @author Irina Dragoste
 *
 */
public class ActiveRoleConjunctions {

	/**
	 * Map with the role conjuncts set as key, and the role conjunction name as value. Role conjunctions are unique per conjuncts set, and the role conjunction
	 * name can be retrieved by the role conjuncts set .
	 */
	private final Map<Set<OWLObjectPropertyExpression>, OWLObjectPropertyExpression> roleConjunctions = new HashMap<>();
	/**
	 * Map with the role conjunction name as key, and the role conjunction as value. Each unique role conjunct set has a unique name. Role conjunctions can be
	 * retrieved by name.
	 */
	private final Map<OWLObjectPropertyExpression, RoleConjunction> roleConjunctionsByName = new HashMap<>();

	/**
	 * Initializes the set of active role conjunctions from the transitive closure of the super-property relation, for each property occuring in the TBox, and
	 * its inverse.
	 *
	 * @param superProperties
	 *            super properties with computed transitive closure.
	 */
	public void initializeActiveRoleConjunctions(final SuperProperties superProperties) {

		/* put keys, and themselves */
		addSingleRoles(superProperties.getSuperProperties().keySet());

		superProperties.getSuperProperties().forEach((property, superRolesSet) -> {
			/* if the role conjunction already exists in the active sets, do nothing */
			if (!roleConjunctions.containsKey(superRolesSet)) {

				final OWLObjectPropertyExpression inverseProperty = property.getInverseProperty().getSimplified();
				final Set<OWLObjectPropertyExpression> inverseSuperRoleSet = superProperties.getSuperProperties(inverseProperty);

				final OWLObjectPropertyExpression roleConjunctionName;

				/*
				 * if the inverse role conjunction already exists in the active sets, invert its name
				 */
				if (roleConjunctions.containsKey(inverseSuperRoleSet)) {
					final OWLObjectPropertyExpression inverseRoleConjunctionName = roleConjunctions.get(inverseSuperRoleSet);
					roleConjunctionName = inverseRoleConjunctionName.getInverseProperty().getSimplified();

				} else {
					/* else, add new role set */
					roleConjunctionName = generateRoleConjunctionName();
				}

				final RoleConjunction roleConjunction = new RoleConjunction(roleConjunctionName, superRolesSet);
				roleConjunctions.put(roleConjunction.getRoleSet(), roleConjunction.getRoleName());
				roleConjunctionsByName.put(roleConjunction.getRoleName(), roleConjunction);
			}

		});
	}

	private void addSingleRoles(final Set<OWLObjectPropertyExpression> singleRoles) {
		singleRoles.forEach(role -> {
			final RoleConjunction singleRoleConjunction = new RoleConjunction(role, Sets.newHashSet(role));
			roleConjunctionsByName.put(role, singleRoleConjunction);
			roleConjunctions.put(singleRoleConjunction.getRoleSet(), role);
		});
	}

	/**
	 * Adds on demand a new active role conjunction.
	 *
	 * @param roleSet
	 * @return
	 */
	public RoleConjunction addRoleConjunction(final Set<OWLObjectPropertyExpression> roleSet) {
		/*
		 * the role conjunction must not already exist in the active sets .
		 */
		if (roleConjunctions.containsKey(roleSet)) {
			throw new CombinedApproachException("Expected a new role conjunction: " + roleSet);
		}

		/* add a new RoleConjunction to the active set . */
		final OWLObjectPropertyExpression roleConjunctionName;
		final Set<OWLObjectPropertyExpression> inverseRoleSet = invert(roleSet);

		/*
		 * if the inverse role conjunction already exists in the active sets, invert its name
		 */
		if (roleConjunctions.containsKey(inverseRoleSet)) {
			final OWLObjectPropertyExpression inverseRoleConjunction = roleConjunctions.get(inverseRoleSet);
			roleConjunctionName = inverseRoleConjunction.getInverseProperty().getSimplified();

		} else {
			/* else, add new role set with a new name */
			roleConjunctionName = generateRoleConjunctionName();
		}

		final RoleConjunction roleConjunction = new RoleConjunction(roleConjunctionName, roleSet);
		roleConjunctions.put(roleConjunction.getRoleSet(), roleConjunction.getRoleName());
		roleConjunctionsByName.put(roleConjunction.getRoleName(), roleConjunction);

		return roleConjunction;
	}

	/**
	 * Gets the inverse set of roles.
	 *
	 * @param roleSet
	 *            set of {@link OWLObjectPropertyExpression}s to be inverted.
	 * @return
	 */
	public static Set<OWLObjectPropertyExpression> invert(final Set<OWLObjectPropertyExpression> roleSet) {
		final Set<OWLObjectPropertyExpression> inverseRoleSet = new HashSet<>();
		roleSet.forEach(role -> inverseRoleSet.add(role.getInverseProperty().getSimplified()));
		return inverseRoleSet;
	}

	/**
	 * Returns the set of active role conjunctions containing given role.
	 *
	 * @param role
	 * @return
	 */
	public Set<OWLObjectPropertyExpression> getRoleConjunctionsContainingRole(final OWLObjectPropertyExpression role) {
		final Set<OWLObjectPropertyExpression> roleConjunctionsContainingRole = new HashSet<>();
		roleConjunctions.keySet().forEach(roleSet -> {
			if (roleSet.contains(role)) {
				final OWLObjectPropertyExpression roleConjunction = roleConjunctions.get(roleSet);
				roleConjunctionsContainingRole.add(roleConjunction);
			}
		});
		return roleConjunctionsContainingRole;
	}

	private OWLObjectPropertyImpl generateRoleConjunctionName() {
		return new OWLObjectPropertyImpl(IRI.create("RS_" + roleConjunctions.size()));
	}

	public OWLObjectPropertyExpression getRoleConjunctionName(final Set<OWLObjectPropertyExpression> roleSet) {
		return roleConjunctions.get(roleSet);
	}

	public Set<OWLObjectPropertyExpression> getRoleConjunctionByName(final OWLObjectPropertyExpression roleConjunctionName) {
		return this.roleConjunctionsByName.get(roleConjunctionName).getRoleSet();
	}

	public Map<OWLObjectPropertyExpression, RoleConjunction> getRoleConjunctionsByName() {
		return this.roleConjunctionsByName;
	}

	public Set<OWLObjectPropertyExpression> getRoleConjunctionNames() {
		return roleConjunctionsByName.keySet();
	}

}
