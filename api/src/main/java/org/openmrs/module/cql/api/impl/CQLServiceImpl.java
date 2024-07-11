/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cql.api.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.cql.CarePlanUtil;
import org.openmrs.module.cql.FhirResourceLoader;
import org.openmrs.module.cql.PlanDefinition;
import org.openmrs.module.cql.PlanDefinition.GeneratedCarePlan;
import org.openmrs.module.cql.api.CQLService;
import org.openmrs.module.cql.api.dao.CQLDao;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

public class CQLServiceImpl extends BaseOpenmrsService implements CQLService {
	
	private CQLDao dao;
	
	private PlanDefinition.Apply planDefinitionApply;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(CQLDao dao) {
		this.dao = dao;
	}

	@Override
	public List<String> applyPlanDefinition(Patient patient, String planDefinitionId) throws APIException {
		
		Encounter encounter = getLatestEncounter(patient);
		if (encounter == null) {
			return new ArrayList<String>();
		}
		
		CarePlan carePlan = applyPlanDefinition(patient, planDefinitionId, encounter).getCarePlan();
		return CarePlanUtil.getActions(carePlan);
	}
	
	@Override
	public String applyPlanDefinition(String patientUuid, String planDefinitionId) throws APIException {
		
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		if (patient == null) {
			return null;
		}
		
		Encounter encounter = getLatestEncounter(patient);
		if (encounter == null) {
			return null;
		}
		
		return applyPlanDefinition(patient, planDefinitionId, encounter).getJson();
	}
	
	private GeneratedCarePlan applyPlanDefinition(Patient patient, String planDefinitionId, Encounter encounter) {
		if (planDefinitionApply == null) {
			planDefinitionApply = new PlanDefinition.Apply();
		}
		
		return planDefinitionApply.apply(planDefinitionId, patient.getUuid(), null, parameters(part("encounter", encounter.getUuid())));
	}

	@Override
	public Encounter getLatestEncounter(Patient patient) {
		return dao.getLatestEncounter(patient);
	}

	@Override
	public String evaluateMeasure(String measureId, String periodStart, String periodEnd, String reportType) {
		
		String url = "http://fhir.org/guides/who/anc-cds/Measure/" + measureId;
		
		if (StringUtils.isBlank(reportType)) {
			reportType = "population";
		}

		//measure and libraries
		Repository contentRepository;
		{
			FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of("measure"));
			BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
			for (IBaseResource resource : resourceLoader.getResources()) {
				if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null && resource.getIdElement().getIdPart().equals(measureId)) {
					builder.addTransactionUpdateEntry(resource);
				}
			}
			contentRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
		}

		//valuesets
		Repository terminologyRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(IBaseBundle.class, "anc-dak/terminology-bundle.json"));

		//observation, patient and encounter
		Repository dataRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(IBaseBundle.class, "measure/Observation-TX_PVLS-Bundle.json"));

		R4MeasureProcessor measureProcessor = new R4MeasureProcessor(Repositories.proxy(dataRepository, contentRepository, terminologyRepository), MeasureEvaluationOptions.defaultOptions());

				
		MeasureReport report = measureProcessor.evaluateMeasure(Eithers.forLeft3(new CanonicalType(url)), periodStart, periodEnd, reportType, null, null, null);
		
		return FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true).encodeResourceToString(report);
	}

	private <T extends IBaseResource> T getResourceFromClasspath(Class<T> type, String location) {
		URL resource = getClass().getResource(location);
		if (resource != null) {
			try {
				IParser parser = FhirContext.forR4Cached().newJsonParser();
				return parser.parseResource(type, IOUtils.toString(resource, StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new APIException(e);
			}
		}

		return null;
	}
}
