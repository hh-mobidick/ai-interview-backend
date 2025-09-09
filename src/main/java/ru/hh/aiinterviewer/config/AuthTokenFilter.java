package ru.hh.aiinterviewer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collection;
import java.util.List;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

  private final String expectedToken;

  public AuthTokenFilter(AuthProperties authProperties) {
    this.expectedToken = authProperties.getToken();
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(7);
    if (StringUtils.hasText(expectedToken) && expectedToken.equals(token)) {
      Authentication auth = new StaticTokenAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }

  private static class StaticTokenAuthentication extends AbstractAuthenticationToken {
    private final String token;

    private StaticTokenAuthentication(String token) {
      super(authorities());
      this.token = token;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return token;
    }

    @Override
    public Object getPrincipal() {
      return "api-user";
    }

    private static Collection<? extends GrantedAuthority> authorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
  }
}


