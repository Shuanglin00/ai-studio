package com.shuanglin.bot.config;

import jakarta.servlet.*;

import java.io.IOException;

public class RequestFilter implements Filter {
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		servletRequest.getAttribute("userId");
	}
}
