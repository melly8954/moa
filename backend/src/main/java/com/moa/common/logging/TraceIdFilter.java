package com.moa.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 traceId를 정해 MDC, 응답 헤더, 오류 응답에 같은 값으로 쓴다.
 * 앞단(게이트웨이 등)이 X-Trace-Id를 주면 그 값을 이어 쓴다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Trace-Id";
	public static final String MDC_KEY = "traceId";

	private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

	public static String currentTraceId() {
		return MDC.get(MDC_KEY);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String traceId = incoming != null && VALID.matcher(incoming).matches() ? incoming : newTraceId();
		MDC.put(MDC_KEY, traceId);
		response.setHeader(HEADER, traceId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	private String newTraceId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}
}
