package com.deliveryfee.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@SpringBootTest
@Transactional
class WeatherImportServiceTest {

    private static final long FEED_TIMESTAMP = 1_762_000_000L;

    @Autowired private WeatherObservationRepository repository;

    @Autowired private XmlMapper xmlMapper;

    private MockWebServer server;
    private WeatherImportService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient client = RestClient.builder().baseUrl(server.url("/").toString()).build();
        service = new WeatherImportService(client, xmlMapper, repository);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void importLatest_onlyRelevantStationsAreImported() {
        enqueueSample();

        ImportResult result = service.importLatest();

        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.skipped()).isZero();
        assertThat(result.missingStations()).isEmpty();
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findAll())
                .extracting(WeatherObservation::getStationName)
                .containsExactlyInAnyOrder("Tallinn-Harku", "Tartu-Tõravere", "Pärnu");
    }

    @Test
    void importLatest_parsesValuesAndMapsBlankFieldsToNull() {
        enqueueSample();

        service.importLatest();

        WeatherObservation tartu = findLatest("Tartu-Tõravere");
        assertThat(tartu.getAirTemperature()).isEqualByComparingTo(new BigDecimal("-12.0"));
        assertThat(tartu.getWindSpeed()).isEqualByComparingTo(new BigDecimal("3.4"));
        assertThat(tartu.getPhenomenon()).isNull();

        WeatherObservation parnu = findLatest("Pärnu");
        assertThat(parnu.getWindSpeed()).isNull();
        assertThat(parnu.getPhenomenon()).isEqualTo("Moderate rain");
    }

    @Test
    void importLatest_rerunIsIdempotent() {
        enqueueSample();
        service.importLatest();

        enqueueSample();
        ImportResult second = service.importLatest();

        assertThat(second.imported()).isZero();
        assertThat(second.skipped()).isEqualTo(3);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void importLatest_observedAtDerivedFromFeedTimestamp() {
        enqueueSample();

        service.importLatest();

        assertThat(repository.findAll())
                .allSatisfy(
                        o ->
                                assertThat(o.getObservedAt())
                                        .isEqualTo(Instant.ofEpochSecond(FEED_TIMESTAMP)));
    }

    @Test
    void importLatest_missingStationIsReportedButOthersAreImported() {
        enqueue(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <observations timestamp="1762000000">
                  <station>
                    <name>Tallinn-Harku</name>
                    <wmocode>26038</wmocode>
                    <phenomenon></phenomenon>
                    <airtemperature>-2.5</airtemperature>
                    <windspeed>4.5</windspeed>
                  </station>
                  <station>
                    <name>Tartu-Tõravere</name>
                    <wmocode>26242</wmocode>
                    <phenomenon></phenomenon>
                    <airtemperature>-12.0</airtemperature>
                    <windspeed>3.4</windspeed>
                  </station>
                </observations>
                """);

        ImportResult result = service.importLatest();

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.missingStations()).containsExactly("Pärnu");
    }

    @Test
    void importLatest_apiErrorLeavesDatabaseUnchanged() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> service.importLatest()).isInstanceOf(Exception.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void importLatest_malformedXmlFailsWithoutPersisting() {
        enqueue("<observations><broken");

        assertThatThrownBy(() -> service.importLatest()).isInstanceOf(Exception.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void importLatest_maliciousDtdIsRejected() {
        enqueue(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE observations [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <observations timestamp="1762000000">
                  <station>
                    <name>Tallinn-Harku</name>
                    <airtemperature>&xxe;</airtemperature>
                  </station>
                </observations>
                """);

        assertThatThrownBy(() -> service.importLatest()).isInstanceOf(Exception.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void importLatest_missingTimestamp_throwsAndPersistsNothing() {
        enqueue(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <observations timestamp="0">
                  <station>
                    <name>Tallinn-Harku</name>
                    <airtemperature>-2.5</airtemperature>
                  </station>
                </observations>
                """);

        assertThatThrownBy(() -> service.importLatest()).isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isZero();
    }

    private void enqueueSample() {
        enqueue(readResource("weather-sample.xml"));
    }

    private void enqueue(String body) {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/xml")
                        .setBody(body));
    }

    private WeatherObservation findLatest(String stationName) {
        return repository
                .findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        stationName, Instant.now())
                .orElseThrow();
    }

    private String readResource(String name) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assert in != null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
