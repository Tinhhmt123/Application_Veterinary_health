package org.example.backend_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.example.backend_app.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    public static final String AUTHENTICATED_USER = "healthypet.authenticatedUser";
    private final AccessTokenService tokenService;
    private final UserRepository userRepository;

    public BearerTokenFilter(AccessTokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith(request.getContextPath() + "/api/vaccinations")
                || path.startsWith(request.getContextPath() + "/api/ai"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) { chain.doFilter(request, response); return; }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Thiếu access token. Vui lòng đăng nhập lại.");
            return;
        }
        AccessTokenService.AuthenticatedUser user = tokenService.verify(header.substring(7).trim());
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token không hợp lệ hoặc đã hết hạn.");
            return;
        }
        var account = userRepository.findById(user.id()).orElse(null);
        if (account == null || Boolean.FALSE.equals(account.getIsActive())
                || !user.role().equalsIgnoreCase(account.getRole())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Tài khoản không còn hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }
        request.setAttribute(AUTHENTICATED_USER, user);
        chain.doFilter(request, response);
    }
}