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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.FhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.LibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.TypedFhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;

import ca.uhn.fhir.context.FhirContext;

public class MeasureProcessorUtil {
	
	private static OpenMrsTerminologyProvider terminologyProvider = null;
    private static OpenMrsFhirLibrarySourceProvider librarySourceProvider = null;
    private static BundleRetrieveProvider retrieveProvider = null;
    private static OpenMrsFhirDal fhirDal = null;

	@SuppressWarnings("serial")
	public static R4MeasureProcessor setup(FhirContext fhirContext, boolean threadedEnabled, int threadedBatchSize) {

		AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();

		LibraryVersionSelector libraryVersionSelector = new LibraryVersionSelector(adapterFactory);

		Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories = new HashSet<TypedLibrarySourceProviderFactory>() {
			{
				add(new TypedLibrarySourceProviderFactory() {
					@Override
					public String getType() {
						return Constants.HL7_FHIR_FILES;
					}

					@Override
					public LibrarySourceProvider create(String urls, List<String> headers) {
						librarySourceProvider = new OpenMrsFhirLibrarySourceProvider(adapterFactory, libraryVersionSelector);
						String[] urlArray = ("anc/libraries/Library-TX-PVLS.json," + 
						"anc/libraries/library-ANCIND07.json,"
						+ "anc/libraries/library-ANCBaseConcepts.json,"
						+ "anc/libraries/library-ANCContactDataElements.json,"
						+ "anc/libraries/library-ANCBaseDataElements.json,"
						+ "anc/libraries/library-ANCCommon.json,"
						+ "anc/libraries/library-FHIRCommon.json,"
						+ "anc/libraries/library-ANCDataElements.json,"
						+ "anc/libraries/library-WHOCommon.json,"
						+ "anc/libraries/library-ANCStratifiers.json,"
						+ "anc/libraries/library-ANCConcepts.json").split(",");
						
	                	for (String url : urlArray) {
	                		librarySourceProvider.add(fhirContext.newJsonParser()
									.parseResource(getClass().getResourceAsStream(url)));
	                	}
						return librarySourceProvider;
					}
				});
			}
		};

		Set<ModelResolverFactory> modelResolverFactories = new HashSet<ModelResolverFactory>() {
			{
				add(new FhirModelResolverFactory());
			}
		};

		LibrarySourceProviderFactory librarySourceProviderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory(
				fhirContext, adapterFactory, librarySourceProviderFactories, libraryVersionSelector);
		Set<TypedRetrieveProviderFactory> retrieveProviderFactories = new HashSet<TypedRetrieveProviderFactory>() {
			{
				add(new TypedRetrieveProviderFactory() {
					@Override
					public String getType() {
						return Constants.HL7_FHIR_FILES;
					}

					@Override
					public RetrieveProvider create(String url, List<String> headers) {
						if (retrieveProvider == null) {
							return new BundleRetrieveProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
									.parseResource(getClass().getResourceAsStream(url)));
						}
						
						return retrieveProvider;
					}
				});
			}
		};

		DataProviderFactory dataProviderFactory = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(
				fhirContext, modelResolverFactories, retrieveProviderFactories);

		Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories = new HashSet<TypedTerminologyProviderFactory>() {
			{
				add(new TypedTerminologyProviderFactory() {
					@Override
					public String getType() {
						return Constants.HL7_FHIR_FILES;
					}

					@Override
					public TerminologyProvider create(String url, List<String> headers) {
						if (terminologyProvider == null) {
	                		terminologyProvider = OpenMrsTerminologyProvider.getInstance(fhirContext);
	                	}
	                	return terminologyProvider;
					}
				});
			}
		};

		TerminologyProviderFactory terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(
				fhirContext, typedTerminologyProviderFactories);

		Set<TypedFhirDalFactory> fhirDalFactories = new HashSet<TypedFhirDalFactory>() {
			{
				add(new TypedFhirDalFactory() {
					@Override
					public String getType() {
						return Constants.HL7_FHIR_FILES;
					}

					@Override
					public FhirDal create(String urls, List<String> headers) {
						
						fhirDal = new OpenMrsFhirDal();
						
						String[] urlArray = urls.split(",");
	                	for (String url : urlArray) {
	                		fhirDal.addAll(fhirContext.newJsonParser()
									.parseResource(getClass().getResourceAsStream(url)));
	                	}

						return fhirDal;
					}
				});
			}
		};

		FhirDalFactory fhirDalFactory = new org.opencds.cqf.cql.evaluator.builder.dal.FhirDalFactory(fhirContext,
				fhirDalFactories);

		EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

		MeasureEvaluationOptions config = MeasureEvaluationOptions.defaultOptions();

		return new R4MeasureProcessor(terminologyProviderFactory, dataProviderFactory,
				librarySourceProviderFactory, fhirDalFactory, endpointConverter, null, null, null, null, config, null,
				null);

	}
}
