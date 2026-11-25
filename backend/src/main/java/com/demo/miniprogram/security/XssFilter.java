package com.demo.miniprogram.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.IOException;

/**
 * XSS 跨站脚本攻击过滤器
 *
 * 防护原理：
 *   对所有请求参数进行HTML转义，将 < > " ' & 等特殊字符转为HTML实体，
 *   防止用户输入的恶意脚本被浏览器执行。
 *
 * 注意：不要添加 @Component 注解，由 SecurityConfig 中的 FilterRegistrationBean 注册
 *
 * @author demo
 */
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // 包装request，使 getParameter 等方法自动转义
        XssRequestWrapper wrappedRequest = new XssRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrappedRequest, response);
    }

    /**
     * Request包装类 - 拦截 getParameter 系列方法进行XSS转义
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        /**
         * 对输入进行HTML转义
         *
         * 保留常用标点（空格、中文标点等），只转义危险字符：
         *   < → &lt;    > → &gt;    " → &quot;    ' → &#x27;    & → &amp;
         *   ( → &#40;    ) → &#41;    # → &#35;
         */
        private static String sanitize(String input) {
            if (input == null || input.isEmpty()) {
                return input;
            }

            // 使用StringBuilder逐字符处理，比多次replace效率更高
            StringBuilder sb = new StringBuilder(input.length());
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                switch (c) {
                    case '<'  -> sb.append("&lt;");
                    case '>'  -> sb.append("&gt;");
                    case '"'  -> sb.append("&quot;");
                    case '\'' -> sb.append("&#x27;");
                    case '&'  -> sb.append("&amp;");
                    case '('  -> sb.append("&#40;");
                    case ')'  -> sb.append("&#41;");
                    case '#'  -> sb.append("&#35;");
                    default   -> sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
