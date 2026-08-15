package br.edu.ufrj.tcc.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.auth.dto.LoginRequest;
import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.User;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Testes unitários do {@link LoginService}: a busca do usuário é mockada com
 * {@link PanacheMock}, então só a regra de autenticação é exercitada.
 */
@QuarkusTest
@DisplayName("LoginService — autenticação por e-mail e senha")
class LoginServiceTest {

    private static final String EMAIL = "felipe@poli.ufrj.br";
    private static final String SENHA = "senha123";

    @Inject
    LoginService service;

    private static User felipe() {
        User user = new User("Felipe Augusto", EMAIL, SENHA, new BigDecimal("2500.00"));
        user.id = 1L;
        return user;
    }

    @Test
    @DisplayName("login com sucesso devolve o usuário autenticado")
    void loginComSucesso() {
        PanacheMock.mock(User.class);
        User felipe = felipe();
        Mockito.when(User.findByEmail(EMAIL)).thenReturn(felipe);

        User autenticado = service.autenticar(new LoginRequest(EMAIL, SENHA));

        assertSame(felipe, autenticado);
    }

    @Test
    @DisplayName("login com senha errada devolve 401")
    void loginComSenhaErrada() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail(EMAIL)).thenReturn(felipe());

        BusinessException erro = assertThrows(BusinessException.class,
                () -> service.autenticar(new LoginRequest(EMAIL, "senha-errada")));

        assertEquals(Response.Status.UNAUTHORIZED, erro.getStatus());
        assertEquals(LoginService.CREDENCIAIS_INVALIDAS, erro.getMessage());
    }

    @Test
    @DisplayName("login com e-mail não cadastrado devolve 401 com a MESMA mensagem da senha errada")
    void loginComEmailInexistente() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail("ninguem@ufrj.br")).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class,
                () -> service.autenticar(new LoginRequest("ninguem@ufrj.br", SENHA)));

        assertEquals(Response.Status.UNAUTHORIZED, erro.getStatus());
        // Mensagem genérica: a resposta não revela se o e-mail existe no banco.
        assertEquals(LoginService.CREDENCIAIS_INVALIDAS, erro.getMessage());
    }

    @Test
    @DisplayName("a senha confere caractere a caractere (não aceita prefixo nem outro caso)")
    void loginNaoAceitaSenhaParecida() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail(EMAIL)).thenReturn(felipe());

        assertThrows(BusinessException.class, () -> service.autenticar(new LoginRequest(EMAIL, "senha")));
        assertThrows(BusinessException.class, () -> service.autenticar(new LoginRequest(EMAIL, "SENHA123")));
        assertThrows(BusinessException.class, () -> service.autenticar(new LoginRequest(EMAIL, "senha1234")));
    }

    @Test
    @DisplayName("senha nula devolve 401 em vez de quebrar")
    void loginComSenhaNula() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail(EMAIL)).thenReturn(felipe());

        BusinessException erro = assertThrows(BusinessException.class,
                () -> service.autenticar(new LoginRequest(EMAIL, null)));

        assertEquals(Response.Status.UNAUTHORIZED, erro.getStatus());
    }

    @Test
    @DisplayName("o e-mail é procurado exatamente como veio na requisição")
    void loginProcuraPeloEmailInformado() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail(EMAIL)).thenReturn(felipe());

        service.autenticar(new LoginRequest(EMAIL, SENHA));

        PanacheMock.verify(User.class).findByEmail(EMAIL);
    }
}
