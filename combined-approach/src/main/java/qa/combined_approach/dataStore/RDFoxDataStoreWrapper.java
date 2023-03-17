package qa.combined_approach.dataStore;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qa.combined_approach.CombinedApproachException;
import qa.combined_approach.rules.Query;
import uk.ac.ox.cs.JRDFox.JRDFoxException;
import uk.ac.ox.cs.JRDFox.store.DataStore;
import uk.ac.ox.cs.JRDFox.store.DataStore.Format;
import uk.ac.ox.cs.JRDFox.store.TupleIterator;

/**
 * Wrapper over RDFox DataStore
 *
 * @author Irina Dragoste
 *
 */
// TODO try with resources
public class RDFoxDataStoreWrapper implements DataStoreInterface {

	private DataStore dataStore;

	/**
	 * Clears the RDFox dataStore resource
	 */
	@Override
	protected void finalize() throws Throwable {
		dataStore.dispose();
		super.finalize();
	}

	@Override
	public void dispose() {
		dataStore.dispose();
	}

	public RDFoxDataStoreWrapper() {
		this(new DataStoreConfiguration());
	}

	public RDFoxDataStoreWrapper(final DataStoreConfiguration config) {
		try {
			dataStore = new DataStore(config.getRdfoxDataStoreType());
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException("Error while trying to create an RDFox DataStore", e);
		}
		try {
			dataStore.setNumberOfThreads(config.getRdfoxDataStoreThreads());
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException("Error while trying to set the number of threads to an RDFox DataStore ", e);
		}
	}

	@Override
	public void importFiles(final File... fileArray) {
		try {
			dataStore.importFiles(fileArray);
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException("Error while trying to import files [" + fileArray + "] to RDFox DataStore", e);
		}
	}

	@Override
	public void importText(final String text) {
		try {
			dataStore.importText(text);
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException(
					"Error while trying to import text to RDFox DataStore: " + ((text.length() < 50) ? text : text.substring(0, 50) + " ..."), e);
		}
	}

	@Override
	public void reason() {
		try {
			dataStore.applyReasoning();
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException("Error while trying to apply reasoning on RDFox DataStore", e);

		}
	}

	@Override
	public long countTriples() {
		try {
			return dataStore.getTriplesCount();
		} catch (final JRDFoxException e) {
			throw new CombinedApproachException("Error while trying to count triples for RDFox DataStore", e);
		}
	}

	@Override
	public Set<Map<String, String>> answerQuery(final Query query) {
		final Set<Map<String, String>> substitutions = new HashSet<>();
		final String sparqlQuery = query.toSPARQLQuery();
		TupleIterator tupleIterator;
		try {
			tupleIterator = dataStore.compileQuery(sparqlQuery);
		} catch (final JRDFoxException e1) {
			throw new RuntimeException("Error while query answering!", e1);
		}
		try {
			for (long multiplicity = tupleIterator.open(); multiplicity != 0; multiplicity = tupleIterator.advance()) {
				// multiplicity = number of possible answers to the query (substitutions)
				final Map<String, String> substitution = new HashMap<>();
				// arity = number o variables
				for (int i = 0; i < tupleIterator.getArity(); i++) {
					if (tupleIterator.getArity() != query.getQuerryVariables().length) {
						throw new RuntimeException("Error while query answering! answer does no match number of variables queried for.");
					}
					substitution.put(query.getQuerryVariables()[i].toString(), tupleIterator.getResource(i).toString());
				}
				substitutions.add(substitution);
			}
		} catch (final JRDFoxException e) {
			throw new RuntimeException("Error while query answering!", e);
		} finally {
			tupleIterator.dispose();
		}
		return substitutions;
	}

	@Override
	public Set<String> answerUnaryQuery(final Query query) {
		final Set<String> answers = new HashSet<String>();
		TupleIterator existTupleIterator;
		try {
			existTupleIterator = dataStore.compileQuery(query.toSPARQLQuery());
		} catch (final JRDFoxException e) {
			throw new RuntimeException("Error while query answering!", e);
		}
		try {
			for (long multiplicity = existTupleIterator.open(); multiplicity != 0; multiplicity = existTupleIterator.advance()) {
				answers.add(existTupleIterator.getResource(0).toString());
			}
		} catch (final JRDFoxException e1) {
			throw new RuntimeException("Error while query answering!", e1);
		} finally {
			existTupleIterator.dispose();
		}
		return answers;
	}

	@Override
	public Set<String> answerSPARQLUnaryQuery(final String sparqlQuery) {
		final Set<String> answers = new HashSet<String>();
		TupleIterator existTupleIterator;
		try {
			existTupleIterator = dataStore.compileQuery(sparqlQuery);
		} catch (final JRDFoxException e) {
			throw new RuntimeException("Error while query answering!", e);
		}
		try {
			for (long multiplicity = existTupleIterator.open(); multiplicity != 0; multiplicity = existTupleIterator.advance()) {
				answers.add(existTupleIterator.getResource(0).toString());
			}
		} catch (final JRDFoxException e1) {
			throw new RuntimeException("Error while query answering!", e1);
		} finally {
			existTupleIterator.dispose();
		}
		return answers;
	}

	@Override
	public boolean answerASKQuery(final String sparqlQuery) {
		final boolean answer;
		TupleIterator existTupleIterator;
		try {
			existTupleIterator = dataStore.compileQuery(sparqlQuery);
		} catch (final JRDFoxException e) {
			throw new RuntimeException("Error while query answering!", e);
		}
		try {
			final long multiplicity = existTupleIterator.open();
			answer = multiplicity != 0;

		} catch (final JRDFoxException e1) {
			throw new RuntimeException("Error while query answering!", e1);
		} finally {
			existTupleIterator.dispose();
		}
		return answer;
	}

	@Override
	public void export(final File file, final Format format) throws JRDFoxException {
		dataStore.export(file, format);
	}

}
