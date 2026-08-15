package br.edu.ufrj.tcc.auth;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.auth.dto.LoginRequest;
import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.PixKey;
import br.edu.ufrj.tcc.user.PixKeyType;
import br.edu.ufrj.tcc.user.User;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Testes do {@link LoginResource} sobre HTTP (REST Assured), com o
 * {@link LoginService} mockado: valida o corpo devolvido no login bem-sucedido,
 * o 401 do login sem sucesso e a validação do payload.
 */
@QuarkusTest
@DisplayName("LoginResource — contrato HTTP de /login")
class LoginResourceTest {

    @InjectMock
    LoginService service;

    private static User newUser() {
        User user = new User("Usuario Teste", "teste@email.com", "senha123", new BigDecimal("2500.00"));
        user.id = 1L;
        PixKey chave = new PixKey(PixKeyType.EMAIL, "teste@email.com", user);
        chave.id = 10L;
        user.chavesPix.add(chave);
        return user;
    }

    @Test
    @DisplayName("POST /login devolve 200 com o usuário autenticado e repassa as credenciais ao service")
    void loginComSucesso() {
        Mockito.when(service.autenticar(Mockito.any())).thenReturn(newUser());

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "teste@email.com", "senha": "senha123"}
                        """)
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("nome", is("Usuario Teste"))
                .body("email", is("teste@email.com"))
                .body("saldo", is(2500.00f))
                .body("chavesPix.size()", is(1))
                .body("chavesPix[0].valor", is("teste@email.com"));

        ArgumentCaptor<LoginRequest> enviado = ArgumentCaptor.forClass(LoginRequest.class);
        Mockito.verify(service).autenticar(enviado.capture());
        assertEquals("teste@email.com", enviado.getValue().email());
        assertEquals("senha123", enviado.getValue().senha());
    }

    @Test
    @DisplayName("POST /login não devolve a senha do usuário")
    void loginNaoExpoeSenha() {
        Mockito.when(service.autenticar(Mockito.any())).thenReturn(newUser());

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "teste@email.com", "senha": "senha123"}
                        """)
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("senha", nullValue());
    }

    @Test
    @DisplayName("POST /login devolve 401 com o erro em JSON quando as credenciais não batem")
    void loginSemSucesso() {
        Mockito.when(service.autenticar(Mockito.any()))
                .thenThrow(BusinessException.unauthorized("E-mail ou senha inválidos."));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "felipe@poli.ufrj.br", "senha": "senha-errada"}
                        """)
                .when().post("/login")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("erro", is("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("POST /login devolve 401 (não 404) para e-mail não cadastrado")
    void loginComEmailInexistente() {
        Mockito.when(service.autenticar(Mockito.any()))
                .thenThrow(BusinessException.unauthorized("E-mail ou senha inválidos."));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "ninguem@ufrj.br", "senha": "senha123"}
                        """)
                .when().post("/login")
                .then()
                .statusCode(401)
                .body("erro", is("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("POST /login devolve 400 e não chama o service quando falta e-mail ou senha")
    void loginComPayloadInvalido() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"senha": "senha123"}
                        """)
                .when().post("/login").then().statusCode(400);

        given().contentType(ContentType.JSON)
                .body("""
                        {"email": "felipe@poli.ufrj.br"}
                        """)
                .when().post("/login").then().statusCode(400);

        given().contentType(ContentType.JSON)
                .body("""
                        {"email": "   ", "senha": "   "}
                        """)
                .when().post("/login").then().statusCode(400);

        Mockito.verifyNoInteractions(service);
    }
}
