package qa.combined_approach;

import uk.ac.ox.cs.JRDFox.JRDFoxException;

/**
 * Exception that occurs during the combined-approach implementation.
 * 
 * @author Irina Dragoste
 *
 */
public class CombinedApproachException extends RuntimeException {

	private static final long serialVersionUID = 3811357341357955334L;

	public CombinedApproachException(final String string, final JRDFoxException e) {
		super(string, e);
	}

	public CombinedApproachException(final String string) {
		super(string);
	}

}
