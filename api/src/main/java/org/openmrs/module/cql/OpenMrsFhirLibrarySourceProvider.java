package org.openmrs.module.cql;

import java.util.ArrayList;
import java.util.Collection;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BaseFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

public class OpenMrsFhirLibrarySourceProvider extends BaseFhirLibrarySourceProvider {

	private LibraryVersionSelector libraryVersionSelector;
	private Collection<IBaseResource> libraries = new ArrayList<>();
	
	protected OpenMrsFhirLibrarySourceProvider(AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
		super(adapterFactory);
		
		this.libraryVersionSelector = libraryVersionSelector;
	}

	@Override
	protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
		
		return libraryVersionSelector.select(libraryIdentifier, libraries);
	}

	public void add(IBaseResource library) {
		libraries.add(library);
	}
}
