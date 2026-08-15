package br.edu.ufrj.tcc.contact;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.contact.dto.SaveContactRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;

/**
 * Testes do {@link ContactResource} sobre HTTP (REST Assured), com o
 * {@link ContactService} mockado: valida rotas, query params, códigos de
 * status e a serialização do {@code _id} do Mongo como hexadecimal.
 */
@QuarkusTest
@DisplayName("ContactResource — contrato HTTP de /users/{userId}/contacts")
class ContactResourceTest {

    private static final String ID_CARLA = "507f1f77bcf86cd799439011";
    private static final String ID_JOAO = "507f1f77bcf86cd799439012";

    @InjectMock
    ContactService service;

    private static Contact contato(String id, String nome, String chavePix, String banco, List<String> alternativos) {
        Contact c = new Contact();
        c.id = new ObjectId(id);
        c.ownerUserId = 1L;
        c.nome = nome;
        c.chavePix = chavePix;
        c.banco = banco;
        c.nomesAlternativos = new ArrayList<>(alternativos);
        return c;
    }

    // ------------------------------------------------------------------
    // GET /users/{userId}/contacts
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /contacts devolve 200 e os contatos recentes do usuário")
    void listar() {
        Mockito.when(service.listarRecentes(1L)).thenReturn(List.of(
                contato(ID_CARLA, "Carla Pereira", "carla.pereira@gmail.com", "Banco do Brasil", List.of("mãe")),
                contato(ID_JOAO, "João Silva", "11122233344", "Nubank", List.of("irmão"))));

        given()
                .when().get("/users/1/contacts")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(ID_CARLA))
                .body("[0].nome", is("Carla Pereira"))
                .body("[0].chavePix", is("carla.pereira@gmail.com"))
                .body("[0].banco", is("Banco do Brasil"))
                .body("[0].nomesAlternativos", contains("mãe"))
                .body("[0].criadoEm", notNullValue())
                .body("[0].usadoPorUltimoEm", notNullValue())
                .body("[1].id", is(ID_JOAO));
    }

    @Test
    @DisplayName("GET /contacts devolve 200 e lista vazia quando o usuário não tem contatos")
    void listarVazio() {
        Mockito.when(service.listarRecentes(42L)).thenReturn(List.of());

        given().when().get("/users/42/contacts").then().statusCode(200).body("size()", is(0));
    }

    // ------------------------------------------------------------------
    // GET /users/{userId}/contacts/search
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /contacts/search repassa termo e campo de busca ao service")
    void buscar() {
        Mockito.when(service.buscar(1L, "mãe", "nomesAlternativos")).thenReturn(List.of(
                contato(ID_CARLA, "Carla Pereira", "carla.pereira@gmail.com", "Banco do Brasil", List.of("mãe"))));

        given()
                .queryParam("termo", "mãe")
                .queryParam("por", "nomesAlternativos")
                .when().get("/users/1/contacts/search")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].nome", is("Carla Pereira"));

        Mockito.verify(service).buscar(1L, "mãe", "nomesAlternativos");
    }

    @Test
    @DisplayName("GET /contacts/search sem query params repassa nulos (o service aplica o padrão)")
    void buscarSemParametros() {
        Mockito.when(service.buscar(1L, null, null)).thenReturn(List.of());

        given().when().get("/users/1/contacts/search").then().statusCode(200).body("size()", is(0));

        Mockito.verify(service).buscar(1L, null, null);
    }

    @Test
    @DisplayName("GET /contacts/search devolve 400 quando o campo de busca é inválido")
    void buscarComCampoInvalido() {
        Mockito.when(service.buscar(1L, "carla", "banco")).thenThrow(new BusinessException(
                Response.Status.BAD_REQUEST, "Parâmetro de busca inválido. Use 'nome' ou 'nomeAlternativo'."));

        given()
                .queryParam("termo", "carla")
                .queryParam("por", "banco")
                .when().get("/users/1/contacts/search")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("erro", is("Parâmetro de busca inválido. Use 'nome' ou 'nomeAlternativo'."));
    }

    // ------------------------------------------------------------------
    // POST /users/{userId}/contacts
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /contacts devolve 201 e repassa o destinatário ao service")
    void salvar() {
        Mockito.when(service.salvarAoEnviar(Mockito.eq(1L), Mockito.any())).thenReturn(
                contato(ID_JOAO, "João Silva", "11122233344", "Nubank", List.of("irmão")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nome": "João Silva",
                          "chavePix": "11122233344",
                          "banco": "Nubank",
                          "nomesAlternativos": ["irmão"]
                        }
                        """)
                .when().post("/users/1/contacts")
                .then()
                .statusCode(201)
                .body("id", is(ID_JOAO))
                .body("nome", is("João Silva"))
                .body("chavePix", is("11122233344"))
                .body("nomesAlternativos", contains("irmão"));

        ArgumentCaptor<SaveContactRequest> enviado = ArgumentCaptor.forClass(SaveContactRequest.class);
        Mockito.verify(service).salvarAoEnviar(Mockito.eq(1L), enviado.capture());
        assertEquals("João Silva", enviado.getValue().nome());
        assertEquals("11122233344", enviado.getValue().chavePix());
        assertEquals("Nubank", enviado.getValue().banco());
        assertEquals(List.of("irmão"), enviado.getValue().nomesAlternativos());
    }

    @Test
    @DisplayName("POST /contacts aceita banco e nomesAlternativos ausentes (campos opcionais)")
    void salvarSomenteComObrigatorios() {
        Mockito.when(service.salvarAoEnviar(Mockito.eq(1L), Mockito.any())).thenReturn(
                contato(ID_JOAO, "João Silva", "11122233344", null, List.of()));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "João Silva", "chavePix": "11122233344"}
                        """)
                .when().post("/users/1/contacts")
                .then()
                .statusCode(201)
                .body("banco", org.hamcrest.Matchers.nullValue())
                .body("nomesAlternativos", is(List.of()));

        ArgumentCaptor<SaveContactRequest> enviado = ArgumentCaptor.forClass(SaveContactRequest.class);
        Mockito.verify(service).salvarAoEnviar(Mockito.eq(1L), enviado.capture());
        assertNull(enviado.getValue().banco());
        assertNull(enviado.getValue().nomesAlternativos());
    }

    @Test
    @DisplayName("POST /contacts devolve 400 e não chama o service quando nome ou chavePix faltam")
    void salvarComPayloadInvalido() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"chavePix": "11122233344"}
                        """)
                .when().post("/users/1/contacts").then().statusCode(400);

        given().contentType(ContentType.JSON)
                .body("""
                        {"nome": "João Silva", "chavePix": "   "}
                        """)
                .when().post("/users/1/contacts").then().statusCode(400);

        Mockito.verifyNoInteractions(service);
    }

    // ------------------------------------------------------------------
    // DELETE /users/{userId}/contacts/{contactId}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /contacts/{contactId} devolve 204")
    void remover() {
        given()
                .when().delete("/users/1/contacts/" + ID_CARLA)
                .then()
                .statusCode(204);

        Mockito.verify(service).remover(1L, ID_CARLA);
    }

    @Test
    @DisplayName("DELETE /contacts/{contactId} devolve 404 quando o contato não é do usuário")
    void removerInexistente() {
        Mockito.doThrow(BusinessException.notFound("Contato " + ID_CARLA + " não encontrado para o usuário 2."))
                .when(service).remover(2L, ID_CARLA);

        given()
                .when().delete("/users/2/contacts/" + ID_CARLA)
                .then()
                .statusCode(404)
                .body("erro", is("Contato " + ID_CARLA + " não encontrado para o usuário 2."));
    }
}
