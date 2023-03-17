package qa.combined_approach;

import java.io.File;
import java.util.Set;

import org.junit.Test;

import qa.combined_approach.dataStore.DataStoreInterface;
import qa.combined_approach.dataStore.RDFoxDataStoreWrapper;
import qa.combined_approach.rules.Atom;
import qa.combined_approach.rules.Query;

public class TestFactsResultingFromRDFoxABox {

	private static final String pathToTestFile = "src/test/data/sample.ttl";

	@Test
	public void testIndividualsAreInClassNamed() {
		final DataStoreInterface dataStore = new RDFoxDataStoreWrapper();
		dataStore.importFiles(new File(pathToTestFile));
		dataStore.reason();

		final Query query = new Query("?x", new Atom(Util.NAMED_PRED, "?x"));
		final Set<String> answerUnaryQuery = dataStore.answerUnaryQuery(query);
		answerUnaryQuery.forEach(System.out::println);

		System.out.println("Number of named:" + answerUnaryQuery.size());
		System.out.println("Fact size: " + dataStore.countTriples());
		dataStore.dispose();

		// dataStore.
	}

}
