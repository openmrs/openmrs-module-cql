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
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.MeasureReport;
import org.json.JSONException;
import org.junit.Test;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;
import org.openmrs.module.cql.PlanDefinition.Apply;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class AncDakTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET = "org/openmrs/module/cql/ANCDT17.xml";
	
    private static InputStream open(String asset) { return PlanDefinition.class.getResourceAsStream(asset); }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    public static String load(String asset) throws IOException { return load(open(asset)); }
    
    /** Fluent interface starts here **/

    static class Assert {
        public static Apply that(String planDefinitionID, String patientID, String encounterID) {
            return new Apply(planDefinitionID, patientID, encounterID);
        }
    }
	
	@SuppressWarnings("deprecation")
	@Test
    public void testANCDT17WithInMemoryDataRepository() {

		this.executeDataSet(DATASET);
		
		isEqualsTo(
				Assert.that(
                "ANCDT17",
                "Patient/5946f880-b197-400b-9caa-a3c661d23041",
                null
            )
			.withParameters(parameters(part("encounter", "403fafb-e5e4-42d0-9d11-4f52e89d148c")))
			.withData("anc-dak/data")
            .apply()
            .getJson(), 
            "anc-dak/output-careplan.json");
    }
	
	@SuppressWarnings("deprecation")
	@Test
    public void testANCDT17WithOpenMrsDataRepository() {
		
		this.executeDataSet(DATASET);
		
		isEqualsTo(
				Assert.that(
                "ANCDT17",
                "Patient/5946f880-b197-400b-9caa-a3c661d23041",
                null
            )
			.withParameters(parameters(part("encounter", "e403fafb-e5e4-42d0-9d11-4f52e89d148c")))
            .apply()
            .getJson(), 
            "anc-dak/output-careplan.json");
    }
	
	public void isEqualsTo(String carePlanJson, String expectedCarePlanAssetName) {
         try { 
             JSONAssert.assertEquals(
            		 load(expectedCarePlanAssetName),
            		 carePlanJson,
                     true
             );
         } catch (JSONException | IOException e) {
             fail("Unable to compare Jsons: " + e.getMessage());
         }
    }
	
	@Test
    public void testTX_PVLSIndicator() {
		
		R4MeasureProcessor measureProcessor = MeasureProcessorUtil.setup(FhirContext.forCached(FhirVersionEnum.R4), false, 1);
		
		//measure and libraries
		Endpoint contentEndpoint = new Endpoint().setAddress("measure/Measure-TX_PVLS.json,measure/Library-TX-PVLS.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//valuesets
		Endpoint terminologyEndpoint = new Endpoint().setAddress("anc-dak/terminology-bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//observation, patient and encounter
		Endpoint dataEndpoint = new Endpoint().setAddress("measure/Observation-TX_PVLS-Bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
				
		MeasureReport report = measureProcessor.evaluateMeasure(
		        "http://fhir.org/plir/Measure/TX-PVLS",
		        "2019-01-01", "2030-12-31", "population", null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint,
		        null);
		
		assertEquals(report.getGroupFirstRep().getMeasureScore().getValue().toString(), "0.6666666666666666");
	}
	
	
	@Test
    public void testANCIND1IndicatorWithInMemoryDataRepository() {
		
		R4MeasureProcessor measureProcessor = MeasureProcessorUtil.setup(FhirContext.forCached(FhirVersionEnum.R4), false, 1);
		
		//measure and libraries
		Endpoint contentEndpoint = new Endpoint().setAddress("measure/measure-ANCIND07.json,measure/library-ANCIND07.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//valuesets
		Endpoint terminologyEndpoint = new Endpoint().setAddress("anc-dak/terminology-bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//observation, patient and encounter
		Endpoint dataEndpoint = new Endpoint().setAddress("measure/Observation-ANCIND7-Bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
				
		MeasureReport report = measureProcessor.evaluateMeasure(
		        "http://fhir.org/guides/who/anc-cds/Measure/ANCIND07",
		        "2018-01-01", "2030-12-31", "population", null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint,
		        null);
		
		System.out.println(FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
		//assertEquals(report.getGroupFirstRep().getMeasureScore().getValue().toString(), "0.6666666666666666");
	}
	
	@Test
    public void testANCIND1IndicatorWithOpenMrsDataRepository() {
		
		R4MeasureProcessor measureProcessor = MeasureProcessorUtil.setup(FhirContext.forCached(FhirVersionEnum.R4), false, 1);
		
		//measure and libraries
		Endpoint contentEndpoint = new Endpoint().setAddress("measure/measure-ANCIND07.json,measure/library-ANCIND07.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//valuesets
		Endpoint terminologyEndpoint = new Endpoint().setAddress("anc-dak/terminology-bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
		
		//observation, patient and encounter
		Endpoint dataEndpoint = new Endpoint().setAddress("measure/Observation-ANCIND7-Bundle.json")
		        .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));
				
		MeasureReport report = measureProcessor.evaluateMeasure(
		        "http://fhir.org/guides/who/anc-cds/Measure/ANCIND07",
		        "2019-01-01", "2030-12-31", "population", null, null, null, contentEndpoint, terminologyEndpoint, dataEndpoint,
		        null);
		
		System.out.println(FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
		//assertEquals(report.getGroupFirstRep().getMeasureScore().getValue().toString(), "0.6666666666666666");
	}
}
