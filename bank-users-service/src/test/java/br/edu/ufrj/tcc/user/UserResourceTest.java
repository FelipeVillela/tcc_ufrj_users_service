package br.edu.ufrj.tcc.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.dto.AddPixKeyRequest;
import br.edu.ufrj.tcc.user.dto.CreateUserRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Testes do {@link UserResource} sobre HTTP (REST Assured), com o
 * {@link UserService} mockado: valida rotas, códigos de status, validação de
 * payload e o mapeamento das exceções de negócio para JSON.
 */
@QuarkusTest
@DisplayName("UserResource — contrato HTTP de /users")
class UserResourceTest {

    @InjectMock
    UserService service;

    private static User usuario(long id, String nome, String email, BigDecimal saldo) {
        User user = new User(nome, email, "senha123", saldo);
        user.id = id;
        return user;
    }

    private static PixKey chave(long id, PixKeyType tipo, String valor, User dono) {
        PixKey chave = new PixKey(tipo, valor, dono);
        chave.id = id;
        return chave;
    }

    // ------------------------------------------------------------------
    // GET /users
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /users devolve 200 e a lista de usuários com suas chaves Pix")
    void listar() {
        User felipe = usuario(1L, "Felipe Augusto", "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        felipe.chavesPix.add(chave(10L, PixKeyType.EMAIL, "felipe@poli.ufrj.br", felipe));
        User maria = usuario(2L, "Maria Silva", "maria@ufrj.br", new BigDecimal("10.50"));
        Mockito.when(service.listarTodos()).thenReturn(List.of(felipe, maria));

        given()
                .when().get("/users")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(1))
                .body("[0].nome", is("Felipe Augusto"))
                .body("[0].email", is("felipe@poli.ufrj.br"))
                .body("[0].saldo", is(2500.00f))
                .body("[0].criadoEm", notNullValue())
                .body("[0].chavesPix.size()", is(1))
                .body("[0].chavesPix[0].tipo", is("EMAIL"))
                .body("[0].chavesPix[0].valor", is("felipe@poli.ufrj.br"))
                .body("[1].nome", is("Maria Silva"))
                .body("[1].chavesPix.size()", is(0));
    }

