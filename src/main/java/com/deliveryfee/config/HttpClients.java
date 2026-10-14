package com.deliveryfee.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javax.xml.stream.XMLInputFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Centralised HTTP and XML infrastructure.
 *
 * <p>The {@link XmlMapper} is hardened against XXE / entity-expansion attacks: DTD support and
 * external entities are disabled, and entity reference replacement is switched off.</p>
 */
@Configuration
public class HttpClients {

    /**
     * Builds the {@link RestClient} used by the weather import with configured timeouts.
     *
     * @param properties validated application configuration
     * @return a configured {@link RestClient}
     */
    @Bean
    public RestClient weatherRestClient(AppProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.weatherImport().connectTimeout());
        requestFactory.setReadTimeout(properties.weatherImport().readTimeout());
        return RestClient.builder()
                .baseUrl(properties.weatherImport().url())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Builds a hardened {@link XmlMapper} that rejects DTDs and external entities and maps blank
     * strings to {@code null}.
     *
     * @return the hardened {@link XmlMapper}
     */
    @Bean
    public XmlMapper hardenedXmlMapper() {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

        XmlFactory xmlFactory = XmlFactory.builder().xmlInputFactory(inputFactory).build();
        XmlMapper mapper = new XmlMapper(xmlFactory);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new BlankToNullStringDeserializer());
        mapper.registerModule(module);
        return mapper;
    }
}
