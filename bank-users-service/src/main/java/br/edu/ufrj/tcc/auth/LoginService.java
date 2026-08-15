package br.edu.ufrj.tcc.auth;

import java.util.Objects;

import br.edu.ufrj.tcc.auth.dto.LoginRequest;
import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.User;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Login simples sem segurança apenas para testar a funcionalidade
 * Do chat lidar com diferentes usuários.
 */
@ApplicationScoped
public class LoginService {

    static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";

    public User autenticar(LoginRequest req) {
        User user = User.findByEmail(req.email());
        if (user == null || !Objects.equals(user.senha, req.senha())) {
            throw BusinessException.unauthorized(CREDENCIAIS_INVALIDAS);
        }
        return user;
    }
}
