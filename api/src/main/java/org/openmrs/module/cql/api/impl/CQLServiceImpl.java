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

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.part;

import java.util.List;

import org.hl7.fhir.r4.model.CarePlan;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.cql.CarePlanUtil;
import org.openmrs.module.cql.PlanDefinition;
import org.openmrs.module.cql.api.CQLService;
import org.openmrs.module.cql.api.dao.CQLDao;

public class CQLServiceImpl extends BaseOpenmrsService implements CQLService {
	
	CQLDao dao;
	
	UserService userService;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(CQLDao dao) {
		this.dao = dao;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	@Override
	public List<String> applyPlanDefinition(Patient patient, String planDefinitionId) throws APIException {
		
		Encounter encounter = getLatestEncounter(patient);
		if (encounter == null) {
			return null;
		}
		
		CarePlan carePlan = new PlanDefinition.Apply(
				planDefinitionId,
				patient.getUuid(),
                null
            )
			.withParameters(parameters(part("encounter", encounter.getUuid())))
            .apply()
            .getCarePlan();
		
		return CarePlanUtil.getActions(carePlan);
	}

	@Override
	public Encounter getLatestEncounter(Patient patient) {
		return dao.getLatestEncounter(patient);
	}
}
