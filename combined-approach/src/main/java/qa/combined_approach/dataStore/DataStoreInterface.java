package qa.combined_approach.dataStore;

import java.io.File;
import java.util.Map;
import java.util.Set;

import qa.combined_approach.rules.Query;
import uk.ac.ox.cs.JRDFox.JRDFoxException;
import uk.ac.ox.cs.JRDFox.store.DataStore.Format;

/**
 * Interface for the RDFox Data Store
 * 
 * @author Irina Dragoste
 *
 */
public interface DataStoreInterface {

	void importFiles(File... fileArray);

	void importText(String text);

	void reason();

	long countTriples();

	Set<Map<String, String>> answerQuery(Query query);

	Set<String> answerUnaryQuery(Query query);

	Set<String> answerSPARQLUnaryQuery(String sparqlQuery);

	void dispose();

	boolean answerASKQuery(String sparqlQuery);

	void export(File file, Format format) throws JRDFoxException;

}
