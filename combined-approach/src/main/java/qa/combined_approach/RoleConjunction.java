package qa.combined_approach;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class RoleConjunction {

	private final OWLObjectPropertyExpression roleName;
	private final Set<OWLObjectPropertyExpression> roleSet = new HashSet<>();

	public RoleConjunction(final OWLObjectPropertyExpression roleName, final Set<OWLObjectPropertyExpression> roleSet) {
		super();
		this.roleName = roleName;
		this.roleSet.addAll(roleSet);
	}

	public boolean isSingleRole() {
		return roleSet.size() == 1;
	}

	/**
	 * A unique role conjunction (i.e. set of role conjuncts) is identified by a unique name, in the form of an OWLObjectPropertyExpression. If a role
	 * conjunction contains a single role, its name is that of the role. If one role conjunction is the inverse of another, its name is also the inverse
	 * property of the other's name.
	 *
	 * @return
	 */
	public OWLObjectPropertyExpression getRoleName() {
		return this.roleName;
	}

	/**
	 * The set of role conjuncts that uniquely identify this role conjunction.
	 *
	 * @return
	 */
	public Set<OWLObjectPropertyExpression> getRoleSet() {
		return this.roleSet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.roleSet.hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RoleConjunction)) {
			return false;
		}
		final RoleConjunction other = (RoleConjunction) obj;
		return this.roleSet.equals(other.roleSet);
	}

}
