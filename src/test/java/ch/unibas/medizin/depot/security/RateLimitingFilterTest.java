package ch.unibas.medizin.depot.security;

import ch.unibas.medizin.depot.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitingFilterTest {

    @Test
    void allows_up_to_max_attempts_then_429() throws Exception {
        var props = new RateLimitProperties(true, 3, Duration.ofMinutes(15));
        var filter = new RateLimitingFilter(props);
        var chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            var req = request("/admin/register", "10.0.0.1");
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        }

        var blocked = new MockHttpServletResponse();
        filter.doFilter(request("/admin/register", "10.0.0.1"), blocked, chain);

        assertEquals(429, blocked.getStatus());
        assertEquals("application/problem+json", blocked.getContentType());
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void buckets_are_per_ip() throws Exception {
        var props = new RateLimitProperties(true, 1, Duration.ofMinutes(15));
        var filter = new RateLimitingFilter(props);
        var chain = mock(FilterChain.class);

        var resA = new MockHttpServletResponse();
        filter.doFilter(request("/admin/register", "10.0.0.1"), resA, chain);
        assertEquals(HttpServletResponse.SC_OK, resA.getStatus());

        var resB = new MockHttpServletResponse();
        filter.doFilter(request("/admin/register", "10.0.0.2"), resB, chain);
        assertEquals(HttpServletResponse.SC_OK, resB.getStatus());

        var resA2 = new MockHttpServletResponse();
        filter.doFilter(request("/admin/register", "10.0.0.1"), resA2, chain);
        assertEquals(429, resA2.getStatus());
    }

    @Test
    void unprotected_path_passes_through() throws Exception {
        var props = new RateLimitProperties(true, 1, Duration.ofMinutes(15));
        var filter = new RateLimitingFilter(props);
        var chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            var res = new MockHttpServletResponse();
            filter.doFilter(request("/list", "10.0.0.1"), res, chain);
            assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        }

        verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void disabled_passes_through() throws Exception {
        var props = new RateLimitProperties(false, 1, Duration.ofMinutes(15));
        var filter = new RateLimitingFilter(props);
        var chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            var res = new MockHttpServletResponse();
            filter.doFilter(request("/admin/register", "10.0.0.1"), res, chain);
            assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        }

        verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request(String uri, String remoteAddr) {
        var req = new MockHttpServletRequest("POST", uri);
        req.setRequestURI(uri);
        req.setRemoteAddr(remoteAddr);
        return req;
    }

}
