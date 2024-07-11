/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.cql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherDSTU3;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR5;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.cql.api.CQLService;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;

import com.google.common.collect.Maps;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;

public class OpenMrsRepository implements Repository {

    private final Map<IIdType, IBaseResource> resourceMap;
    private final FhirContext context;

    private ObservationFhirResourceProvider obsResourceProvider;
    private final Map<String, IBaseBundle> obsCache = new HashMap<>();

    Class<?> relativeToClazz;

    public OpenMrsRepository() {
        this.context = null;
        this.resourceMap = new LinkedHashMap<>();
    }

    public OpenMrsRepository(FhirContext context) {
        this.context = context;
        this.resourceMap = new LinkedHashMap<>();
    }

    public OpenMrsRepository(FhirContext context, Class<?> clazz,
                             List<String> directoryList, boolean recursive) {
        this.context = context;
        this.relativeToClazz = clazz;

        List<IBaseResource> resources = new FhirResourceLoader(context, clazz, directoryList, recursive).getResources();

        this.resourceMap = Maps.uniqueIndex(resources,
                r -> Ids.newId(this.context, r.getIdElement().getResourceType(),
                        r.getIdElement().getIdPart()));
    }

    public OpenMrsRepository(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        this.resourceMap = Maps.uniqueIndex(BundleUtil.toListOfResources(this.context, bundle),
                r -> Ids.newId(this.context, r.getIdElement().getResourceType(),
                        r.getIdElement().getIdPart()));
    }

    public ResourceMatcher getResourceMatcher() {
        switch (this.context.getVersion().getVersion()) {
            case DSTU3:
                return new ResourceMatcherDSTU3();
            case R4:
                return new ResourceMatcherR4();
            case R5:
                return new ResourceMatcherR5();
            default:
                throw new NotImplementedException(
                        "Resource matching is not implemented for FHIR version "
                                + this.context.getVersion().getVersion());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id,
                                                               Map<String, String> headers) {
        var theId = Ids.newId(context, resourceType.getSimpleName(), id.getIdPart());
        if (resourceMap.containsKey(theId)) {
            return (T) resourceMap.get(theId);
        }
        throw new ResourceNotFoundException("Resource not found with id " + theId);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newRandomId(context, resource.getIdElement().getResourceType());
        while (resourceMap.containsKey(theId)) {
            theId = Ids.newRandomId(context, resource.getIdElement().getResourceType());
        }
        resource.setId(theId);
        resourceMap.put(theId, resource);
        outcome.setCreated(true);
        return outcome;
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters,
                                                                              Map<String, String> headers) {
        throw new NotImplementedException("The PATCH operation is not currently supported");
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newId(context, resource.getIdElement().getResourceType(),
                resource.getIdElement().getIdPart());
        if (!resourceMap.containsKey(theId)) {
            outcome.setCreated(true);
        }
        resourceMap.put(theId, resource);
        return outcome;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType,
                                                                             I id, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newId(context, resourceType.getSimpleName(), id.getIdPart());
        if (resourceMap.containsKey(theId)) {
            resourceMap.remove(theId);
        } else {
            throw new ResourceNotFoundException("Resource not found with id " + theId);
        }
        return outcome;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType,
                                                                     Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters,
                                                                     Map<String, String> headers) {

        if ("Observation".equals(resourceType.getSimpleName())) {
            return searchObservation(searchParameters);
        }

        BundleBuilder builder = new BundleBuilder(this.context);

        var resourceList = resourceMap.values().stream().filter(
                        resource -> resource.getIdElement().getResourceType().equals(resourceType.getSimpleName()))
                .collect(Collectors.toList());

        List<IBaseResource> filteredResources = new ArrayList<>();
        if (searchParameters != null && !searchParameters.isEmpty()) {
            var resourceMatcher = getResourceMatcher();
            for (var resource : resourceList) {
                boolean include = false;
                for (var nextEntry : searchParameters.entrySet()) {
                    var paramName = nextEntry.getKey();
                    if (resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                        include = true;
                    } else {
                        include = false;
                        break;
                    }
                }
                if (include) {
                    filteredResources.add(resource);
                }
            }
            filteredResources.forEach(builder::addCollectionEntry);
        } else {
            resourceList.forEach(builder::addCollectionEntry);
        }

        builder.setType("searchset");
        return (B) builder.getBundle();
    }

    @SuppressWarnings("unchecked")
    private <B extends IBaseBundle, T extends IBaseResource> B searchObservation(Map<String, List<IQueryParameterType>> searchParameters) {

        TokenParam codeParam = ((TokenParam) searchParameters.get("code").get(0));

        //The only reason why we do caching here is that, in the course of running a plan definition
        //for a patient, this is called more than once, for the same patient and observation.
        //But as a result of this optimization, we need to call the clearCache() method when we switch to another patient
        IBaseBundle bundle = obsCache.get(codeParam.getValue());
        if (bundle == null) {

            String uuid = ((ReferenceParam) searchParameters.get("subject").get(0)).getValue();
            uuid = uuid.replace("Patient/", "");

            Patient patient = Context.getPatientService().getPatientByUuid(uuid);
            Encounter encounter = Context.getService(CQLService.class).getLatestEncounter(patient);

            TokenAndListParam code = new TokenAndListParam();
            code.addAnd(codeParam);

            ReferenceAndListParam patientReference = new ReferenceAndListParam();
            ReferenceParam patientParam = new ReferenceParam();
            patientParam.setValue(uuid);
            patientReference.addValue(new ReferenceOrListParam().add(patientParam));

            ReferenceAndListParam encounterReference = new ReferenceAndListParam();
            ReferenceParam encounterParam = new ReferenceParam();
            encounterParam.setValue(encounter.getUuid());
            encounterReference.addValue(new ReferenceOrListParam().add(encounterParam));

            if (obsResourceProvider == null) {
                obsResourceProvider = Context.getRegisteredComponent("observationFhirR4ResourceProvider", ObservationFhirResourceProvider.class);
            }

            IBundleProvider results = obsResourceProvider.searchObservations(null, patientReference, null, null, null, null, null, null, code,
                    null, null, null, null, null, null, null);

            bundle = new Bundle();
            for (IBaseResource resource : results.getAllResources()) {
                ((Bundle) bundle).addEntry().setResource((Resource) resource);
            }

            obsCache.put(codeParam.getValue(), bundle);
        }

        return (B) bundle;
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url,
                                          Map<String, String> headers) {
        throw new NotImplementedException("Paging is not currently supported");
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType,
                                                       Map<String, String> headers) {
        throw new NotImplementedException("The capabilities interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        throw new NotImplementedException("The transaction operation is not currently supported");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters,
                                                                         Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters,
                                                            Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType,
            Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
            Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id,
                                                                                            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name,
                                                                               P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters,
                                                                        Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
            Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id,
                                                                                           P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public FhirContext fhirContext() {
        return this.context;
    }

    public void clearCache() {
        obsCache.clear();
    }
}