    @Test
    @DisplayName("GET /users devolve 200 e lista vazia quando não há usuários")
    void listarVazio() {
        Mockito.when(service.listarTodos()).thenReturn(List.of());

        given()
                .when().get("/users")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @DisplayName("A senha nunca é exposta na resposta")
    void senhaNaoEhExposta() {
        Mockito.when(service.buscarPorId(1L))
                .thenReturn(usuario(1L, "Felipe Augusto", "felipe@poli.ufrj.br", new BigDecimal("2500.00")));

        given()
                .when().get("/users/1")
                .then()
                .statusCode(200)
                .body("senha", nullValue());
    }

    // ------------------------------------------------------------------
    // GET /users/{id} e /users/{id}/saldo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /users/{id} devolve 200 e o usuário")
    void buscar() {
        Mockito.when(service.buscarPorId(1L))
                .thenReturn(usuario(1L, "Felipe Augusto", "felipe@poli.ufrj.br", new BigDecimal("2500.00")));

        given()
                .when().get("/users/1")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("nome", is("Felipe Augusto"))
                .body("saldo", is(2500.00f));
    }

    @Test
    @DisplayName("GET /users/{id} devolve 404 com o erro em JSON quando o usuário não existe")
    void buscarInexistente() {
        Mockito.when(service.buscarPorId(99L)).thenThrow(BusinessException.notFound("Usuário 99 não encontrado."));

        given()
                .when().get("/users/99")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("erro", is("Usuário 99 não encontrado."));
    }

    @Test
    @DisplayName("GET /users/{id}/saldo devolve 200 e o saldo")
    void saldo() {
        Mockito.when(service.saldoDe(1L)).thenReturn(new BigDecimal("2500.00"));

        given()
                .when().get("/users/1/saldo")
                .then()
                .statusCode(200)
                .body("saldo", is(2500.00f));
    }

    @Test
    @DisplayName("GET /users/{id}/saldo devolve 404 quando o usuário não existe")
    void saldoDeUsuarioInexistente() {
        Mockito.when(service.saldoDe(99L)).thenThrow(BusinessException.notFound("Usuário 99 não encontrado."));

        given()
                .when().get("/users/99/saldo")
                .then()
                .statusCode(404)
                .body("erro", is("Usuário 99 não encontrado."));
    }

    // ------------------------------------------------------------------
    // POST /users
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /users devolve 201 e repassa o payload ao service")
    void criar() {
        Mockito.when(service.criar(Mockito.any()))
                .thenReturn(usuario(3L, "Maria Silva", "maria@ufrj.br", new BigDecimal("1500.75")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nome": "Maria Silva",
                          "email": "maria@ufrj.br",
                          "senha": "senha123",
                          "saldoInicial": 1500.75
                        }
                        """)
                .when().post("/users")
                .then()
                .statusCode(201)
                .body("id", is(3))
                .body("nome", is("Maria Silva"))
                .body("saldo", is(1500.75f));

        ArgumentCaptor<CreateUserRequest> enviado = ArgumentCaptor.forClass(CreateUserRequest.class);
        Mockito.verify(service).criar(enviado.capture());
        assertEquals("Maria Silva", enviado.getValue().nome());
        assertEquals("maria@ufrj.br", enviado.getValue().email());
        assertEquals("senha123", enviado.getValue().senha());
        assertEquals(new BigDecimal("1500.75"), enviado.getValue().saldoInicial());
    }

    @Test
    @DisplayName("POST /users aceita saldoInicial ausente (o service decide o padrão)")
    void criarSemSaldoInicial() {
        Mockito.when(service.criar(Mockito.any()))
                .thenReturn(usuario(3L, "Maria Silva", "maria@ufrj.br", BigDecimal.ZERO));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Maria Silva", "email": "maria@ufrj.br", "senha": "senha123"}
                        """)
                .when().post("/users")
                .then()
                .statusCode(201)
                .body("saldo", is(0));

        ArgumentCaptor<CreateUserRequest> enviado = ArgumentCaptor.forClass(CreateUserRequest.class);
        Mockito.verify(service).criar(enviado.capture());
        assertNull(enviado.getValue().saldoInicial());
    }

    @Test
    @DisplayName("POST /users devolve 409 quando o e-mail já existe")
    void criarComEmailDuplicado() {
        Mockito.when(service.criar(Mockito.any()))
                .thenThrow(BusinessException.conflict("Já existe um usuário com o e-mail felipe@poli.ufrj.br."));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Outro", "email": "felipe@poli.ufrj.br", "senha": "senha123"}
                        """)
                .when().post("/users")
                .then()
                .statusCode(409)
                .body("erro", is("Já existe um usuário com o e-mail felipe@poli.ufrj.br."));
    }

    @Test
    @DisplayName("POST /users devolve 400 e não chama o service quando o payload é inválido")
    void criarComPayloadInvalido() {
        // nome em branco
        given().contentType(ContentType.JSON)
                .body("""
                        {"nome": "  ", "email": "maria@ufrj.br", "senha": "senha123"}
                        """)
                .when().post("/users").then().statusCode(400);

        // e-mail fora do formato
        given().contentType(ContentType.JSON)
                .body("""
                        {"nome": "Maria", "email": "nao-e-email", "senha": "senha123"}
                        """)
                .when().post("/users").then().statusCode(400);

        // senha ausente
        given().contentType(ContentType.JSON)
                .body("""
                        {"nome": "Maria", "email": "maria@ufrj.br"}
                        """)
                .when().post("/users").then().statusCode(400);

        // saldo inicial negativo
        given().contentType(ContentType.JSON)
                .body("""
                        {"nome": "Maria", "email": "maria@ufrj.br", "senha": "senha123", "saldoInicial": -1}
                        """)
                .when().post("/users").then().statusCode(400);

        Mockito.verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // Chaves Pix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /users/{id}/pix-keys devolve 200 e as chaves do usuário")
    void listarChaves() {
        User felipe = usuario(1L, "Felipe Augusto", "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        Mockito.when(service.chavesDe(1L)).thenReturn(List.of(
                chave(10L, PixKeyType.EMAIL, "felipe@poli.ufrj.br", felipe),
                chave(11L, PixKeyType.CPF, "09876543210", felipe)));

        given()
                .when().get("/users/1/pix-keys")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("id", hasItems(10, 11))
                .body("tipo", hasItems("EMAIL", "CPF"))
                .body("[0].valor", is("felipe@poli.ufrj.br"))
                .body("[0].criadoEm", notNullValue());
    }

    @Test
    @DisplayName("GET /users/{id}/pix-keys devolve 404 quando o usuário não existe")
    void listarChavesDeUsuarioInexistente() {
        Mockito.when(service.chavesDe(99L)).thenThrow(BusinessException.notFound("Usuário 99 não encontrado."));

        given().when().get("/users/99/pix-keys").then().statusCode(404).body("erro", notNullValue());
    }

    @Test
    @DisplayName("POST /users/{id}/pix-keys devolve 201 e repassa tipo e valor ao service")
    void adicionarChave() {
        User felipe = usuario(1L, "Felipe Augusto", "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        Mockito.when(service.adicionarChavePix(Mockito.eq(1L), Mockito.any()))
                .thenReturn(chave(12L, PixKeyType.CPF, "09876543210", felipe));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"tipo": "CPF", "valor": "09876543210"}
                        """)
                .when().post("/users/1/pix-keys")
                .then()
                .statusCode(201)
                .body("id", is(12))
                .body("tipo", is("CPF"))
                .body("valor", is("09876543210"));

        ArgumentCaptor<AddPixKeyRequest> enviado = ArgumentCaptor.forClass(AddPixKeyRequest.class);
        Mockito.verify(service).adicionarChavePix(Mockito.eq(1L), enviado.capture());
        assertEquals(PixKeyType.CPF, enviado.getValue().tipo());
        assertEquals("09876543210", enviado.getValue().valor());
    }

