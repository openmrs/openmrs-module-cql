package org.openmrs.module.cql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.providers.r4.EncounterFhirResourceProvider;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;

public class OpenMrsRetrieveProvider implements RetrieveProvider {
	
	private ObservationFhirResourceProvider obsResourceProvider;
	private Map<String, IBundleProvider> obsCache = new HashMap<>();
	
	private EncounterFhirResourceProvider encounterResourceProvider;

	@Override
	public Iterable<Object> retrieve(final String context, final String contextPath, final Object contextValue, final String dataType,
			final String templateId, final String codePath, final Iterable<Code> codes, final String valueSet, final String datePath,
			final String dateLowPath, final String dateHighPath, final Interval dateRange) {
		
		
		if (valueSet == null) {
			if ("Encounter".equals(dataType)) {
				if (encounterResourceProvider == null) {
					encounterResourceProvider = Context.getRegisteredComponent("encounterFhirR4ResourceProvider", EncounterFhirResourceProvider.class);
				}
				
				ReferenceAndListParam patientReference = new ReferenceAndListParam();
				ReferenceParam patient = new ReferenceParam();
				patient.setValue(PlanDefinition.getPatientId());
				patientReference.addValue(new ReferenceOrListParam().add(patient));
				
				IBundleProvider results = encounterResourceProvider.searchEncounter(null, null, null, null, patientReference, null, null, null,
				    null, null, null, null, null);
				
				if (results.size() == 0) {
					return Collections.emptyList();
				}
				
				return Collections.singleton(results.getAllResources().iterator().next());
			}
			else {
				//check to verify if dataType can be only Encounter or Observation. In which case we should never get here.
				return Collections.emptyList();
			}
		}
			
		IBundleProvider results = obsCache.get(valueSet);
		
		if (results == null) {
			Code cd = OpenMrsTerminologyProvider.getInstance().lookup(valueSet);
			
			Concept concept = Context.getConceptService().getConceptByMapping(cd.getCode(), "ANCDAK");
			if (concept == null) {
				return Collections.emptyList();
			}
			
			TokenAndListParam code = new TokenAndListParam();
			TokenParam codingToken = new TokenParam();
			codingToken.setValue(concept.getUuid());
			code.addAnd(codingToken);
	
			ReferenceAndListParam patientReference = new ReferenceAndListParam();
			ReferenceParam patient = new ReferenceParam();
			patient.setValue(PlanDefinition.getPatientId());
			patientReference.addValue(new ReferenceOrListParam().add(patient));
			
			if (obsResourceProvider == null) {
				obsResourceProvider = Context.getRegisteredComponent("observationFhirR4ResourceProvider", ObservationFhirResourceProvider.class);
			}
			
			results = obsResourceProvider.searchObservations(null, patientReference, null, null, null, null, null, null, code,
				    null, null, null, null, null, null, null);
			
			obsCache.put(valueSet, results);
		}

		return results.getAllResources().stream().map(x -> (Object) x).collect(Collectors.toList());
	}
}
