/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cql.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.cql.api.CQLService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/plandefinitionapply")
public class PlanDefinitionApplyController extends BaseRestController {

	@Autowired
	private CQLService cqlService;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Object apply(HttpServletRequest request) {
		
		String subject = request.getParameter("subject");
		String planDefinitonId = request.getParameter("plandefinition");
		
		return cqlService.applyPlanDefinition(subject, planDefinitonId);
	}
}
