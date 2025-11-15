package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.domain.admi.Admi002;
import com.fednow.iso20022.domain.camt.Camt054;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain001;
import com.fednow.iso20022.domain.pain.Pain002;
import com.fednow.iso20022.service.XmlMarshallingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST API Controller for ISO 20022 XML Operations.
 *
 * Provides endpoints for:
 * - XML parsing (unmarshal) to Java objects
 * - XML generation (marshal) from Java objects
 * - XML validation and formatting
 * - XML/JSON conversion
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/xml")
@RequiredArgsConstructor
@Tag(name = "XML Operations", description = "ISO 20022 XML parsing, generation, and utilities")
public class XmlController {

    private final XmlMarshallingService xmlService;

    // ========================================
    // XML Parsing (Unmarshal)
    // ========================================

    @Operation(
            summary = "Parse pain.001 XML",
            description = """
                    Parses pain.001 (Customer Credit Transfer Initiation) XML into Java object.

                    **Use Case:** Receive XML from customer and parse into domain object for processing
                    """
    )
    @PostMapping(value = "/parse/pain001",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pain001>> parsePain001(
            @Parameter(description = "pain.001 XML", required = true)
            @RequestBody String xml) {

        log.info("Parsing pain.001 XML");

        return xmlService.unmarshalPain001(xml)
                .map(pain001 -> ApiResponse.success(pain001, "pain.001 parsed successfully"))
                .doOnError(e -> log.error("Failed to parse pain.001", e));
    }

    @Operation(
            summary = "Parse pacs.008 XML",
            description = """
                    Parses pacs.008 (FI to FI Customer Credit Transfer) XML into Java object.

                    **Use Case:** Receive XML from FedNow network and parse for processing
                    """
    )
    @PostMapping(value = "/parse/pacs008",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs008>> parsePacs008(
            @Parameter(description = "pacs.008 XML", required = true)
            @RequestBody String xml) {

        log.info("Parsing pacs.008 XML");

        return xmlService.unmarshalPacs008(xml)
                .map(pacs008 -> ApiResponse.success(pacs008, "pacs.008 parsed successfully"))
                .doOnError(e -> log.error("Failed to parse pacs.008", e));
    }

    @Operation(
            summary = "Parse pacs.002 XML",
            description = """
                    Parses pacs.002 (Payment Status Report) XML into Java object.

                    **Use Case:** Receive status report from FedNow and parse for processing
                    """
    )
    @PostMapping(value = "/parse/pacs002",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs002>> parsePacs002(
            @Parameter(description = "pacs.002 XML", required = true)
            @RequestBody String xml) {

        log.info("Parsing pacs.002 XML");

        return xmlService.unmarshalPacs002(xml)
                .map(pacs002 -> ApiResponse.success(pacs002, "pacs.002 parsed successfully"))
                .doOnError(e -> log.error("Failed to parse pacs.002", e));
    }

    @Operation(
            summary = "Parse camt.054 XML",
            description = """
                    Parses camt.054 (Debit/Credit Notification) XML into Java object.

                    **Use Case:** Receive account notification and parse for processing
                    """
    )
    @PostMapping(value = "/parse/camt054",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Camt054>> parseCamt054(
            @Parameter(description = "camt.054 XML", required = true)
            @RequestBody String xml) {

        log.info("Parsing camt.054 XML");

        return xmlService.unmarshalCamt054(xml)
                .map(camt054 -> ApiResponse.success(camt054, "camt.054 parsed successfully"))
                .doOnError(e -> log.error("Failed to parse camt.054", e));
    }

    // ========================================
    // XML Generation (Marshal)
    // ========================================

    @Operation(
            summary = "Generate pain.001 XML",
            description = """
                    Generates pain.001 (Customer Credit Transfer Initiation) XML from Java object.

                    **Use Case:** Create XML message for customer payment initiation
                    """
    )
    @PostMapping(value = "/generate/pain001",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> generatePain001(
            @Parameter(description = "pain.001 object", required = true)
            @RequestBody Pain001 pain001) {

        log.info("Generating pain.001 XML: messageId={}",
                pain001.getGroupHeader().getMessageId());

        return xmlService.marshalPain001(pain001)
                .doOnSuccess(xml -> log.info("pain.001 XML generated successfully"))
                .doOnError(e -> log.error("Failed to generate pain.001 XML", e));
    }

    @Operation(
            summary = "Generate pacs.008 XML",
            description = """
                    Generates pacs.008 (FI to FI Customer Credit Transfer) XML from Java object.

                    **Use Case:** Create XML message for FedNow network transmission
                    """
    )
    @PostMapping(value = "/generate/pacs008",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> generatePacs008(
            @Parameter(description = "pacs.008 object", required = true)
            @RequestBody Pacs008 pacs008) {

        log.info("Generating pacs.008 XML: messageId={}",
                pacs008.getGroupHeader().getMessageId());

        return xmlService.marshalPacs008(pacs008)
                .doOnSuccess(xml -> log.info("pacs.008 XML generated successfully"))
                .doOnError(e -> log.error("Failed to generate pacs.008 XML", e));
    }

    @Operation(
            summary = "Generate pacs.002 XML",
            description = """
                    Generates pacs.002 (Payment Status Report) XML from Java object.

                    **Use Case:** Create status report XML for network transmission
                    """
    )
    @PostMapping(value = "/generate/pacs002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> generatePacs002(
            @Parameter(description = "pacs.002 object", required = true)
            @RequestBody Pacs002 pacs002) {

        log.info("Generating pacs.002 XML");

        return xmlService.marshalPacs002(pacs002)
                .doOnSuccess(xml -> log.info("pacs.002 XML generated successfully"))
                .doOnError(e -> log.error("Failed to generate pacs.002 XML", e));
    }

    @Operation(
            summary = "Generate pain.002 XML",
            description = """
                    Generates pain.002 (Customer Payment Status Report) XML from Java object.

                    **Use Case:** Create customer status notification XML
                    """
    )
    @PostMapping(value = "/generate/pain002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> generatePain002(
            @Parameter(description = "pain.002 object", required = true)
            @RequestBody Pain002 pain002) {

        log.info("Generating pain.002 XML");

        return xmlService.marshalPain002(pain002)
                .doOnSuccess(xml -> log.info("pain.002 XML generated successfully"))
                .doOnError(e -> log.error("Failed to generate pain.002 XML", e));
    }

    @Operation(
            summary = "Generate admi.002 XML",
            description = """
                    Generates admi.002 (System Event Notification) XML from Java object.

                    **Use Case:** Create system event notification XML
                    """
    )
    @PostMapping(value = "/generate/admi002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> generateAdmi002(
            @Parameter(description = "admi.002 object", required = true)
            @RequestBody Admi002 admi002) {

        log.info("Generating admi.002 XML");

        return xmlService.marshalAdmi002(admi002)
                .doOnSuccess(xml -> log.info("admi.002 XML generated successfully"))
                .doOnError(e -> log.error("Failed to generate admi.002 XML", e));
    }

    // ========================================
    // XML Utilities
    // ========================================

    @Operation(
            summary = "Validate XML",
            description = """
                    Validates if provided string is well-formed XML.

                    **Use Case:** Pre-validation before processing XML messages
                    """
    )
    @PostMapping(value = "/validate",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Boolean>> validateXml(
            @Parameter(description = "XML to validate", required = true)
            @RequestBody String xml) {

        log.debug("Validating XML");

        return xmlService.isValidXml(xml)
                .map(isValid -> ApiResponse.success(isValid,
                        isValid ? "XML is valid" : "XML is invalid"))
                .doOnSuccess(response -> log.info("XML validation result: {}", response.getData()));
    }

    @Operation(
            summary = "Pretty-print XML",
            description = """
                    Formats XML with proper indentation for readability.

                    **Use Case:** Format XML for logging or display purposes
                    """
    )
    @PostMapping(value = "/pretty-print",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> prettyPrintXml(
            @Parameter(description = "XML to format", required = true)
            @RequestBody String xml) {

        log.debug("Pretty-printing XML");

        return xmlService.prettyPrintXml(xml)
                .doOnSuccess(formatted -> log.debug("XML formatted successfully"))
                .doOnError(e -> log.error("Failed to pretty-print XML", e));
    }

    @Operation(
            summary = "Minify XML",
            description = """
                    Removes unnecessary whitespace from XML to reduce size.

                    **Use Case:** Optimize XML for network transmission
                    """
    )
    @PostMapping(value = "/minify",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> minifyXml(
            @Parameter(description = "XML to minify", required = true)
            @RequestBody String xml) {

        log.debug("Minifying XML");

        return xmlService.minifyXml(xml)
                .doOnSuccess(minified -> log.debug("XML minified successfully"))
                .doOnError(e -> log.error("Failed to minify XML", e));
    }

    @Operation(
            summary = "Convert XML to JSON",
            description = """
                    Converts XML to JSON format.

                    **Use Case:** Transform XML messages to JSON for REST APIs or modern applications
                    """
    )
    @PostMapping(value = "/to-json",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> xmlToJson(
            @Parameter(description = "XML to convert", required = true)
            @RequestBody String xml) {

        log.debug("Converting XML to JSON");

        return xmlService.xmlToJson(xml)
                .doOnSuccess(json -> log.debug("XML converted to JSON successfully"))
                .doOnError(e -> log.error("Failed to convert XML to JSON", e));
    }

    @Operation(
            summary = "Convert JSON to XML",
            description = """
                    Converts JSON to XML format.

                    **Use Case:** Transform JSON messages to XML for ISO 20022 compliance
                    """
    )
    @PostMapping(value = "/to-xml",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public Mono<String> jsonToXml(
            @Parameter(description = "JSON to convert", required = true)
            @RequestBody String json) {

        log.debug("Converting JSON to XML");

        return xmlService.jsonToXml(json)
                .doOnSuccess(xml -> log.debug("JSON converted to XML successfully"))
                .doOnError(e -> log.error("Failed to convert JSON to XML", e));
    }
}
