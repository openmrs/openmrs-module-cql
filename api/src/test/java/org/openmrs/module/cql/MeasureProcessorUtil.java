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
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor;

import ca.uhn.fhir.context.FhirContext;

public class MeasureProcessorUtil {

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
					public LibrarySourceProvider create(String url, List<String> headers) {
						return new BundleFhirLibrarySourceProvider(fhirContext,
								(IBaseBundle) fhirContext.newJsonParser()
										.parseResource(AncDakTest.class.getResourceAsStream(url)),
								adapterFactory, libraryVersionSelector);
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

						return new BundleRetrieveProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
								.parseResource(AncDakTest.class.getResourceAsStream(url)));
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
						return new BundleTerminologyProvider(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
								.parseResource(AncDakTest.class.getResourceAsStream(url)));
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
					public FhirDal create(String url, List<String> headers) {
						return new BundleFhirDal(fhirContext, (IBaseBundle) fhirContext.newJsonParser()
								.parseResource(AncDakTest.class.getResourceAsStream(url)));
					}
				});
			}
		};

		FhirDalFactory fhirDalFactory = new org.opencds.cqf.cql.evaluator.builder.dal.FhirDalFactory(fhirContext,
				fhirDalFactories);

		EndpointConverter endpointConverter = new EndpointConverter(adapterFactory);

		MeasureEvaluationOptions config = MeasureEvaluationOptions.defaultOptions();

		if (threadedEnabled) {
			config.setThreadedEnabled(true);
			config.setThreadedBatchSize(threadedBatchSize);
		}

		return new R4MeasureProcessor(terminologyProviderFactory, dataProviderFactory,
				librarySourceProviderFactory, fhirDalFactory, endpointConverter, null, null, null, null, config, null,
				null);

	}
}
