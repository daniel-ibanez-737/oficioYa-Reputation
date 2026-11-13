package edu.eci.dosw.oficioya.reputation.dto;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import edu.eci.dosw.oficioya.reputation.domain.TipoParte;
import edu.eci.dosw.oficioya.reputation.exception.OperacionNoPermitidaException;

/**
 * Usuario que hace la petición, tomado del JWT.
 * SUPUESTO (pendiente con Identity, Squad 1): el claim {@code sub} es el id numérico del usuario.
 */
public record UsuarioAutenticado(Integer id, Set<TipoParte> roles, boolean administrador) {

    public static final String ROL_TRABAJADOR = "ROLE_TRABAJADOR";
    public static final String ROL_CONTRATANTE = "ROLE_CONTRATANTE";
    public static final String ROL_ADMINISTRADOR = "ROLE_ADMINISTRADOR";

    public static UsuarioAutenticado desde(JwtAuthenticationToken token) {
        Integer id;
        try {
            id = Integer.valueOf(token.getToken().getSubject());
        } catch (NumberFormatException e) {
            throw new OperacionNoPermitidaException("El token no identifica a un usuario válido");
        }
        Set<TipoParte> roles = EnumSet.noneOf(TipoParte.class);
        boolean administrador = false;
        for (GrantedAuthority autoridad : token.getAuthorities()) {
            String rol = autoridad.getAuthority();
            if (ROL_TRABAJADOR.equals(rol)) {
                roles.add(TipoParte.TRABAJADOR);
            } else if (ROL_CONTRATANTE.equals(rol)) {
                roles.add(TipoParte.CONTRATANTE);
            } else if (ROL_ADMINISTRADOR.equals(rol)) {
                administrador = true;
            }
        }
        return new UsuarioAutenticado(id, roles, administrador);
    }
}
