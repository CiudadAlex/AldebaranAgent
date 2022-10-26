package org.aldebaran.agent.security;

import java.util.List;

import javax.servlet.ServletException;

import org.aldebaran.common.constants.CommonConstants;
import org.aldebaran.common.utils.text.TextUtils;
import org.springframework.http.HttpHeaders;

/**
 * SecurityChecker.
 *
 * @author Alejandro
 *
 */
public final class SecurityChecker {

    private SecurityChecker() {
    }

    /**
     * Checks for security.
     *
     * @param headers
     *            headers
     * @param servicePassword
     *            servicePassword
     * @throws Exception
     */
    public static void securityCheck(final HttpHeaders headers, final String servicePassword) throws Exception {

        final List<String> listServicePasswordHeaderValue = headers.get(CommonConstants.HEADER_SERVICE_PASSWORD);

        if (listServicePasswordHeaderValue.isEmpty()) {
            throw new Exception("No password suplied");
        }

        final String servicePasswordHeaderValue = listServicePasswordHeaderValue.get(0);

        if (!servicePasswordHeaderValue.equals(servicePassword)) {
            throw new Exception("Wrong password");
        }
    }

    /**
     * Checks the allowed IPs.
     *
     * @param ipRemoteAddress
     *            ipRemoteAddress
     * @param configAllowedIPs
     *            configAllowedIPs
     * @throws Exception
     */
    public static void checkAllowedIps(final String ipRemoteAddress, final String configAllowedIPs) throws ServletException {

        if ("127.0.0.1".equals(ipRemoteAddress)) {
            return;
        }

        if ("*".equals(configAllowedIPs)) {
            return;
        }

        final List<String> listTokensIp = TextUtils.divideString(ipRemoteAddress, ".");

        final List<String> listAllowedIp = TextUtils.divideString(configAllowedIPs, ",");

        for (final String allowedIP : listAllowedIp) {

            if (matches(listTokensIp, allowedIP)) {
                return;
            }
        }

        throw new ServletException("Wrong ip source");
    }

    private static boolean matches(final List<String> listTokensIp, final String allowedIP) {

        final List<String> listTokensAllowedIp = TextUtils.divideString(allowedIP, ".");

        for (int i = 0; i < 4; i++) {

            final String tokenIP = listTokensIp.get(i);
            final String tokenAllowedIP = listTokensAllowedIp.get(i);

            if ("*".equals(tokenAllowedIP)) {
                continue;

            } else if (!tokenIP.equals(tokenAllowedIP)) {
                return false;
            }
        }

        return true;
    }
}
