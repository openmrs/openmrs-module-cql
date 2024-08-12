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

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;

public class CarePlanUtil {

	public static List<String> getActions(CarePlan carePlan) {
		
		List<String> actionList = new ArrayList<String>();
		
		RequestGroup group = (RequestGroup)((CarePlan)carePlan).getContained().iterator().next();
    	List<RequestGroupActionComponent> actions = group.getAction();
    	for (RequestGroupActionComponent action : actions) {
    		List<RequestGroupActionComponent> childActions = action.getAction();
    		for (RequestGroupActionComponent childAction : childActions) {
    			actionList.add(childAction.getTitle() +" : " + childAction.getDescription());
    		}
    	}
    	
    	return actionList;
	}
}
