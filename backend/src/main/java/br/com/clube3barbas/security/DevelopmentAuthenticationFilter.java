package br.com.clube3barbas.security;

import br.com.clube3barbas.domain.PerfilUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class DevelopmentAuthenticationFilter extends OncePerRequestFilter {

    public static final String ROLE_HEADER = "X-Dev-Role";
    public static final String USER_HEADER = "X-Dev-User";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var roleClaim = request.getHeader(ROLE_HEADER);
        if (roleClaim != null && !roleClaim.isBlank()) {
            var perfil = PerfilUsuario.fromClaim(roleClaim);
            var uid = request.getHeader(USER_HEADER);
            if (uid == null || uid.isBlank()) {
                uid = "desenvolvedor-local";
            }
            var usuario = new UsuarioAutenticado(uid, "Desenvolvedor local", "local@3barbas.dev", perfil);
            var authority = new SimpleGrantedAuthority("ROLE_" + perfil.name());
            var authentication = new UsernamePasswordAuthenticationToken(
                    usuario,
                    null,
                    java.util.List.of(authority)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}

