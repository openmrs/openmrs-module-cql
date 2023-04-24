package org.openmrs.module.cql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.evaluator.fhir.test.TestRepository;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.cql.evaluator.plandefinition.r4.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Repositories;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class PlanDefinition {
	
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    
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
    
    public static PlanDefinitionProcessor buildProcessor(Repository repository) {
        return new PlanDefinitionProcessor(repository);
    }

    /** Fluent interface starts here **/

    static class Assert {
        public static Apply that(String planDefinitionID, String patientID, String encounterID) {
            return new Apply(planDefinitionID, patientID, encounterID);
        }
    }
    
    public static class Apply {
        private String planDefinitionID;

        private String patientID;
        private String encounterID;
        
        private Repository repository;
        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;

        public Apply(String planDefinitionID, String patientID, String encounterID) {
            this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
            
            patientIdHolder.set(patientID);
        }
        
        public Apply withData(String dataAssetName) {
            dataRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));
            return this;
        }
        
        public Apply withTerminology(String dataAssetName) {
            terminologyRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));
            return this;
        }


        public Apply withContent(String dataAssetName) {
            contentRepository = new TestRepository(fhirContext, (Bundle) parse(dataAssetName));
            return this;
        }
        
        private void buildRepository() {
            if (repository != null) {
              return;
            }
            if (dataRepository == null) {
              dataRepository = new TestRepository(fhirContext, this.getClass(), List.of("tests"), false);
            }
            if (contentRepository == null) {
              contentRepository = new TestRepository(fhirContext, this.getClass(), List.of("content"), false);
            }
            if (terminologyRepository == null) {
              terminologyRepository = new TestRepository(fhirContext, this.getClass(),
                  List.of("vocabulary/CodeSystem", "vocabulary/ValueSet"), false);
            }

            repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
          }

        public GeneratedCarePlan apply() {
        	
            buildRepository();
            
            var libraryEngine = new LibraryEngine(this.repository);
     
            return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
                new IdType("PlanDefinition", planDefinitionID), null, null, patientID, encounterID, null, null, null,
                null, null, null, null, null, null, null, null, libraryEngine));
        }
    }
  
    public static class GeneratedCarePlan {
    	IBaseResource carePlan;

        public GeneratedCarePlan(IBaseResource carePlan) {
            this.carePlan = carePlan;
        }
        
        public String getJson() {
        	return jsonParser.encodeResourceToString(carePlan);
        }
    }
}