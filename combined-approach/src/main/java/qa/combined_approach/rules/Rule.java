package qa.combined_approach.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import uk.ac.manchester.cs.owl.owlapi.InternalizedEntities;

public class Rule {

	private final Atom[] body;
	private final Atom[] head;

	public Rule(final Atom[] head, final Atom[] body) {
		this.head = eliminateTop(head);
		this.body = eliminateTop(body);
	}

	public Rule(final Atom head, final Atom... body) {
		this.head = new Atom[] { head };
		this.body = eliminateTop(body);
	}

	public Rule(final Atom head, final Atom body) {
		this.head = new Atom[] { head };
		this.body = new Atom[] { body };
	}

	public String toRDFOxFormat() {
		final StringBuilder rdfoxFormattedRule = new StringBuilder();
		for (final Atom headCounjunct : head) {
			rdfoxFormattedRule.append(headCounjunct.toRDFOxFormat()).append(" :- ");
			for (int i = 0; i < body.length; i++) {
				rdfoxFormattedRule.append(body[i].toRDFOxFormat());
				if (i < body.length - 1) {
					rdfoxFormattedRule.append(", ");
				}
			}
			rdfoxFormattedRule.append(" .\n");
		}
		return rdfoxFormattedRule.toString();
	}

	public Atom[] getHead() {
		return head;
	}

	public Atom[] getBody() {
		return body;
	}

	public static Atom[] eliminateTop(final Atom[] atomSet) {
		boolean changed = false;
		final List<Atom> atomSetCopy = new ArrayList<>(Arrays.asList(atomSet));
		final Iterator<Atom> iterator = atomSetCopy.iterator();
		while (iterator.hasNext()) {
			final Atom atom = iterator.next();
			if (atom.getPredicate().equals(InternalizedEntities.OWL_THING.toStringID()) && atom.getArgs().length == 1) {
				if (atomSetCopy.size() > 1) {
					iterator.remove();
					changed = true;
				}
			}
		}

		if (changed) {
			return atomSetCopy.toArray(new Atom[atomSetCopy.size()]);
		} else {
			return atomSet;
		}
	}
}
