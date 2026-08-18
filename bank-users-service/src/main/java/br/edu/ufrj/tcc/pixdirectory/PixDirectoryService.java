package br.edu.ufrj.tcc.pixdirectory;

import java.util.Map;
import java.util.Optional;

import br.edu.ufrj.tcc.pixdirectory.dto.PixOwnerResponse;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * "Diretório Pix" mockado: resolve o titular (nome e banco) a partir de uma
 * chave Pix. No arranjo real essa consulta seria feita ao DICT, gerido pelo
 * Banco Central; como não há integração, usamos uma tabela fixa em memória.
 *
 * As chaves aqui NÃO estão na lista de contatos do seed ({@code DevDataSeeder}),
 * de propósito: são destinatários "novos", para demonstrar o fluxo de resolver
 * um desconhecido pela chave e salvá-lo como contato após o envio. Uma chave
 * fora desta tabela não é encontrada (o recurso responde 404), e o agente pede
 * o nome ao usuário.
 */
@ApplicationScoped
public class PixDirectoryService {

    private record Titular(String nome, String banco) {
    }

    /** 
     * Mock de chaves pix para exemplo. 
     * Em um cenário real, a consulta seria feita ao DICT (Banco Central).
     * */
    private static final Map<String, Titular> DIRETORIO = Map.ofEntries(
            Map.entry("padaria@pix.com", new Titular("Padaria Pão Quente", "Banco Amarelo")),
            Map.entry("bruno.tavares@pix.com", new Titular("Bruno Tavares", "Banco Vermelho")),
            Map.entry("21970001234", new Titular("Marcos Andrade", "Banco Verde")),
            Map.entry("11960002222", new Titular("Patrícia Gomes", "Banco Laranja")),
            Map.entry("12345678900", new Titular("Juliana Ramos", "Banco Roxo")));

    /**
     * Resolve o titular da chave Pix. A busca é case-insensitive e ignora
     * espaços nas bordas. Devolve vazio quando a chave é nula/em branco ou não
     * está no diretório.
     * Em um cenário real, essa função deveria integrar com o Banco Central (DICT).
     */
    public Optional<PixOwnerResponse> consultar(String chave) {
        if (chave == null || chave.isBlank()) {
            return Optional.empty();
        }
        String normalizada = chave.trim();
        Titular titular = DIRETORIO.get(normalizada.toLowerCase());
        if (titular == null) {
            return Optional.empty();
        }
        return Optional.of(new PixOwnerResponse(normalizada, titular.nome(), titular.banco()));
    }
}