    @Test
    @DisplayName("POST /users/{id}/pix-keys devolve 409 quando a chave já está cadastrada")
    void adicionarChaveDuplicada() {
        Mockito.when(service.adicionarChavePix(Mockito.eq(1L), Mockito.any()))
                .thenThrow(BusinessException.conflict("A chave pix 'ja@existe.br' já está cadastrada."));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"tipo": "EMAIL", "valor": "ja@existe.br"}
                        """)
                .when().post("/users/1/pix-keys")
                .then()
                .statusCode(409)
                .body("erro", is("A chave pix 'ja@existe.br' já está cadastrada."));
    }

    @Test
    @DisplayName("POST /users/{id}/pix-keys devolve 400 quando tipo ou valor faltam")
    void adicionarChaveComPayloadInvalido() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"valor": "09876543210"}
                        """)
                .when().post("/users/1/pix-keys").then().statusCode(400);

        given().contentType(ContentType.JSON)
                .body("""
                        {"tipo": "CPF", "valor": ""}
                        """)
                .when().post("/users/1/pix-keys").then().statusCode(400);

        Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("DELETE /users/{id}/pix-keys/{chaveId} devolve 204")
    void removerChave() {
        given()
                .when().delete("/users/1/pix-keys/10")
                .then()
                .statusCode(204);

        Mockito.verify(service).removerChavePix(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /users/{id}/pix-keys/{chaveId} devolve 404 quando a chave não é do usuário")
    void removerChaveInexistente() {
        Mockito.doThrow(BusinessException.notFound("Chave pix 10 não encontrada para o usuário 2."))
                .when(service).removerChavePix(2L, 10L);

        given()
                .when().delete("/users/2/pix-keys/10")
                .then()
                .statusCode(404)
                .body("erro", is("Chave pix 10 não encontrada para o usuário 2."));
    }
}
