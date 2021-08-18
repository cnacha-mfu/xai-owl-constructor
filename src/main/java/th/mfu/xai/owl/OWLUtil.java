package th.mfu.xai.owl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semarglproject.vocab.OWL;

import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

public class OWLUtil {
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private IRI ontologyIRI;
	private OWLOntology ontology;
	private ReasonerFactory reasonerFactory;
	private HSTExplanationGenerator multExplanator;

	public OWLUtil(OWLOntologyManager manager, OWLDataFactory factory, IRI ontologyIRI, OWLOntology ontology,
			ReasonerFactory reasonerf, HSTExplanationGenerator multExplanator) {
		super();
		this.manager = manager;
		this.factory = factory;
		this.ontologyIRI = ontologyIRI;
		this.ontology = ontology;
		this.reasonerFactory = reasonerf;
		this.multExplanator = multExplanator;
	}
	
	public OWLClass createClass(String name) {
		OWLClass newCompClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + name));
		OWLAxiom axiom = factory.getOWLDeclarationAxiom(newCompClass);
		manager.addAxiom(ontology, axiom);

		return newCompClass;
	}

	public OWLClass createSubClass(String name, OWLClass superclass) {
		OWLClass newCompClass = factory.getOWLClass(IRI.create(ontologyIRI + "#" + name));
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(newCompClass, superclass);
		AddAxiom addAxiom = new AddAxiom(ontology, axiom);
		manager.applyChange(addAxiom);

		return newCompClass;
	}
	
	public void addSubClassAxiom(OWLClass origin, Set<OWLClass> set) {

		OWLObjectIntersectionOf intersect = factory.getOWLObjectIntersectionOf(set);

		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, intersect);

		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);

	}


	public OWLNamedIndividual createIndividual(String name, OWLClass cls) {
		OWLNamedIndividual indv = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + "#" + name));
		if (cls != null) {
			// assert class to individual
			OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(cls, indv);
			manager.addAxiom(ontology, classAssertion);
		}

		return indv;
	}

	public OWLIndividual addClassAssertion(OWLIndividual indv, OWLClass cls) {

		OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(cls, indv);
		manager.addAxiom(ontology, classAssertion);

		return indv;
	}

	public void markAllIndividualDifferent() {
		Set<OWLNamedIndividual> indvSet = ontology.getIndividualsInSignature();

		manager.addAxiom(ontology, factory.getOWLDifferentIndividualsAxiom(indvSet));

	}
	
	public OWLClassExpression createDataRangeExpression(String propName, double max, double min) {
		OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#" + propName));
		
		OWLDatatypeRestriction minres = factory.getOWLDatatypeMinInclusiveRestriction(min);
		OWLDatatypeRestriction maxres = factory.getOWLDatatypeMaxExclusiveRestriction(max);

		//OWLDataUnionOf range = factory.getOWLDataUnionOf(minres, maxres);
		OWLDataIntersectionOf rangeint =  factory.getOWLDataIntersectionOf(minres,maxres);
		OWLDataIntersectionOf rangeint2 =  factory.getOWLDataIntersectionOf(minres,maxres);
		OWLClassExpression ex = factory.getOWLDataSomeValuesFrom(prop, rangeint);
		return ex;
		
	}
	
	
	public void addClassIntersectExpression(OWLClass origin, Set<OWLClassExpression> set) {
		OWLObjectIntersectionOf intersect = factory.getOWLObjectIntersectionOf(set);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, intersect);
		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);
	}
	
	
		
	public OWLIndividual addDataProperty(OWLIndividual indv, OWLDataProperty prop, double value) {
		OWLDatatype doubleDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
		OWLLiteral literal = factory.getOWLLiteral(Double.toString(value), doubleDatatype);
		OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(prop, indv, literal);
		manager.addAxiom(ontology, ax);

		return indv;
	}

	public OWLIndividual addDataProperty(OWLIndividual indv, String propName, int value) {
		OWLDataProperty hasDeps = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#" + propName));

		OWLDatatype integerDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
		OWLLiteral literal = factory.getOWLLiteral(Integer.toString(value), integerDatatype);
		OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(hasDeps, indv, literal);
		manager.addAxiom(ontology, ax);

		return indv;
	}
	
	public OWLIndividual addDataProperty(OWLIndividual indv, String propName, double value) {
		OWLDataProperty hasDeps = factory.getOWLDataProperty(IRI.create(ontologyIRI + "#" + propName));

		OWLDatatype integerDatatype = factory.getOWLDatatype(OWL2Datatype.XSD_DOUBLE.getIRI());
		OWLLiteral literal = factory.getOWLLiteral(Double.toString(value), integerDatatype);
		OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(hasDeps, indv, literal);
		manager.addAxiom(ontology, ax);

		return indv;
	}

	public void addObjectProperties(OWLIndividual origin, OWLIndividual target, String objPropName) {
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));
		OWLObjectPropertyAssertionAxiom ax = factory.getOWLObjectPropertyAssertionAxiom(prop, origin, target);

		manager.addAxiom(ontology, ax);
	}

	public void addClassMinObjectProperties(OWLClass origin, OWLClass target, String objPropName) {

		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		OWLClassExpression propExpr = factory.getOWLObjectMinCardinality(1, prop, target);// factory.getOWLObjectSomeValuesFrom(prop,
																							// target);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, propExpr);
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		manager.applyChange(addAxiom);

	}
	
	public OWLClassExpression createClassSomeObjectProperties(OWLClass target, String objPropName) {

		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, target);
		
		return propExpr;

	}

	public void addClassSomeObjectProperties(OWLClass origin, OWLClass target, String objPropName) {

		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, target);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, propExpr);

		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);

	}
	
	public OWLObjectUnionOf addClassUnionSomeObjectProperties(OWLClass origin, Set<OWLClass> set, String objPropName) {
		// create properties with and
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		Set<OWLClassExpression> exprSet = new HashSet<OWLClassExpression>();
		for (OWLClass item : set) {
			OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, item);
			exprSet.add(propExpr);
		}
		OWLObjectUnionOf union = factory.getOWLObjectUnionOf(exprSet);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, union);

		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);
		return union;

	}
	
	

	public void addClassIntersectSomeObjectProperties(OWLClass origin, Set<OWLClass> set, String objPropName) {
		// create properties with and
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		Set<OWLClassExpression> exprSet = new HashSet<OWLClassExpression>();
		for (OWLClass item : set) {
			OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, item);
			exprSet.add(propExpr);
		}

		OWLObjectIntersectionOf intersect = factory.getOWLObjectIntersectionOf(exprSet);

		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, intersect);

		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);

	}
	
	public void addClassIntersectSomeObjectProperties(OWLClass origin, Set<OWLClass> set, String objPropName, Set<OWLClassExpression> propset) {
		// create properties with and
		OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(ontologyIRI + "#" + objPropName));

		Set<OWLClassExpression> exprSet = new HashSet<OWLClassExpression>();
		for (OWLClass item : set) {
			OWLClassExpression propExpr = factory.getOWLObjectSomeValuesFrom(prop, item);
			exprSet.add(propExpr);
		}
		exprSet.addAll(propset);

		OWLObjectIntersectionOf intersect = factory.getOWLObjectIntersectionOf(exprSet);

		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(origin, intersect);

		AddAxiom addAxiom = new AddAxiom(ontology, ax);

		manager.applyChange(addAxiom);

	}

	public boolean isSatisfiable(OWLClass cls) {
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		if (!reasoner.isSatisfiable(cls)) {
			System.out.println(cls.getIRI().getShortForm() + " is invalid...");
			/*
			 * Set<Set<OWLAxiom>>
			 * explanations=multExplanator.getExplanations(factory.getOWLThing(),2); // loop
			 * through expalnation for (Set<OWLAxiom> explanation : explanations) {
			 * System.out.println("------------------");
			 * System.out.println("Axioms causing the inconsistency: "); // find problematic
			 * axiom for (OWLAxiom causingAxiom : explanation) {
			 * 
			 * if(causingAxiom instanceof OWLSubClassOfAxiom) { OWLClass pcls =
			 * (OWLClass)((OWLSubClassOfAxiom)causingAxiom).getSubClass();
			 * System.out.println("problematic class: "+pcls.getIRI().getShortForm()); } //
			 * System.out.println(causingAxiom); } System.out.println("------------------");
			 * }
			 */
			return false;
		} else
			return true;
	}

	public void addClassExpression(OWLClass cls, String def) {
		OWLClassExpression clsExpr = this.convertStringToClassExpression(def);
		OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, clsExpr);
		AddAxiom addAxiom = new AddAxiom(ontology, ax);
		manager.applyChange(addAxiom);
	}

	private OWLClassExpression convertStringToClassExpression(String expression) {
		ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();

		Set<OWLOntology> importsClosure = ontology.getImportsClosure();
		OWLEntityChecker entityChecker = new ShortFormEntityChecker(
				new BidirectionalShortFormProviderAdapter(manager, importsClosure, new SimpleShortFormProvider()));

		parser.setStringToParse(expression);
		parser.setDefaultOntology(ontology);
		parser.setOWLEntityChecker(entityChecker);

		return parser.parseClassExpression();
	}

	public void saveToFile(File file) {
		// save ontology to file for debugging
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(file);
			manager.saveOntology(ontology, fout);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
