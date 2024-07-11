package org.openmrs.module.cql;

import java.util.ArrayList;
import java.util.Collection;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.cql2elm.content.BaseFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;

public class OpenMrsFhirLibrarySourceProvider extends BaseFhirLibrarySourceProvider {

	private final LibraryVersionSelector libraryVersionSelector;
	private final Collection<IBaseResource> libraries = new ArrayList<>();
	
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
