package org.openmrs.module.cql;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.opencds.cqf.cql.evaluator.library.EvaluationSettings;
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
    
    public static PlanDefinitionProcessor buildProcessor(Repository repository) {
        return new PlanDefinitionProcessor(repository);
    }
    
    public static class Apply {
        private String planDefinitionID;

        private String patientID;
        private String encounterID;
        
        private Repository repository;
        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;
        
        private Parameters parameters;
        
        private LibraryEngine libraryEngine;

        public Apply() {
        	
        }
        
        public Apply(String planDefinitionID, String patientID, String encounterID) {
            this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
        }
        
        public Apply withData(String dataFolder) {
        	dataRepository = new InMemoryFhirRepository(fhirContext, this.getClass(), List.of(dataFolder), false);
            return this;
        }
        
        public Apply withParameters(Parameters params) {
            parameters = params;
            return this;
        }
        
        private void buildRepository() {
            if (repository != null) {
            	return;
            }
            
            if (dataRepository == null) {
                dataRepository = new OpenMrsRepository(fhirContext);
            }
              
            if (contentRepository == null) {
                contentRepository = new InMemoryFhirRepository(fhirContext, this.getClass(), List.of("anc/libraries", "anc/plandefinitions"), false);
            }
              
            if (terminologyRepository == null) {
                terminologyRepository = new InMemoryFhirRepository(fhirContext, this.getClass(),
                    List.of("anc/valuesets"), false);
            }
              
            repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
        }

        public GeneratedCarePlan apply() {	
            return apply(planDefinitionID, patientID, encounterID, parameters);
        }
        
        public GeneratedCarePlan apply(String planDefinitionID, String patientID, String encounterID, Parameters parameters) {
        	
        	this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
            this.parameters = parameters;
            
        	if (libraryEngine == null) {
        		buildRepository();
                
                libraryEngine = new LibraryEngine(this.repository, EvaluationSettings.getDefault());
        	}
        	
        	if (dataRepository instanceof OpenMrsRepository) {
        		((OpenMrsRepository)dataRepository).clearCache();
        	}
        	
        	return new GeneratedCarePlan((CarePlan) buildProcessor(repository).apply(
                    new IdType("PlanDefinition", planDefinitionID), null, null, patientID, encounterID, null, null, null,
                    null, null, null, null, parameters, null, null, null, libraryEngine));
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
        
        public CarePlan getCarePlan() {
        	return (CarePlan)carePlan;
        }
    }
}