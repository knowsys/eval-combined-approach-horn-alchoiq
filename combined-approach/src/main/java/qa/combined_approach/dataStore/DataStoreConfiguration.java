package qa.combined_approach.dataStore;

import uk.ac.ox.cs.JRDFox.store.DataStore;

/**
 * Configuration for RDFox data store. Enables us to configure store type and number of threads to be used.
 *
 * @author Irina Dragoste
 *
 */
public class DataStoreConfiguration {
	private int rdfoxDataStoreThreads = 1;
	private DataStore.StoreType rdfoxDataStoreType = DataStore.StoreType.ParallelComplexWW;

	/**
	 * Number of threads used by RDFox data store for reasoning. Default value is 1.
	 *
	 * @return
	 */
	public int getRdfoxDataStoreThreads() {
		return rdfoxDataStoreThreads;
	}

	/**
	 * Sets the number of threads used by RDFox data store for reasoning. Default value is 1.
	 *
	 * @param rdfoxDataStoreThreads
	 */
	public void setRdfoxDataStoreThreads(final int rdfoxDataStoreThreads) {
		this.rdfoxDataStoreThreads = rdfoxDataStoreThreads;
	}

	// TODO copy from rdfsore javadoc
	/**
	 * Gets the RDFox data store type. Default value is ParallelSimpleNN
	 *
	 * @return
	 */
	public DataStore.StoreType getRdfoxDataStoreType() {
		return rdfoxDataStoreType;
	}

	/**
	 * Sets the RDFox data store type. Default value is ParallelSimpleNN
	 *
	 * @param rdfoxDataStoreType
	 */
	public void setRdfoxDataStoreType(final DataStore.StoreType rdfoxDataStoreType) {
		this.rdfoxDataStoreType = rdfoxDataStoreType;
	}

}
