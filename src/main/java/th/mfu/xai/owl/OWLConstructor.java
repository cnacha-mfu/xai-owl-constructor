package th.mfu.xai.owl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import com.ctc.wstx.util.StringUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class OWLConstructor {
	private static String MODEL_NAME = "coviddiet";
	private static String MODEL_PATH = "/home/nacha/owl-workspace/owlcontructor/output/";
	private static String IRI_PATH = "http://www.mfu.ac.th/ontologies/xai-model.owl";

	OWLOntology ontology = null;
	OWLOntologyManager manager;
	Configuration configuration;
	OWLUtil util;

	IRI ontologyIRI;
	OWLDataFactory factory;
	OWLReasoner reasoner;
	ReasonerFactory rf;

	Map<String, List<OWLClass>> colValMap = new HashMap<String, List<OWLClass>>();
	Map<String, OWLClass> notValMap = new HashMap<String, OWLClass>(); // storing not value class
	Map<String, OWLClass> regValMap = new HashMap<String, OWLClass>(); // storing regular value class
	Map<String, List> rangeValueMap = new HashMap<String, List>();// storing data property for value range
	OWLClass predClass;

	public static void main(String args[]) {
		OWLConstructor constructor = new OWLConstructor();
		try {
			constructor.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() throws OWLOntologyCreationException, IOException, URISyntaxException {

		// load ontology
		manager = OWLManager.createOWLOntologyManager();
		ontologyIRI = IRI.create(IRI_PATH);
		factory = manager.getOWLDataFactory();

		// create ontology model
		OWLOntology ontology = manager.createOntology();

		// setup reasoner and factory
		rf = new ReasonerFactory();
		configuration = new Configuration();
		configuration.throwInconsistentOntologyException = false;
		reasoner = rf.createReasoner(ontology, configuration);
		rf = new Reasoner.ReasonerFactory() {
			protected OWLReasoner createHermiTOWLReasoner(org.semanticweb.HermiT.Configuration configuration,
					OWLOntology o) {
				configuration.throwInconsistentOntologyException = false;
				return new Reasoner(configuration, o);
			}
		};

		BlackBoxExplanation exp = new BlackBoxExplanation(ontology, rf, reasoner);
		HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);

		util = new OWLUtil(manager, factory, ontologyIRI, ontology, rf, multExplanator);
		createFeatureClass();

		createPredictionClasses();

		createTestIndividuals();

		/******** save the model to file ***********/
		String ontologyowlURL = MODEL_PATH + MODEL_NAME + "-ontology.owl";
		System.out.println("saving file to: " + ontologyowlURL);
		File owlFile = new File(ontologyowlURL);
		owlFile.createNewFile();
		util.saveToFile(owlFile);

	}

	private void createTestIndividuals() throws FileNotFoundException, IOException {
		URL analysis_resource = OWLConstructor.class.getClassLoader().getResource(MODEL_NAME + "-gendata.txt");
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader objsReader = objectMapper.readerFor(new TypeReference<List<JsonNode>>() {
		});
		try (FileReader reader = new FileReader(analysis_resource.getFile());
				BufferedReader bufferedReader = new BufferedReader(reader);) {
			String currentLine;
			if ((currentLine = bufferedReader.readLine()) != null) {
				List<JsonNode> nodesList = objsReader.readValue(currentLine);
				
				// loop through each test record
				int no = 1;
			
				for (JsonNode node : nodesList) {
					System.out.println("generating testdata: "+no);
					// create individual of type prediction class
					OWLIndividual indv = util.createIndividual(MODEL_NAME + "_" + no, predClass);
					no++;

					Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();

					// define all its properties as in the generated file
					while (fieldsIterator.hasNext()) {
						Map.Entry<String, JsonNode> field = fieldsIterator.next();
						System.out.println("Key: " + field.getKey() + "\tValue:" + field.getValue());

						if (field.getKey().endsWith("range")) {
							// if it is range, create number within the range
							String rangeStr = rangeValueMap.get(field.getKey()).get(field.getValue().asInt())
									.toString();
							System.out.println("			range:"+rangeStr);
							double value = randomInRange(rangeStr);
							util.addDataProperty(indv, field.getKey(), value);
						} else {
							// if it is attribute, create an individual of certain type and associate with
							// it
							//System.out.println("	finding " + field.getKey() + "_" + field.getValue());
							if (colValMap.containsKey(field.getKey())) {
								List<OWLClass> values = colValMap.get(field.getKey());
							
									OWLIndividual featureIndv = util.createIndividual(
											field.getKey() + "_" + field.getValue(),
											values.get(field.getValue().asInt()));
									util.addObjectProperties(indv, featureIndv, "hasFeature");
								
							}

						}

					}
				}
			}
		}
	}

	private void createFeatureClass() {
		/***** create the model from available data set ****/
		OWLClass featureClass = util.createClass("feature");
		List<TableValues> tabvalues = readValuesFromFile();
		for (TableValues tabval : tabvalues) {

			// check if it is number range such as 1.00-2.00
			if ((tabval.getValues().get(0).toString()).indexOf("-") != -1) {
				// create data properties

				rangeValueMap.put(tabval.getColumn(), tabval.getValues());

				// if it is of other type such as String or number ranges value
			} else {

				// create a class for the column
				OWLClass colClass = util.createSubClass(tabval.getColumn(), featureClass);
				List<OWLClass> colValClassList = new ArrayList<OWLClass>();
				List<String> colValNameList = new ArrayList<String>();
				boolean isSingleValue = false;
				Map<String, OWLClass> notValColMap = new HashMap<String, OWLClass>();
				for (Object value : tabval.getValues()) {

					System.out.println("creating: " + value.toString() + " in " + tabval.getColumn() + " type:"
							+ value.getClass());
					System.out.println(value.toString());

					// create a class for each value
					String valueClassName = tabval.getColumn() + "_" + value.toString();
					OWLClass colValClass = util.createSubClass(valueClassName, colClass);
					regValMap.put(valueClassName, colValClass);
					colValClassList.add(colValClass);
					colValNameList.add(valueClassName);

					// we add not class to it.
					OWLClass notValClass = util.createSubClass("NOT_" + valueClassName, colClass);
					notValColMap.put(valueClassName, notValClass);
					isSingleValue = true;

				}

				// define not value to prevent universal inference
				for (int i = 0; i < colValNameList.size(); i++) {
					Set<OWLClass> notvalClsSet = new HashSet<OWLClass>();
					for (Map.Entry<String, OWLClass> entry : notValColMap.entrySet()) {
						if (!entry.getKey().equals(colValNameList.get(i))) {
							notvalClsSet.add(entry.getValue());
						}
					}
					// add subset to notvalue class
					util.addSubClassAxiom(colValClassList.get(i), notvalClsSet);
				}

				notValMap.putAll(notValColMap);
				colValMap.put(tabval.getColumn(), colValClassList);
			}

		}

	}

	private void createPredictionClasses() throws IOException, JsonProcessingException, FileNotFoundException {
		/***** create classes for the prediction ****/
		predClass = util.createClass(MODEL_NAME);
		URL analysis_resource = OWLConstructor.class.getClassLoader().getResource(MODEL_NAME + "-analysis.txt");

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader valueReader = objectMapper.readerFor(new TypeReference<List<Integer>>() {
		});
		try (FileReader reader = new FileReader(analysis_resource.getFile());
				BufferedReader bufferedReader = new BufferedReader(reader);) {
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				JsonNode categoryNode = objectMapper.readTree(currentLine);
				System.out.println("catg = " + categoryNode.toString() + ", " + categoryNode.path("factor").asText());

				// create class representing category of result
				OWLClass catgClass = util.createSubClass(MODEL_NAME + categoryNode.path("output").asText(), predClass);
				Iterator<Map.Entry<String, JsonNode>> itfactor = categoryNode.path("factor").fields();
				Set<OWLClassExpression> exprPropSet = new HashSet<OWLClassExpression>();

				while (itfactor.hasNext()) {
					// create definition for each factor
					Map.Entry<String, JsonNode> factorNode = itfactor.next();
					System.out.println("	" + factorNode.getKey() + " : " + factorNode.getValue());
					// create equivalent expression to link the classes of tableValues

					List<Integer> values = valueReader.readValue(factorNode.getValue());
					if (values.size() > 0) {
						if (colValMap.containsKey(factorNode.getKey())) {
							
							// column name matches factor name in the key
							Set<OWLClassExpression> exprObjectProps = new HashSet<OWLClassExpression>();
							for (Integer value : values) {
								
								OWLClassExpression expr = util.createClassSomeObjectProperties(colValMap.get(factorNode.getKey()).get(value), "hasFeature");
								exprObjectProps.add(expr);
								System.out.println("		objectprop:"+expr);
								
							}
							OWLObjectUnionOf union = factory.getOWLObjectUnionOf(exprObjectProps);
							exprPropSet.add(union);
						} else {
							// column name does not match the key, we find them in the value range
							if (rangeValueMap.containsKey(factorNode.getKey())) {
								List valueRanges = rangeValueMap.get(factorNode.getKey());
								Set<OWLClassExpression> propSet = new HashSet<OWLClassExpression>();
								for (Integer value : values) {
									String strValueRange = valueRanges.get(value).toString();
									double min = getLeftInRange(strValueRange);
									double max = getRightInRange(strValueRange);
									OWLClassExpression prop = util.createDataRangeExpression(factorNode.getKey(), max,
											min);
									propSet.add(prop);
								}
								OWLObjectUnionOf union = factory.getOWLObjectUnionOf(propSet);
								exprPropSet.add(union);
							}
						}
						System.out.println("	exprno:"+exprPropSet.size());
						
						
					}

				}
				util.addClassIntersectExpression(catgClass, exprPropSet);
				// util.addClassIntersectSomeObjectProperties(catgClass, classValSet,
				// "hasFeature", propSet);
			}
		}
	}

	private List<TableValues> readValuesFromFile() {
		// read valuedict file
		URL resource = OWLConstructor.class.getClassLoader().getResource(MODEL_NAME + "-valuedict.txt");
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		List<TableValues> tableValues = new ArrayList<TableValues>();

		try {
			JsonFactory jsonFactory = new JsonFactory();
			File valueDict = new File(resource.toURI());
			try (BufferedReader br = new BufferedReader(new FileReader(valueDict))) {
				Iterator<TableValues> value = mapper.readValues(jsonFactory.createParser(br), TableValues.class);
				value.forEachRemaining((u) -> {
					tableValues.add(u);
					System.out.println(u.getColumn() + " " + u.getValues().toString());
				});
			}

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return tableValues;
	}

	private double getLeftInRange(String range) {
		return Double.parseDouble(range.substring(0, range.lastIndexOf('-')));
	}

	private double getRightInRange(String range) {
		return Double.parseDouble(range.substring(range.lastIndexOf('-') + 1));
	}

	private double randomInRange(String rangeStr) {
		double min = this.getLeftInRange(rangeStr);
		double max = this.getRightInRange(rangeStr);
		Random r = new Random();
		double randomValue = min + (max - min) * r.nextDouble();
		return randomValue;
	}
}
