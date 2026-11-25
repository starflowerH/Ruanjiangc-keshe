package com.demo.miniprogram.config;

import com.demo.miniprogram.common.UploadConstants;
import com.demo.miniprogram.security.RateLimitInterceptor;
import com.demo.miniprogram.security.XssFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 全局安全配置 - 综合防护体系
 *
 * 安全防护层次（纵深防御）：
 *   第1层：CORS             → 阻止非法来源的跨域请求
 *   第2层：XSS Filter       → 过滤所有请求参数中的恶意脚本
 *   第3层：Rate Limiter     → 限制请求频率，防暴力破解和DDoS
 *   第4层：JWT Auth Filter  → 无状态身份认证
 *   第5层：Security Headers → 浏览器端安全策略（CSP/HSTS等）
 *   第6层：BCrypt           → 密码哈希存储，不可逆
 *
 * @author demo
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitInterceptor rateLimitInterceptor;

    /* ==================== 第1层：CORS 跨域配置 ==================== */

    /**
     * CORS 跨域配置 - 只允许信任的来源访问
     *
     * 生产环境必须：
     *   1. 将 allowedOrigins 改为你的小程序/公众号H5的实际域名
     *   2. 不要使用 allowedOriginPatterns("*") 或 allowCredentials(true) + "*"
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源（开发环境允许所有，生产环境改为实际域名）
        config.setAllowedOriginPatterns(List.of("*"));

        // 允许的HTTP方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // 允许前端携带凭证（Cookie等），开启后不能用 "*" 作为来源
        config.setAllowCredentials(true);

        // 预检请求缓存时间（减少OPTIONS请求开销）
        config.setMaxAge(3600L);

        // 暴露的自定义响应头
        config.addExposedHeader("X-Request-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /* ==================== 第2-6层：Spring Security 配置 ==================== */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ---- CORS ----
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ---- CSRF：JWT无状态，不需要CSRF保护 ----
            .csrf(csrf -> csrf.disable())

            // ---- Session：无状态 ----
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ---- 安全响应头（第5层） ----
            .headers(headers -> headers
                // XSS保护：浏览器检测到反射型XSS时阻止页面渲染
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // 禁止浏览器将响应嵌入frame/iframe（防点击劫持）
                .frameOptions(frame -> frame.deny())
                // 禁止MIME类型嗅探
                .contentTypeOptions(contentType -> {})
                // HSTS：强制HTTPS（生产环境开启，有效期1年）
                // .httpStrictTransportSecurity(hsts -> hsts
                //     .maxAgeInSeconds(31536000).includeSubDomains(true))
                // CSP：内容安全策略（根据实际前端资源调整）
                // .contentSecurityPolicy(csp -> csp.policyDirectives(
                //     "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"))
            )

            // ---- 请求授权 ----
            .authorizeHttpRequests(auth -> auth
                // 所有请求先放行（具体鉴权由JwtAuthFilter处理）
                .anyRequest().permitAll()
            )

            // ---- JWT过滤器（第4层） ----
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /* ==================== 注册限流拦截器（第3层） ==================== */

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器，拦截所有 /api/** 请求
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(Ordered.HIGHEST_PRECEDENCE); // 最先执行
    }

    /* ==================== 静态资源映射：用户上传图片 ==================== */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传图片保存在固定目录（UploadConstants.UPLOAD_DIR，默认 <backend>/uploads），
        // 生产 jar 内 static 目录不可写，故映射到外部目录；与 Spring Boot 默认 /static 处理共存
        registry.addResourceHandler("/uploads/**")
                // 注意：目录 location 必须以 "/" 结尾，Spring 才能将请求文件名正确拼接到目录下；
                // 否则 createRelative 拼接出错，静态资源找不到，请求会落到全局 404 处理器
                .addResourceLocations(new FileSystemResource(UploadConstants.UPLOAD_DIR.toString() + "/"));
    }

    /* ==================== 注册XSS过滤器（第2层） ==================== */

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // 第二个执行
        registration.setName("xssFilter");
        return registration;
    }

    /* ==================== 密码编码器 ==================== */

    /**
     * BCrypt 密码哈希
     * strength=12 → 2^12 轮哈希迭代，每次约0.3秒
     * 这个延迟对正常登录无感，但能极大增加暴力破解成本
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
