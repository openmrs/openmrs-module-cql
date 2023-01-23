package org.openmrs.module.cql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.plandefinition.OperationParametersParser;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class PlanDefinition {
	
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    private static OpenMrsTerminologyProvider terminologyProvider = null;
    private static OpenMrsFhirLibrarySourceProvider librarySourceProvider = null;
    private static OpenMrsRetrieveProvider retrieveProvider = null;
    
    private static final ThreadLocal<String> patientIdHolder = new ThreadLocal<>();
    
    private static InputStream open(String asset) { return PlanDefinition.class.getResourceAsStream(asset); }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    public static String load(String asset) throws IOException { return load(open(asset)); }
    
    public static IBaseResource parse(String asset) {
        return jsonParser.parseResource(open(asset));
    }
    
    public static String getPatientId() {
    	return patientIdHolder.get();
    }
    
    public static PlanDefinitionProcessor buildProcessor(FhirDal fhirDal, String planDefinitionID) {
    	
    	terminologyProvider = null;
    	librarySourceProvider = null;
    	retrieveProvider = null;
    	
        AdapterFactory adapterFactory = new AdapterFactory();
        LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        CqlFhirParametersConverter cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

        FhirModelResolverFactory fhirModelResolverFactory = new FhirModelResolverFactory();
        Set<ModelResolverFactory> modelResolverFactories = Collections.singleton(fhirModelResolverFactory);

        Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = new HashSet<TypedLibrarySourceProviderFactory>();
        
        librarySourceProviderFactories = Collections.singleton(
            new TypedLibrarySourceProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public LibrarySourceProvider create(String urls, List<String> headers) {
                	
                	if (librarySourceProvider == null) {
                		librarySourceProvider = new OpenMrsFhirLibrarySourceProvider(adapterFactory, libraryVersionSelector);

                		urls = "library-ANCConfig.json,library-ANCConcepts.json,"
                    			+ "library-ANCDataElements.json,library-FHIRCommon.json,library-WHOCommon.json,"
                    			+ "library-ANCCommon.json,library-ANCBaseConcepts.json,library-ANCContactDataElements.json,"
                    			+ "library-" + planDefinitionID + ".json";
                		
	                	String[] urlArray = urls.split(",");
	                	for (String url : urlArray) {
	                		librarySourceProvider.add(parse("anc/libraries/" + url));
	                	}
                	}
                	
                	return librarySourceProvider;
                }
            }
        );
        

        LibrarySourceProviderFactory librarySourceProviderFactory = new LibrarySourceProviderFactory(
                fhirContext, adapterFactory, librarySourceProviderFactories, libraryVersionSelector);

        Set<TypedRetrieveProviderFactory> retrieveProviderFactories = Collections.singleton(
            new TypedRetrieveProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public RetrieveProvider create(String url, List<String> headers) {
                	if (retrieveProvider == null) {
                		retrieveProvider = new OpenMrsRetrieveProvider(/*fhirContext, (IBaseBundle) parse(url)*/);
                	}
                	
                	return retrieveProvider;
                }
            }
        );

        DataProviderFactory dataProviderFactory = new DataProviderFactory(
                fhirContext, modelResolverFactories, retrieveProviderFactories);

        Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = Collections.singleton(
            new TypedTerminologyProviderFactory() {
                @Override
                public String getType() {
                    return Constants.HL7_FHIR_FILES;
                }

                @Override
                public TerminologyProvider create(String urls, List<String> headers) {
                	if (terminologyProvider == null) {
                		terminologyProvider = new OpenMrsTerminologyProvider(fhirContext);
                	}
                	return terminologyProvider;
                }
            }
        );

        TerminologyProviderFactory terminologyProviderFactory = new TerminologyProviderFactory(
                fhirContext, typedTerminologyProviderFactories);

        EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

        LibraryProcessor libraryProcessor = new LibraryProcessor(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
                dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());

        ExpressionEvaluator evaluator = new ExpressionEvaluator(fhirContext, cqlFhirParametersConverter, librarySourceProviderFactory,
            dataProviderFactory, terminologyProviderFactory, endpointConverter, fhirModelResolverFactory, () -> new CqlEvaluatorBuilder());

        ActivityDefinitionProcessor activityDefinitionProcessor = new ActivityDefinitionProcessor(fhirContext, fhirDal, libraryProcessor);
        OperationParametersParser operationParametersParser = new OperationParametersParser(adapterFactory, fhirTypeConverter);

        return new PlanDefinitionProcessor(
            fhirContext, fhirDal, libraryProcessor, evaluator,
            activityDefinitionProcessor, operationParametersParser
        );
    }

    /** Fluent interface starts here **/

    static class Assert {
        public static Apply that(String planDefinitionID, String patientID, String encounterID) {
            return new Apply(planDefinitionID, patientID, encounterID);
        }
    }
    
    static class Apply {
        private String planDefinitionID;

        private String patientID;
        private String encounterID;

        private OpenMrsFhirDal fhirDal = new OpenMrsFhirDal();
        private Endpoint dataEndpoint;
        private Endpoint libraryEndpoint;
        private IBaseResource baseResource;

        public Apply(String planDefinitionID, String patientID, String encounterID) {
            this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
            
            patientIdHolder.set(patientID);
        }

        public Apply withData(String dataAssetName) {
            dataEndpoint = new Endpoint()
                .setAddress(dataAssetName)
                .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

            return this;
        }

        public Apply withLibraries(String dataAssetName) {
            libraryEndpoint = new Endpoint()
                    .setAddress(dataAssetName)
                    .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

            return this;
        }

        public GeneratedCarePlan apply() {
            return new GeneratedCarePlan(
                buildProcessor(fhirDal, planDefinitionID)
                    .apply(
                        new IdType("PlanDefinition", planDefinitionID),
                        patientID,
                        encounterID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new Parameters(),
                        null,
                        (Bundle) baseResource,
                        null,
                        dataEndpoint,
                        libraryEndpoint,
                        libraryEndpoint
                    )
            );
        }
    }
  
    static class GeneratedCarePlan {
        CarePlan carePlan;

        public GeneratedCarePlan(CarePlan carePlan) {
            this.carePlan = carePlan;
        }
        
        public String getJson() {
        	return jsonParser.encodeResourceToString(carePlan);
        }
    }
}