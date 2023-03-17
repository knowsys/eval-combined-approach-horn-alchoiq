package qa.combined_approach.rules;

public class Query {

	private final String[] querryVariables;
	public Atom[] body;
	private String filter = "";

	public Query(final String inputAnswerVar, final Atom inputBodyAtom) {
		querryVariables = new String[] { inputAnswerVar };
		body = new Atom[] { inputBodyAtom };
	}

	public Query(final String inputAnswerVar, final Atom... inputBody) {
		querryVariables = new String[] { inputAnswerVar };
		body = inputBody;
	}

	public Query(final String inputAnswerVar1, final String inputAnswerVar2, final Atom... inputBody) {
		querryVariables = new String[] { inputAnswerVar1, inputAnswerVar2 };
		body = inputBody;
	}

	public Query(final String[] inputAnswerVar, final Atom... inputBody) {
		querryVariables = inputAnswerVar;
		body = inputBody;
	}

	public String toSPARQLQuery() {
		String sparqlQuery = "SELECT DISTINCT ";
		for (final String answerVar : querryVariables) {
			sparqlQuery += answerVar + " ";
		}
		sparqlQuery += "WHERE {\n";
		for (final Atom bodyAtom : body) {
			sparqlQuery += bodyAtom.toTurtleAtom() + "\n";
		}
		sparqlQuery += filter + "\n}";
		return sparqlQuery;
	}

	public String[] getQuerryVariables() {
		return querryVariables;
	}

	public Atom[] getBody() {
		return body;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(final String filter) {
		this.filter = filter;
	}

}
