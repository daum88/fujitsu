package com.deliveryfee.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesUuidWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInMdc = new String[1];
        FilterChain chain = (req, res) -> seenInMdc[0] = MDC.get(RequestIdFilter.MDC_KEY);

        filter.doFilter(request, response, chain);

        String id = response.getHeader(RequestIdFilter.HEADER);
        assertThat(id).isNotBlank();
        assertThat(request.getAttribute(RequestIdFilter.ATTRIBUTE)).isEqualTo(id);
        assertThat(seenInMdc[0]).isEqualTo(id);
    }

    @Test
    void reusesIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "caller-supplied-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("caller-supplied-id");
        assertThat(request.getAttribute(RequestIdFilter.ATTRIBUTE)).isEqualTo("caller-supplied-id");
    }

    @Test
    void sanitizesIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "caller id\nwith:bad/chars");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("calleridwithbadchars");
    }
}
