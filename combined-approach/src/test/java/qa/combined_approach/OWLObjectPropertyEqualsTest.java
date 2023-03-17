package qa.combined_approach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectInverseOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

public class OWLObjectPropertyEqualsTest {

	@Test
	public void testEquals() {
		final OWLObjectPropertyExpression simpleProp = new OWLObjectPropertyImpl(IRI.create("simpleProp"));
		assertEquals(new OWLObjectPropertyImpl(IRI.create(new String("simpleProp"))), simpleProp);

		final OWLObjectPropertyExpression inverseOfSimpleProp = new OWLObjectInverseOfImpl((OWLObjectProperty) simpleProp);
		assertNotEquals(simpleProp, inverseOfSimpleProp);
		assertEquals(new OWLObjectInverseOfImpl(new OWLObjectPropertyImpl(IRI.create(new String("simpleProp")))), inverseOfSimpleProp);
	}

}
