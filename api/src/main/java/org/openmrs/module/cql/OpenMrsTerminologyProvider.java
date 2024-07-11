package org.openmrs.module.cql;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.cql.engine.utility.ValueSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class OpenMrsTerminologyProvider implements TerminologyProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenMrsTerminologyProvider.class);

    private FhirContext fhirContext;
    private IFhirPath fhirPath;
    private List<IBaseResource> valueSets = new ArrayList<>();
    private Map<String, Iterable<Code>> valueSetIndex = new HashMap<>();

    private static OpenMrsTerminologyProvider instance;
    
    public OpenMrsTerminologyProvider(FhirContext fhirContext) {
        requireNonNull(fhirContext, "fhirContext can not be null.");

        this.fhirContext = fhirContext;
        this.fhirPath = fhirContext.newFhirPath();
        
        instance = this;
    }

    public static OpenMrsTerminologyProvider getInstance(FhirContext fhirContext) {
    	if (instance == null) {
    		instance = new OpenMrsTerminologyProvider(fhirContext);
    	}
		return instance;
	}
    
    /** 
     * This method checks for membership of a Code in a ValueSet
     * @param code The Code to check.
     * @param valueSet The ValueSetInfo for the ValueSet to check membership of. Can not be null.
     * @return True if code is in the ValueSet.
     */
    @Override
    public boolean in(Code code, ValueSetInfo valueSet) {
        requireNonNull(code, "code can not be null when using 'expand'");
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");

        Iterable<Code> codes = this.expand(valueSet);
        checkExpansion(codes, valueSet);
        for (Code c : codes) {
            if (c.getCode().equals(code.getCode()) && c.getSystem().equals(code.getSystem())) {
                return true;
            }
        }

        return false;
    }

    /** 
     * This method expands a ValueSet into a list of Codes. It will use the "expansion" element of the ValueSet if present.
     * It will fall back the to "compose" element if not present. <b>NOTE:</b> This provider does not provide a full expansion
     * of the "compose" element. If only lists the codes present in the "compose". 
     * @param valueSet The ValueSetInfo of the ValueSet to expand
     * @return The Codes in the ValueSet. <b>NOTE:</b> This method never returns null.
     */
    @Override
    public Iterable<Code> expand(ValueSetInfo valueSet) {
        requireNonNull(valueSet, "valueSet can not be null when using 'expand'");
        
        return expandInternal(valueSet);
    }
    
    public Iterable<Code> expandInternal(ValueSetInfo valueSet) {
        if (!valueSetIndex.containsKey(valueSet.getId())) {
	        String[] tokens = valueSet.getId().split("/");
	        String id = tokens[tokens.length - 1];
	        String asset = "anc/valuesets/valueset-" + id + ".json";
            IBaseResource resource = OpenmrsFhirRepository.parse(IBaseResource.class, asset);
	        valueSets.add(resource);
	        
	        initValueSet(resource);
        }

        if (!valueSetIndex.containsKey(valueSet.getId())) {
            throw new IllegalArgumentException(String.format("Unable to locate ValueSet %s", valueSet.getId()));
        }

        return valueSetIndex.get(valueSet.getId());
    }

    
    /** 
     * Lookup is only partially implemented for this TerminologyProvider. Full implementation requires the ability to
     * access the full CodeSystem. This implementation only checks the code system of the code matches the CodeSystemInfo
     * url, and verifies the version if present.
     * @param code The Code to lookup
     * @param codeSystem The CodeSystemInfo of the CodeSystem to check.
     * @return The Code if the system of the Code (and version if specified) matches the CodeSystemInfo url (and version)
     */
    @Override
	public Code lookup(Code code, CodeSystemInfo codeSystem) {
        if (code.getSystem() == null) {
            return null;
        }

        if (code.getSystem().equals(codeSystem.getId()) && (code.getVersion() == null || code.getVersion().equals(codeSystem.getVersion()))) {
            logger.warn("Unvalidated CodeSystem lookup: {} in {}", code.toString(), codeSystem.getId());
            return code;
        }

        return null;
    }
    
    public Code lookup (String valueSet) {
    	if (!valueSetIndex.containsKey(valueSet)) {
    		ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
    		Iterable<Code> codes = expandInternal(valueSetInfo);
    		return codes.iterator().next();
    	}
    	
    	Code code = valueSetIndex.get(valueSet).iterator().next();
    	return code;
    }
    
    private void initValueSet(IBaseResource resource) {
    	String url = ValueSets.getUrl(fhirContext, resource);
        Iterable<Code> codes = ValueSets.getCodesInExpansion(this.fhirContext, resource);

        if (codes == null) {
            logger.warn("ValueSet {} is not expanded. Falling back to compose definition. This will potentially produce incorrect results. ", url);
            codes = ValueSets.getCodesInCompose(this.fhirContext, resource);
        } else {
            Boolean isNaiveExpansion = isNaiveExpansion(resource);
            if (isNaiveExpansion != null && isNaiveExpansion) {
                logger.warn("Codes expanded without a terminology server, some results may not be correct.");     
            }
        }

        if (codes == null) {
            codes = Collections.emptySet();
        }

        this.valueSetIndex.put(url, codes);
    }

    @SuppressWarnings("unchecked")
    private Boolean isNaiveExpansion(IBaseResource resource) {
        IBase expansion = ValueSets.getExpansion(this.fhirContext, resource);
        if (expansion != null) {
            Object object = ValueSets.getExpansionParameters(expansion, fhirPath, ".where(name = 'naive').value");
            if (object instanceof IBase) {
                return resolveNaiveBoolean((IBase)object);
            } else if (object instanceof Iterable) {
                List<IBase> naiveParameters = (List<IBase>) object;
                for (IBase param : naiveParameters) {
                    return resolveNaiveBoolean(param);
                }
            }
        }
        return null;
    }


    private Boolean resolveNaiveBoolean(IBase param) {
        if (param.fhirType().equals("boolean")) {
            return (Boolean)((IPrimitiveType<?>)param).getValue();
        } else {
            return null;
        }
    }

    
    private void checkExpansion(Iterable<Code> expandedCodes, ValueSetInfo valueSet) {
        if (expandedCodes != null && !Iterables.isEmpty(expandedCodes)) {
            return;
        }

        IBaseResource resource = null;
        for (IBaseResource res : this.valueSets) {
            String idPart = res.getIdElement().getIdPart();
            String versionIdPart = res.getIdElement().getVersionIdPart();
            if (valueSet.getId().equals(idPart) || valueSet.getId().endsWith(idPart) || valueSet.getId().endsWith(idPart + "|" + versionIdPart)) {
                resource = res;
            }
        }

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to locate ValueSet %s", valueSet.getId()));
        }

        if (containsExpansionLogic(resource)) {
            String msg = "ValueSet {} not expanded and compose contained expansion logic.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }


    private boolean containsExpansionLogic(IBaseResource resource) {
        List<IBase> includeFilters = ValueSets.getIncludeFilters(this.fhirContext, resource);
        if (includeFilters != null && !includeFilters.isEmpty()) {
            return true;
        }
        List<IBase> excludeFilters = ValueSets.getExcludeFilters(this.fhirContext, resource);
        if (excludeFilters != null && !excludeFilters.isEmpty()) {
            return true;
        }
        return false;
    }

}