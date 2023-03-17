package qa.combined_approach;

import java.io.File;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class TestCompareResultingAssertions {

	String base = "d:\\phd\\env\\combined-approach\\KR\\compareResults\\";

	@Test
	public void testLUBM025() throws OWLOntologyCreationException {
		final String folder = base + File.separator + "LUBM025";
		final File a = new File(folder + File.separator + "LUBM025_konclude.ttl");
		final File b = new File(folder + File.separator + "LUBM025_rdfox.nt");
		CompareFacts.areAFactsContainedInB(a, b);
	}

}
