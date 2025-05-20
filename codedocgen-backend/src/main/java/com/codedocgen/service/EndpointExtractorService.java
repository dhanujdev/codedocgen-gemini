package com.codedocgen.service;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;
import java.io.File;
import java.util.List;

public interface EndpointExtractorService {
    List<EndpointMetadata> extractEndpoints(List<ClassMetadata> classMetadataList, File projectDir);
    // This service might need to inspect WSDL files for SOAP, and annotations for REST.
    // It will use ClassMetadata as input to find relevant classes (controllers, @WebService annotated classes).
} 