package ma.lawflux.platform.web;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    public static String normalize(String value) {
        return value != null && VALID.matcher(value).matches() ? value : UUID.randomUUID().toString();
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = normalize(req.getHeader(HEADER));
        res.setHeader(HEADER, id);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", id)) { chain.doFilter(req, res); }
    }
}
