package com.example.portal_oportunidades_back.security;
import com.example.portal_oportunidades_back.auth.repository.*;
import com.example.portal_oportunidades_back.profile.repository.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder decoder; private final UserRepository users; private final StudentRepository students;
    private final RecruiterRepository recruiters; private final AdministratorRepository administrators;
    public JwtAuthenticationFilter(JwtDecoder decoder, UserRepository users, StudentRepository students,
            RecruiterRepository recruiters, AdministratorRepository administrators) {
        this.decoder = decoder; this.users = users; this.students = students;
        this.recruiters = recruiters; this.administrators = administrators;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/csrf") || path.equals("/api/auth/login")
                || path.equals("/api/auth/logout") || path.startsWith("/api/auth/register/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    if (token != null) { response.sendError(401); return; }
                    token = cookie.getValue();
                }
            }
        }
        if (token != null) {
            try {
                Jwt jwt = decoder.decode(token);
                Long id = Long.valueOf(jwt.getSubject());
                var user = users.findById(id).filter(u -> u.isActive()).orElse(null);
                if (user == null) { response.sendError(401); return; }
                var roles = new ArrayList<SimpleGrantedAuthority>();
                if (students.existsById(id)) roles.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
                if (recruiters.existsById(id)) roles.add(new SimpleGrantedAuthority("ROLE_RECRUITER"));
                if (administrators.existsById(id)) roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id.toString(), null, roles));
            } catch (JwtException | IllegalArgumentException ex) { response.sendError(401); return; }
        }
        chain.doFilter(request, response);
    }
}
