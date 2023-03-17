package qa.combined_approach;

import java.io.FileNotFoundException;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import qa.combined_approach.main.TrimToHornALCHOIQ;

public class TestTrimToHornALCHOIQ {

	@Ignore
	@Test
	public void trimReactome() throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String base = "D:\\phd\\env\\combined-approach\\KR\\ontos\\";
		final String ontologyFilePath = base + "2_TBoxes_Normalized_minCard\\normalized_minCard_manually_Reactome.owl";
		final String trimmedOntologyFolder = base + "3_TBoxes_Normalized_minCard_Trimed_HornAlchoiq";
		TrimToHornALCHOIQ.trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

	@Ignore
	@Test
	public void trimUniprot() throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String base = "D:\\phd\\env\\combined-approach\\KR\\ontos\\";
		final String ontologyFilePath = base + "2_TBoxes_Normalized_minCard\\normalized_minCard_manually_Uniprot.owl";
		final String trimmedOntologyFolder = base + "3_TBoxes_Normalized_minCard_Trimed_HornAlchoiq";
		TrimToHornALCHOIQ.trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

	@Ignore
	@Test
	public void trimLUBM() throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String base = "D:\\phd\\env\\combined-approach\\KR\\ontos\\";
		final String ontologyFilePath = base + "2_TBoxes_Normalized_minCard\\normalized_minCard_manually_LUBM.owl";
		final String trimmedOntologyFolder = base + "3_TBoxes_Normalized_minCard_Trimed_HornAlchoiq";
		TrimToHornALCHOIQ.trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

	@Ignore
	@Test
	public void trimUOBM() throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String base = "D:\\phd\\env\\combined-approach\\KR\\ontos\\";
		final String ontologyFilePath = base + "2_TBoxes_Normalized_minCard\\normalized_minCard_manually_UOBM.owl";
		final String trimmedOntologyFolder = base + "3_TBoxes_Normalized_minCard_Trimed_HornAlchoiq";
		TrimToHornALCHOIQ.trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

	@Test
	public void trimFirstPaperExample() throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
		final String base = "src\\test\\data\\paper_example\\";
		final String ontologyFilePath = base + "norm-paper_example.owl";
		final String trimmedOntologyFolder = base + "owlxml";
		TrimToHornALCHOIQ.trimToHornALCHOIQ(ontologyFilePath, trimmedOntologyFolder);
	}

}
