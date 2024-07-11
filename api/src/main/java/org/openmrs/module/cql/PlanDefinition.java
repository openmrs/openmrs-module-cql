package org.openmrs.module.cql;

import java.util.List;

import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class PlanDefinition {
	
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    
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
            FhirResourceLoader resourceLoader = new FhirResourceLoader(fhirContext, this.getClass(), List.of(dataFolder));
            BundleBuilder builder = new BundleBuilder(fhirContext);
            for (IBaseResource resource : resourceLoader.getResources()) {
                builder.addTransactionUpdateEntry(resource);
            }
        	dataRepository = new InMemoryFhirRepository(fhirContext, builder.getBundle());
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
                FhirResourceLoader resourceLoader = new FhirResourceLoader(fhirContext, this.getClass(), List.of("anc/libraries", "anc/plandefinitions"));
                BundleBuilder builder = new BundleBuilder(fhirContext);
                for (IBaseResource resource : resourceLoader.getResources()) {
                    builder.addTransactionUpdateEntry(resource);
                }
                contentRepository = new InMemoryFhirRepository(fhirContext, builder.getBundle());
            }
              
            if (terminologyRepository == null) {
                FhirResourceLoader resourceLoader = new FhirResourceLoader(fhirContext, this.getClass(), List.of("anc/valuesets"));
                BundleBuilder builder = new BundleBuilder(fhirContext);
                for (IBaseResource resource : resourceLoader.getResources()) {
                    builder.addTransactionUpdateEntry(resource);
                }
                terminologyRepository = new InMemoryFhirRepository(fhirContext, builder.getBundle());
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

        	return new GeneratedCarePlan(new PlanDefinitionProcessor(repository).<IdType, CarePlan>apply(
                    Eithers.for3(null, new IdType("PlanDefinition", planDefinitionID), null), patientID, encounterID, null, null, null,
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