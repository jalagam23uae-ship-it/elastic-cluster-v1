package com.fednow.iso20022.service;

import com.fednow.iso20022.domain.admi.Admi002;
import com.fednow.iso20022.domain.admi.Admi007;
import com.fednow.iso20022.domain.camt.*;
import com.fednow.iso20022.domain.pacs.*;
import com.fednow.iso20022.domain.pain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Service for XML marshalling and unmarshalling of ISO 20022 messages.
 *
 * Provides functionality for:
 * - Parsing XML to Java objects (unmarshal)
 * - Generating XML from Java objects (marshal)
 * - XML validation
 * - Support for all ISO 20022 message types (pain, pacs, camt, admi)
 */
@Service
@Slf4j
public class XmlMarshallingService {

    private final XmlMapper xmlMapper;
    private final ObjectMapper objectMapper;

    public XmlMarshallingService() {
        this.xmlMapper = createXmlMapper();
        this.objectMapper = createObjectMapper();
    }

    /**
     * Creates and configures XML mapper.
     */
    private XmlMapper createXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Creates and configures JSON object mapper.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // ========================================
    // PAIN (Customer Payments) Messages
    // ========================================

    /**
     * Parse pain.001 XML to Pain001 object.
     */
    public Mono<Pain001> unmarshalPain001(String xml) {
        log.debug("Unmarshalling pain.001 XML");
        return Mono.fromCallable(() -> {
            Pain001 message = xmlMapper.readValue(xml, Pain001.class);
            log.info("Successfully unmarshalled pain.001: messageId={}",
                    message.getGroupHeader().getMessageId());
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.001", e));
    }

    /**
     * Generate pain.001 XML from Pain001 object.
     */
    public Mono<String> marshalPain001(Pain001 pain001) {
        log.debug("Marshalling pain.001: messageId={}",
                pain001.getGroupHeader().getMessageId());
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain001);
            log.info("Successfully marshalled pain.001: messageId={}",
                    pain001.getGroupHeader().getMessageId());
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.001", e));
    }

    /**
     * Parse pain.002 XML to Pain002 object.
     */
    public Mono<Pain002> unmarshalPain002(String xml) {
        log.debug("Unmarshalling pain.002 XML");
        return Mono.fromCallable(() -> {
            Pain002 message = xmlMapper.readValue(xml, Pain002.class);
            log.info("Successfully unmarshalled pain.002");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.002", e));
    }

    /**
     * Generate pain.002 XML from Pain002 object.
     */
    public Mono<String> marshalPain002(Pain002 pain002) {
        log.debug("Marshalling pain.002");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain002);
            log.info("Successfully marshalled pain.002");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.002", e));
    }

    /**
     * Parse pain.007 XML to Pain007 object.
     */
    public Mono<Pain007> unmarshalPain007(String xml) {
        log.debug("Unmarshalling pain.007 XML");
        return Mono.fromCallable(() -> {
            Pain007 message = xmlMapper.readValue(xml, Pain007.class);
            log.info("Successfully unmarshalled pain.007");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.007", e));
    }

    /**
     * Generate pain.007 XML from Pain007 object.
     */
    public Mono<String> marshalPain007(Pain007 pain007) {
        log.debug("Marshalling pain.007");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain007);
            log.info("Successfully marshalled pain.007");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.007", e));
    }

    /**
     * Parse pain.008 XML to Pain008 object.
     */
    public Mono<Pain008> unmarshalPain008(String xml) {
        log.debug("Unmarshalling pain.008 XML");
        return Mono.fromCallable(() -> {
            Pain008 message = xmlMapper.readValue(xml, Pain008.class);
            log.info("Successfully unmarshalled pain.008");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.008", e));
    }

    /**
     * Generate pain.008 XML from Pain008 object.
     */
    public Mono<String> marshalPain008(Pain008 pain008) {
        log.debug("Marshalling pain.008");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain008);
            log.info("Successfully marshalled pain.008");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.008", e));
    }

    /**
     * Parse pain.009 XML to Pain009 object.
     */
    public Mono<Pain009> unmarshalPain009(String xml) {
        log.debug("Unmarshalling pain.009 XML");
        return Mono.fromCallable(() -> {
            Pain009 message = xmlMapper.readValue(xml, Pain009.class);
            log.info("Successfully unmarshalled pain.009");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.009", e));
    }

    /**
     * Generate pain.009 XML from Pain009 object.
     */
    public Mono<String> marshalPain009(Pain009 pain009) {
        log.debug("Marshalling pain.009");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain009);
            log.info("Successfully marshalled pain.009");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.009", e));
    }

    /**
     * Parse pain.013 XML to Pain013 object.
     */
    public Mono<Pain013> unmarshalPain013(String xml) {
        log.debug("Unmarshalling pain.013 XML");
        return Mono.fromCallable(() -> {
            Pain013 message = xmlMapper.readValue(xml, Pain013.class);
            log.info("Successfully unmarshalled pain.013");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pain.013", e));
    }

    /**
     * Generate pain.013 XML from Pain013 object.
     */
    public Mono<String> marshalPain013(Pain013 pain013) {
        log.debug("Marshalling pain.013");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pain013);
            log.info("Successfully marshalled pain.013");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pain.013", e));
    }

    // ========================================
    // PACS (Payments Clearing and Settlement) Messages
    // ========================================

    /**
     * Parse pacs.002 XML to Pacs002 object.
     */
    public Mono<Pacs002> unmarshalPacs002(String xml) {
        log.debug("Unmarshalling pacs.002 XML");
        return Mono.fromCallable(() -> {
            Pacs002 message = xmlMapper.readValue(xml, Pacs002.class);
            log.info("Successfully unmarshalled pacs.002");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.002", e));
    }

    /**
     * Generate pacs.002 XML from Pacs002 object.
     */
    public Mono<String> marshalPacs002(Pacs002 pacs002) {
        log.debug("Marshalling pacs.002");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs002);
            log.info("Successfully marshalled pacs.002");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.002", e));
    }

    /**
     * Parse pacs.003 XML to Pacs003 object.
     */
    public Mono<Pacs003> unmarshalPacs003(String xml) {
        log.debug("Unmarshalling pacs.003 XML");
        return Mono.fromCallable(() -> {
            Pacs003 message = xmlMapper.readValue(xml, Pacs003.class);
            log.info("Successfully unmarshalled pacs.003");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.003", e));
    }

    /**
     * Generate pacs.003 XML from Pacs003 object.
     */
    public Mono<String> marshalPacs003(Pacs003 pacs003) {
        log.debug("Marshalling pacs.003");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs003);
            log.info("Successfully marshalled pacs.003");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.003", e));
    }

    /**
     * Parse pacs.004 XML to Pacs004 object.
     */
    public Mono<Pacs004> unmarshalPacs004(String xml) {
        log.debug("Unmarshalling pacs.004 XML");
        return Mono.fromCallable(() -> {
            Pacs004 message = xmlMapper.readValue(xml, Pacs004.class);
            log.info("Successfully unmarshalled pacs.004");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.004", e));
    }

    /**
     * Generate pacs.004 XML from Pacs004 object.
     */
    public Mono<String> marshalPacs004(Pacs004 pacs004) {
        log.debug("Marshalling pacs.004");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs004);
            log.info("Successfully marshalled pacs.004");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.004", e));
    }

    /**
     * Parse pacs.007 XML to Pacs007 object.
     */
    public Mono<Pacs007> unmarshalPacs007(String xml) {
        log.debug("Unmarshalling pacs.007 XML");
        return Mono.fromCallable(() -> {
            Pacs007 message = xmlMapper.readValue(xml, Pacs007.class);
            log.info("Successfully unmarshalled pacs.007");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.007", e));
    }

    /**
     * Generate pacs.007 XML from Pacs007 object.
     */
    public Mono<String> marshalPacs007(Pacs007 pacs007) {
        log.debug("Marshalling pacs.007");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs007);
            log.info("Successfully marshalled pacs.007");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.007", e));
    }

    /**
     * Parse pacs.008 XML to Pacs008 object.
     */
    public Mono<Pacs008> unmarshalPacs008(String xml) {
        log.debug("Unmarshalling pacs.008 XML");
        return Mono.fromCallable(() -> {
            Pacs008 message = xmlMapper.readValue(xml, Pacs008.class);
            log.info("Successfully unmarshalled pacs.008: messageId={}",
                    message.getGroupHeader().getMessageId());
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.008", e));
    }

    /**
     * Generate pacs.008 XML from Pacs008 object.
     */
    public Mono<String> marshalPacs008(Pacs008 pacs008) {
        log.debug("Marshalling pacs.008: messageId={}",
                pacs008.getGroupHeader().getMessageId());
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs008);
            log.info("Successfully marshalled pacs.008: messageId={}",
                    pacs008.getGroupHeader().getMessageId());
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.008", e));
    }

    /**
     * Parse pacs.028 XML to Pacs028 object.
     */
    public Mono<Pacs028> unmarshalPacs028(String xml) {
        log.debug("Unmarshalling pacs.028 XML");
        return Mono.fromCallable(() -> {
            Pacs028 message = xmlMapper.readValue(xml, Pacs028.class);
            log.info("Successfully unmarshalled pacs.028");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal pacs.028", e));
    }

    /**
     * Generate pacs.028 XML from Pacs028 object.
     */
    public Mono<String> marshalPacs028(Pacs028 pacs028) {
        log.debug("Marshalling pacs.028");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(pacs028);
            log.info("Successfully marshalled pacs.028");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal pacs.028", e));
    }

    // ========================================
    // CAMT (Cash Management) Messages
    // ========================================

    /**
     * Parse camt.029 XML to Camt029 object.
     */
    public Mono<Camt029> unmarshalCamt029(String xml) {
        log.debug("Unmarshalling camt.029 XML");
        return Mono.fromCallable(() -> {
            Camt029 message = xmlMapper.readValue(xml, Camt029.class);
            log.info("Successfully unmarshalled camt.029");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal camt.029", e));
    }

    /**
     * Generate camt.029 XML from Camt029 object.
     */
    public Mono<String> marshalCamt029(Camt029 camt029) {
        log.debug("Marshalling camt.029");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(camt029);
            log.info("Successfully marshalled camt.029");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal camt.029", e));
    }

    /**
     * Parse camt.052 XML to Camt052 object.
     */
    public Mono<Camt052> unmarshalCamt052(String xml) {
        log.debug("Unmarshalling camt.052 XML");
        return Mono.fromCallable(() -> {
            Camt052 message = xmlMapper.readValue(xml, Camt052.class);
            log.info("Successfully unmarshalled camt.052");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal camt.052", e));
    }

    /**
     * Generate camt.052 XML from Camt052 object.
     */
    public Mono<String> marshalCamt052(Camt052 camt052) {
        log.debug("Marshalling camt.052");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(camt052);
            log.info("Successfully marshalled camt.052");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal camt.052", e));
    }

    /**
     * Parse camt.053 XML to Camt053 object.
     */
    public Mono<Camt053> unmarshalCamt053(String xml) {
        log.debug("Unmarshalling camt.053 XML");
        return Mono.fromCallable(() -> {
            Camt053 message = xmlMapper.readValue(xml, Camt053.class);
            log.info("Successfully unmarshalled camt.053");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal camt.053", e));
    }

    /**
     * Generate camt.053 XML from Camt053 object.
     */
    public Mono<String> marshalCamt053(Camt053 camt053) {
        log.debug("Marshalling camt.053");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(camt053);
            log.info("Successfully marshalled camt.053");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal camt.053", e));
    }

    /**
     * Parse camt.054 XML to Camt054 object.
     */
    public Mono<Camt054> unmarshalCamt054(String xml) {
        log.debug("Unmarshalling camt.054 XML");
        return Mono.fromCallable(() -> {
            Camt054 message = xmlMapper.readValue(xml, Camt054.class);
            log.info("Successfully unmarshalled camt.054");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal camt.054", e));
    }

    /**
     * Generate camt.054 XML from Camt054 object.
     */
    public Mono<String> marshalCamt054(Camt054 camt054) {
        log.debug("Marshalling camt.054");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(camt054);
            log.info("Successfully marshalled camt.054");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal camt.054", e));
    }

    /**
     * Parse camt.056 XML to Camt056 object.
     */
    public Mono<Camt056> unmarshalCamt056(String xml) {
        log.debug("Unmarshalling camt.056 XML");
        return Mono.fromCallable(() -> {
            Camt056 message = xmlMapper.readValue(xml, Camt056.class);
            log.info("Successfully unmarshalled camt.056");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal camt.056", e));
    }

    /**
     * Generate camt.056 XML from Camt056 object.
     */
    public Mono<String> marshalCamt056(Camt056 camt056) {
        log.debug("Marshalling camt.056");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(camt056);
            log.info("Successfully marshalled camt.056");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal camt.056", e));
    }

    // ========================================
    // ADMI (Administration) Messages
    // ========================================

    /**
     * Parse admi.002 XML to Admi002 object.
     */
    public Mono<Admi002> unmarshalAdmi002(String xml) {
        log.debug("Unmarshalling admi.002 XML");
        return Mono.fromCallable(() -> {
            Admi002 message = xmlMapper.readValue(xml, Admi002.class);
            log.info("Successfully unmarshalled admi.002");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal admi.002", e));
    }

    /**
     * Generate admi.002 XML from Admi002 object.
     */
    public Mono<String> marshalAdmi002(Admi002 admi002) {
        log.debug("Marshalling admi.002");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(admi002);
            log.info("Successfully marshalled admi.002");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal admi.002", e));
    }

    /**
     * Parse admi.007 XML to Admi007 object.
     */
    public Mono<Admi007> unmarshalAdmi007(String xml) {
        log.debug("Unmarshalling admi.007 XML");
        return Mono.fromCallable(() -> {
            Admi007 message = xmlMapper.readValue(xml, Admi007.class);
            log.info("Successfully unmarshalled admi.007");
            return message;
        }).doOnError(e -> log.error("Failed to unmarshal admi.007", e));
    }

    /**
     * Generate admi.007 XML from Admi007 object.
     */
    public Mono<String> marshalAdmi007(Admi007 admi007) {
        log.debug("Marshalling admi.007");
        return Mono.fromCallable(() -> {
            String xml = xmlMapper.writeValueAsString(admi007);
            log.info("Successfully marshalled admi.007");
            return xml;
        }).doOnError(e -> log.error("Failed to marshal admi.007", e));
    }

    // ========================================
    // Generic XML Operations
    // ========================================

    /**
     * Validate XML format.
     *
     * @param xml XML string to validate
     * @return true if valid XML, false otherwise
     */
    public Mono<Boolean> isValidXml(String xml) {
        log.debug("Validating XML format");
        return Mono.fromCallable(() -> {
            if (xml == null || xml.trim().isEmpty()) {
                return false;
            }
            try {
                // Attempt to parse as XML
                xmlMapper.readTree(xml);
                log.debug("XML is valid");
                return true;
            } catch (Exception e) {
                log.warn("Invalid XML: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Pretty-print XML with indentation.
     *
     * @param xml XML string to format
     * @return formatted XML string
     */
    public Mono<String> prettyPrintXml(String xml) {
        log.debug("Pretty-printing XML");
        return Mono.fromCallable(() -> {
            Object obj = xmlMapper.readValue(xml, Object.class);
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }).doOnError(e -> log.error("Failed to pretty-print XML", e));
    }

    /**
     * Minify XML (remove whitespace).
     *
     * @param xml XML string to minify
     * @return minified XML string
     */
    public Mono<String> minifyXml(String xml) {
        log.debug("Minifying XML");
        return Mono.fromCallable(() -> {
            Object obj = xmlMapper.readValue(xml, Object.class);
            return xmlMapper.writeValueAsString(obj);
        }).doOnError(e -> log.error("Failed to minify XML", e));
    }

    /**
     * Convert XML to JSON.
     *
     * @param xml XML string to convert
     * @return JSON string
     */
    public Mono<String> xmlToJson(String xml) {
        log.debug("Converting XML to JSON");
        return Mono.fromCallable(() -> {
            Object obj = xmlMapper.readValue(xml, Object.class);
            return objectMapper.writeValueAsString(obj);
        }).doOnError(e -> log.error("Failed to convert XML to JSON", e));
    }

    /**
     * Convert JSON to XML.
     *
     * @param json JSON string to convert
     * @return XML string
     */
    public Mono<String> jsonToXml(String json) {
        log.debug("Converting JSON to XML");
        return Mono.fromCallable(() -> {
            Object obj = objectMapper.readValue(json, Object.class);
            return xmlMapper.writeValueAsString(obj);
        }).doOnError(e -> log.error("Failed to convert JSON to XML", e));
    }
}
