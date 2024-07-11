package org.openmrs.module.cql;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.api.APIException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FhirResourceLoader {

    private final FhirContext fhirContext;
    private final Class<?> relativeToClazz;
    private final List<IBaseResource> resources;

    public FhirResourceLoader(FhirContext fhirContext, Class<?> clazz,
                              List<String> directoryList) {
        this(fhirContext, clazz, directoryList, false);
    }
    public FhirResourceLoader(FhirContext fhirContext, Class<?> clazz,
                              List<String> directoryList, boolean recursive) {
        this.fhirContext = fhirContext;
        this.relativeToClazz = clazz;
        this.resources = directoryList.stream()
                .map(this::getDirectoryOrFileLocation)
                .filter(Objects::nonNull)
                .map(d -> this.getFilePaths(d, recursive))
                .flatMap(Collection::stream)
                .map(this::loadResource)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<IBaseResource> getResources() {
        return resources;
    }

    private List<String> getFilePaths(String directoryPath, boolean recursive) {
        var filePaths = new ArrayList<String>();
        File inputDir = new File(directoryPath);
        var files = inputDir.isDirectory()
                ? new ArrayList<>(Arrays.asList(
                Optional.ofNullable(inputDir.listFiles()).orElseThrow(NoSuchElementException::new)))
                : new ArrayList<File>();

        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    filePaths.addAll(getFilePaths(file.getPath(), recursive));
                }
            } else {
                if (!file.getPath().endsWith(".cql")) filePaths.add(file.getPath());
            }
        }
        return filePaths;
    }

    private IBaseResource loadResource(String file) {
        String resourceString = stringFromResource(file);
        if (resourceString == null) {
            return null;
        }

        if (file.endsWith("json")) {
            return loadResource(this.fhirContext, "json", resourceString);
        } else {
            return loadResource(this.fhirContext, "xml", resourceString);
        }
    }

    private IBaseResource loadResource(FhirContext context, String encoding, String resourceString) {
        return parseResource(context, encoding, resourceString);
    }

    private IBaseResource parseResource(FhirContext context, String encoding, String resourceString) {
        IParser parser;
        switch (encoding.toLowerCase()) {
            case "json":
                parser = context.newJsonParser();
                break;
            case "xml":
                parser = context.newXmlParser();
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Expected encoding xml, or json.  %s is not a valid encoding", encoding));
        }

        return parser.parseResource(resourceString);
    }

    private String stringFromResource(String location) {
        try (FileInputStream fis = new FileInputStream(location)) {
            return IOUtils.toString(fis, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new APIException(e);
        }
    }

    private String getDirectoryOrFileLocation(String relativePath) {
        var resource = this.relativeToClazz.getResource(relativePath);

        if (resource == null) {
            return null;
        }

        String directoryLocationUrl = resource.toString();

        if (directoryLocationUrl.startsWith("file:/")) {
            directoryLocationUrl = directoryLocationUrl.substring("file:/".length() - 1);
        }
        return directoryLocationUrl;
    }
}
