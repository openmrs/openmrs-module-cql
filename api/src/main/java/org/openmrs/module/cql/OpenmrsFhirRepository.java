package org.openmrs.module.cql;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MetadataResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.opencds.cqf.fhir.api.Repository;
import org.openmrs.api.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenmrsFhirRepository implements Repository {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsFhirRepository.class);
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    public static <T extends IBaseResource> T parse(Class<T> resourceType, String asset) {
        try (InputStream assetIs = OpenmrsFhirRepository.class.getResourceAsStream(asset)) {
            return jsonParser.parseResource(resourceType, assetIs);
        } catch (IOException e) {
            log.error("Error parsing " + asset, e);
            throw new APIException("Error parsing " + asset, e);
        }
    }
    
    private final Map<String, IBaseResource> cacheById = new HashMap<>();
    private final Map<String, List<IBaseResource>> cacheByURL = new HashMap<>();
    private final Map<String, List<IBaseResource>> cacheByType = new HashMap<>();

    private String toKey(IIdType resource) {
        return resource.getResourceType() + "/" + resource.getIdPart();
    }

    private void insertOrUpdate(Map<String, List<IBaseResource>> list, String key, IBaseResource element) {
        if (list.containsKey(key))
            list.get(key).add(element);
        else
            list.put(key, new ArrayList<>(Collections.singletonList(element)));
    }

    private void putIntoCache(IBaseResource resource) {
        cacheById.put(toKey(resource.getIdElement()), resource);
        insertOrUpdate(cacheByType, resource.getIdElement().getResourceType(), resource);
     
        if (resource instanceof MetadataResource) {
            insertOrUpdate(cacheByURL, ((MetadataResource)resource).getUrl(), resource);
        }
    }

    public void addAll(IBaseResource resource) {
        if (resource == null) return;

        if (resource instanceof Bundle) {
            ((Bundle) resource).getEntry().forEach(entry -> {
                addAll(entry.getResource());
            });
        } else {
            putIntoCache(resource);
        }
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id) {
        T resource =  null;

        if (id.getResourceType().equals(PlanDefinition.class.getSimpleName())) {
            String asset = "anc/plandefinitions/plandefinition-" + id.getIdPart() + ".json";
            resource = (T) cacheById.get(asset);
            if (resource == null) {
                resource = parse(resourceType, asset);
                cacheById.put(asset, resource);
            }
        }

        return resource;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id, Map<String, String> headers) {
        return read(resourceType, id);
    }

    @Override
    public <T extends IBaseResource>  MethodOutcome create(T resource) {
    	throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource) {
    	throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType, Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters) {
        // TODO Implement this
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType, Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters, Map<String, String> headers) {
        // TODO Implement this
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }
}
