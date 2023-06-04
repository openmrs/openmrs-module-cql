/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cql;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.io.IOException;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.MeasureReport;
import org.json.JSONException;
import org.junit.Test;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class AncDakTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET = "org/openmrs/module/cql/ANCDT17.xml";
	
	protected FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
	
	@SuppressWarnings("deprecation")
	@Test
    public void testANCDT17PlanDefinition() {
		
		var parameters = parameters(part("encounter", "eec646cb-c847-45a7-98bc-91c8c4f70add"));
		
		this.executeDataSet(DATASET);
		
		isEqualsTo(
				PlanDefinition.Assert.that(
                "ANCDT17",
                "Patient/5946f880-b197-400b-9caa-a3c661d23041",
                null
            )
			.withParameters(parameters)
            .apply()
            .getJson(), 
            "anc-dak/output-careplan.json");
    }
	
	public void isEqualsTo(String carePlanJson, String expectedCarePlanAssetName) {
         try {
             JSONAssert.assertEquals(
            		 PlanDefinition.load(expectedCarePlanAssetName),
            		 carePlanJson,
                     true
             );
         } catch (JSONException | IOException e) {
             fail("Unable to compare Jsons: " + e.getMessage());
         }
    }
	
	@Test
    public void testMeasure() {
		
		R4MeasureProcessor measureProcessor = MeasureProcessorUtil.setup(fhirContext, false, 1);
		
		//measure and libraries
		Endpoint contentEndpoint = new Endpoint().setAddress("measure/Measure-TX_PVLS.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//valuesets
		Endpoint terminologyEndpoint = new Endpoint().setAddress("anc-dak/terminology-bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//observation, patient and encounter
		Endpoint dataEndpoint = new Endpoint().setAddress("measure/Observation-TX_PVLS.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
				
		MeasureReport report = measureProcessor.evaluateMeasure(
		        "http://fhir.org/plir/Measure/TX-PVLS",
		        "2019-01-01", "2030-12-31", "population", null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint,
		        null);
		
		System.out.println(PlanDefinition.toString(report));
	}
}
