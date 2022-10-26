package org.aldebaran.agent.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.aldebaran.agent.constants.AgentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * IPServletFilter.
 *
 * @author Alejandro
 *
 */
@Component
@Order(1)
public class IPServletFilter implements Filter {

    @Autowired
    private Environment env;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;

        final String configAllowedIPs = env.getProperty(AgentProperties.SERVICE_ALLOWED_IPS);
        final String ipRemoteAddress = req.getRemoteAddr();

        SecurityChecker.checkAllowedIps(ipRemoteAddress, configAllowedIPs);

        chain.doFilter(request, response);
    }

    // other methods
}
