package qa.combined_approach.rules;

import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class Atom {

	private String predicate;
	private String[] args;

	public Atom(String predicate, String... args) {
		this.predicate = predicate;
		this.args = args;
	}

	public boolean isUnary() {
		return args.length == 1;
	}

	public String toTurtleAtom() {
		if (isUnary())
			return args[0] + " <" + OWLRDFVocabulary.RDF_TYPE + "> <" + predicate + "> .";
		else
			return args[0] + " <" + predicate + "> " + args[1] + " .";
	}

	public String toRDFOxFormat() {
		if (isUnary())
			return "<" + predicate + ">(" + args[0] + ")";
		else
			return "<" + predicate + ">(" + args[0] + ", " + args[1] + ")";
	}

	public String getPredicate() {
		return predicate;
	}

	public String[] getArgs() {
		return args;
	}

}
