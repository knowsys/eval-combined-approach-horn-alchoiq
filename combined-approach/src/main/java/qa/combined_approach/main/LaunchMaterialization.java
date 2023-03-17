package qa.combined_approach.main;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import qa.combined_approach.Materialization;
import qa.combined_approach.OntologyToProgram;
import qa.combined_approach.Program;
import qa.combined_approach.Util;
import qa.combined_approach.dataStore.DataStoreConfiguration;
import uk.ac.ox.cs.JRDFox.JRDFoxException;

/**
 * Main class that launches the Materialisation of our combined-approach propotype.
 *
 * @author Irina Dragoste
 *
 */
public class LaunchMaterialization {

	public static void main(final String[] args) throws OWLOntologyCreationException, IOException, JRDFoxException {

		/**
		 * Path to a Horn-ALCHOIQ OWL ontology with axioms in the normal form as discussed in the paper.
		 */
		final String ontoPath = args[0];

		/**
		 * Path to the folder where rules files and facts files will be generated, to be imported into RDFox.
		 */
		final String rdfoxInputLocation = args[1];

		/**
		 * Path to the folder where the ABox of the ontology is, in the form of several Turtle (.ttl) files. These files will be imported directly into RDFox,
		 * without being processed trough Java. For each named individual in the ABox files, there is a class assertion to <code>owl:amedIndividual</code>
		 * class.
		 */
		final String aboxFolderLocation = args[2];

		/**
		 * Number of parallel threads to be used by RDFox.
		 */
		final Integer nbThreads = Integer.valueOf(args[3]);

		String exportToFolderLocation = null;

		if (args.length > 4) {
			/**
			 * if present, the location where the facts inferred by materialisation will be exported.
			 */
			exportToFolderLocation = args[4];
		}

		materialize(ontoPath, new File(rdfoxInputLocation), new File(aboxFolderLocation), nbThreads, exportToFolderLocation);
	}

	/**
	 *
	 * @param ontoPath
	 *            Path to a Horn-ALCHOIQ OWL ontology with axioms in the normal form as discussed in the paper.
	 * @param rdfoxInputLocation
	 *            Path to the folder where rules files and facts files will be generated, to be imported into RDFox.
	 * @param aboxFolderLocation
	 *            Path to the folder where the ABox of the ontology is, in the form of several Turtle (.ttl) files. These files will be imported directly into
	 *            RDFox, without being processed trough Java. For each named individual in the ABox files, there is a class assertion to
	 *            <code>owl:amedIndividual</code> class.
	 * @param nbThreads
	 *            Number of parallel threads to be used by RDFox.
	 * @param exportToFolderLocation
	 *            if not null, the location where the facts inferred by materialisation will be exported.
	 *
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws JRDFoxException
	 */
	public static void materialize(final String ontoPath, final File rdfoxInputLocation, final File aboxFolderLocation, final int nbThreads,
			final String exportToFolderLocation) throws OWLOntologyCreationException, IOException, JRDFoxException {
		// load
		final long startTime = System.currentTimeMillis();
		final DataStoreConfiguration dataStoreConfiguration = new DataStoreConfiguration();
		dataStoreConfiguration.setRdfoxDataStoreThreads(nbThreads);

		System.out.println("Evaluating ontology: " + ontoPath + "; Time: " + LocalDate.now() + " " + LocalTime.now());
		final OWLOntology ontology = Util.loadOntologyWithImports(new File(ontoPath));
		final OntologyToProgram ontologyToProgram = new OntologyToProgram(ontology);
		final Program program = ontologyToProgram.getProgram();

		// materialize
		final Materialization materialization = new Materialization(rdfoxInputLocation, aboxFolderLocation, dataStoreConfiguration, program,
				exportToFolderLocation);
		materialization.materialize();

		final long loadingDuration = materialization.getStartMaterialization() - startTime;
		final long materializationDuration = materialization.getMaterializationDuration();
		final int numberOfUnnamedIndiv = program.getUnnamedIndividualNames().keySet().size();
		final int numberOfActiveRoleConjunctions = program.getActiveRoleConjunctions().getRoleConjunctionNames().size();

		System.out.println(" - #final active role conjunctions: " + numberOfActiveRoleConjunctions);
		System.out.println(" - #Introduced Unnamed Individuals: " + numberOfUnnamedIndiv);
		System.out.println(" - total loading   duration: " + loadingDuration + " ms");
		System.out.println(" - materialization duration: " + materializationDuration + " ms");
	}

}
