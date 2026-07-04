package com.ecommerce.common.utils;

import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.ecommerce.common.constants.Constant.REQUEST_ID_HEADER;

public class RequestIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String requestId = ((HttpServletRequest) request).getHeader(REQUEST_ID_HEADER);
            if (requestId == null) {
                requestId = cn.hutool.core.lang.UUID.randomUUID().toString(true);
            }
            MDC.put(REQUEST_ID_HEADER, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
