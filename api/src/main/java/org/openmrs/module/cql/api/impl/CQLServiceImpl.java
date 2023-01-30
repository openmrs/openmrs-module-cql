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

import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
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
	public String applyPlanDefinition(String patientUuid, String planDefinitionId) throws APIException {
		
		return new PlanDefinition.Apply(
				planDefinitionId,
				patientUuid,
                null
            )
        	.withLibraries("")
            .withData("combined_bundle.json")
            .apply()
            .getJson();
	}
}
