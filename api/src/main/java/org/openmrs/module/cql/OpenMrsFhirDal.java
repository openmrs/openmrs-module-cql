package org.openmrs.module.cql;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class OpenMrsFhirDal implements FhirDal {
	
	private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    private static InputStream open(String asset) { return OpenMrsFhirDal.class.getResourceAsStream(asset); }
    
    public static IBaseResource parse(String asset) {
        return jsonParser.parseResource(open(asset));
    }
    
    private final Map<String, IBaseResource> cacheById = new HashMap<String, IBaseResource>();
    private final Map<String, List<IBaseResource>> cacheByURL = new HashMap<String, List<IBaseResource>>();
    private final Map<String, List<IBaseResource>> cacheByType = new HashMap<String, List<IBaseResource>>();

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
    public IBaseResource read(IIdType id) {

    	IBaseResource resource =  null;
    	
    	if (id.getResourceType().equals(PlanDefinition.class.getSimpleName())) {
    		String asset = "anc/plandefinitions/plandefinition-" + id.getIdPart() + ".json";
        	resource = cacheById.get(asset);	
        	if (resource == null) {
        		resource = parse(asset);
        		cacheById.put(asset, resource);
        	}
    	}
    	
    	return resource;
    }

    @Override
    public void create(IBaseResource resource) {
    	throw new NotImplementedException();
    }

    @Override
    public void update(IBaseResource resource) {
    	throw new NotImplementedException();
    }

    @Override
    public void delete(IIdType id) {
    	throw new NotImplementedException();
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        return cacheByType.get(resourceType);
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        return cacheByURL.get(url).stream()
            .filter(resource -> resourceType.equals(resource.getIdElement().getResourceType()))
            .collect(Collectors.toList());
    }
    
}
