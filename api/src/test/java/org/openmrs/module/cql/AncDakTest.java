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

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.skyscreamer.jsonassert.JSONAssert;

public class AncDakTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET = "org/openmrs/module/cql/ANCDT17.xml";
	
	@SuppressWarnings("deprecation")
	@Test
    public void testANCDT17PlanDefinition() {
		
		this.executeDataSet(DATASET);
		
		isEqualsTo(
				PlanDefinition.Assert.that(
                "ANCDT17",
                "5946f880-b197-400b-9caa-a3c661d23041",
                null
            )
        	.withLibraries("")
            .withData("combined_bundle.json")
            .apply()
            .getJson(), 
            "output_careplan.json");
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
}
