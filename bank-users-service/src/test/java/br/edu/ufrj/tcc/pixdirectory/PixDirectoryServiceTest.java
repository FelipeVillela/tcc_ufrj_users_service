package br.edu.ufrj.tcc.pixdirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ufrj.tcc.pixdirectory.dto.PixOwnerResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Testes do {@link PixDirectoryService}: a resolução mockada de titular por
 * chave Pix, incluindo normalização (trim/case) e chave desconhecida.
 */
@QuarkusTest
@DisplayName("PixDirectoryService — diretório Pix mockado")
class PixDirectoryServiceTest {

    @Inject
    PixDirectoryService service;

    @Test
    @DisplayName("consultar resolve nome e banco de uma chave conhecida")
    void chaveConhecida() {
        PixOwnerResponse titular = service.consultar("padaria@pix.com").orElseThrow();

        assertEquals("padaria@pix.com", titular.chavePix());
        assertEquals("Padaria Pão Quente", titular.nome());
        assertEquals("Banco Amarelo", titular.banco());
    }

    @Test
    @DisplayName("consultar ignora espaços e caixa, preservando a chave informada")
    void chaveNormalizada() {
        PixOwnerResponse titular = service.consultar("  Padaria@Pix.com  ").orElseThrow();

        assertEquals("Padaria@Pix.com", titular.chavePix());
        assertEquals("Padaria Pão Quente", titular.nome());
    }

    @Test
    @DisplayName("consultar não encontra chave fora do diretório")
    void chaveDesconhecida() {
        assertTrue(service.consultar("ninguem@pix.com").isEmpty());
    }

    @Test
    @DisplayName("consultar trata chave nula ou em branco como não encontrada")
    void chaveVazia() {
        assertEquals(Optional.empty(), service.consultar(null));
        assertEquals(Optional.empty(), service.consultar("   "));
    }
}
