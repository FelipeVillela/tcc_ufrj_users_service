package br.edu.ufrj.tcc.auth;

import br.edu.ufrj.tcc.auth.dto.LoginRequest;
import br.edu.ufrj.tcc.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Entrada do usuário no app. Devolve os dados do usuário autenticado — é com o
 * {@code id} desta resposta que o front chama os demais endpoints
 * ({@code /users/{id}/saldo}, {@code /users/{id}/contacts}, ...), o que permite
 * demonstrar o sistema alternando entre usuários diferentes.
 */
@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {

    private final LoginService service;

    public LoginResource(LoginService service) {
        this.service = service;
    }

    @POST
    public UserResponse entrar(@Valid LoginRequest req) {
        return UserResponse.from(service.autenticar(req));
    }
}
