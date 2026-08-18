package br.edu.ufrj.tcc.pixdirectory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Testes do {@link PixDirectoryResource} sobre HTTP (REST Assured): contrato de
 * GET /pix-directory/search, incluindo o 404 para chave desconhecida.
 */
@QuarkusTest
@DisplayName("PixDirectoryResource — contrato HTTP de /pix-directory/search")
class PixDirectoryResourceTest {

    @Test
    @DisplayName("GET /search devolve 200 com nome e banco de uma chave conhecida")
    void lookupEncontrado() {
        given()
                .queryParam("chave", "padaria@pix.com")
                .when().get("/pix-directory/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("chavePix", is("padaria@pix.com"))
                .body("nome", is("Padaria Pão Quente"))
                .body("banco", is("Banco Amarelo"));
    }

    @Test
    @DisplayName("GET /search devolve 404 quando a chave não está no diretório")
    void lookupNaoEncontrado() {
        given()
                .queryParam("chave", "ninguem@pix.com")
                .when().get("/pix-directory/search")
                .then()
                .statusCode(404)
                .body("erro", is("Nenhum titular encontrado para a chave Pix informada."));
    }

    @Test
    @DisplayName("GET /search sem a query param 'chave' devolve 404")
    void lookupSemChave() {
        given()
                .when().get("/pix-directory/search")
                .then()
                .statusCode(404);
    }
}
