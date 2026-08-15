package br.edu.ufrj.tcc.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.contact.dto.SaveContactRequest;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Testes unitários do {@link ContactService}. As consultas ao MongoDB são
 * mockadas com {@link PanacheMock}, então os testes verificam as regras
 * (filtro montado, ordenação, merge de nomes alternativos) sem depender do
 * conteúdo do banco. O {@code update()} do contato existente é neutralizado
 * no spy — o {@code persist()} do contato novo, por ser método de instância,
 * o PanacheMock não intercepta e segue pelo Panache normalmente.
 */
@QuarkusTest
@DisplayName("ContactService — regras da lista de contatos")
class ContactServiceTest {

    private static final long DONO = 1L;
    private static final String LISTAR_RECENTES = "ownerUserId = ?1";
    private static final String POR_CHAVE = "ownerUserId = ?1 and chavePix = ?2";
    private static final String REMOVER = "ownerUserId = ?1 and _id = ?2";

    @Inject
    ContactService service;

    private PanacheQuery<Contact> query;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void prepararQuery() {
        query = Mockito.mock(PanacheQuery.class);
    }

    /**
     * O Panache entrega os parâmetros varargs ao mock como UM único
     * {@code Object[]}: por isso eles precisam ser casados por um matcher só.
     */
    private static Object[] params(Object... valores) {
        return Mockito.eq(valores);
    }

    private static Contact contato(String id, String nome, String chavePix, String banco, List<String> alternativos) {
        Contact c = new Contact();
        c.id = new ObjectId(id);
        c.ownerUserId = DONO;
        c.nome = nome;
        c.chavePix = chavePix;
        c.banco = banco;
        c.nomesAlternativos = new ArrayList<>(alternativos);
        return c;
    }

    // ------------------------------------------------------------------
    // Campo de busca
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Campo.of aceita 'nome', 'nomeAlternativo' e 'nomesAlternativos' (case-insensitive)")
    void campoDeBuscaAceito() {
        assertEquals(ContactService.Campo.NOME, ContactService.Campo.of(null));
        assertEquals(ContactService.Campo.NOME, ContactService.Campo.of("nome"));
        assertEquals(ContactService.Campo.NOME, ContactService.Campo.of("NOME"));
        assertEquals(ContactService.Campo.NOME_ALTERNATIVO, ContactService.Campo.of("nomeAlternativo"));
        assertEquals(ContactService.Campo.NOME_ALTERNATIVO, ContactService.Campo.of("nomesAlternativos"));
        assertEquals(ContactService.Campo.NOME_ALTERNATIVO, ContactService.Campo.of("NOMESALTERNATIVOS"));

        assertEquals("nome", ContactService.Campo.NOME.mongoField);
        assertEquals("nomesAlternativos", ContactService.Campo.NOME_ALTERNATIVO.mongoField);
    }

    @Test
    @DisplayName("Campo.of lança 400 para um campo de busca desconhecido")
    void campoDeBuscaInvalido() {
        BusinessException erro = assertThrows(BusinessException.class, () -> ContactService.Campo.of("chavePix"));

        assertEquals(Response.Status.BAD_REQUEST, erro.getStatus());
        assertEquals("Parâmetro de busca inválido. Use 'nome' ou 'nomeAlternativo'.", erro.getMessage());
    }

    @Test
    @DisplayName("buscar rejeita campo inválido antes de consultar o banco")
    void buscarComCampoInvalido() {
        PanacheMock.mock(Contact.class);

        assertThrows(BusinessException.class, () -> service.buscar(DONO, "carla", "banco"));

        PanacheMock.verifyNoInteractions(Contact.class);
    }

    // ------------------------------------------------------------------
    // listarRecentes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listarRecentes filtra pelo dono e ordena por usadoPorUltimoEm decrescente")
    void listarRecentes() {
        PanacheMock.mock(Contact.class);
        Contact carla = contato("507f1f77bcf86cd799439011", "Carla Pereira", "carla@gmail.com", "BB", List.of("mãe"));
        Contact joao = contato("507f1f77bcf86cd799439012", "João Silva", "11122233344", "Nubank", List.of("irmão"));
        Mockito.when(Contact.<Contact>find(Mockito.eq(LISTAR_RECENTES), Mockito.any(Sort.class), params(DONO)))
                .thenReturn(query);
        Mockito.when(query.list()).thenReturn(List.of(carla, joao));

        List<Contact> recentes = service.listarRecentes(DONO);

        assertEquals(List.of(carla, joao), recentes);

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        PanacheMock.verify(Contact.class).find(Mockito.eq(LISTAR_RECENTES), sort.capture(), params(DONO));
        List<Sort.Column> colunas = sort.getValue().getColumns();
        assertEquals(1, colunas.size());
        assertEquals("usadoPorUltimoEm", colunas.get(0).getName());
        assertEquals(Sort.Direction.Descending, colunas.get(0).getDirection());
    }

    // ------------------------------------------------------------------
    // buscar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("buscar sem termo cai no comportamento de listarRecentes")
    void buscarSemTermo() {
        PanacheMock.mock(Contact.class);
        Contact carla = contato("507f1f77bcf86cd799439011", "Carla Pereira", "carla@gmail.com", "BB", List.of("mãe"));
        Mockito.when(Contact.<Contact>find(Mockito.eq(LISTAR_RECENTES), Mockito.any(Sort.class), params(DONO)))
                .thenReturn(query);
        Mockito.when(query.list()).thenReturn(List.of(carla));

        assertEquals(List.of(carla), service.buscar(DONO, null, "nome"));
        assertEquals(List.of(carla), service.buscar(DONO, "   ", "nome"));

        PanacheMock.verify(Contact.class, Mockito.times(2))
                .find(Mockito.eq(LISTAR_RECENTES), Mockito.any(Sort.class), params(DONO));
    }

    @Test
    @DisplayName("buscar por nome monta filtro do dono + regex case-insensitive em 'nome'")
    void buscarPorNome() {
        PanacheMock.mock(Contact.class);
        Contact carla = contato("507f1f77bcf86cd799439011", "Carla Pereira", "carla@gmail.com", "BB", List.of("mãe"));
        Mockito.when(Contact.<Contact>find(Mockito.any(Document.class))).thenReturn(query);
        Mockito.when(query.list()).thenReturn(List.of(carla));

        assertEquals(List.of(carla), service.buscar(DONO, "carla", "nome"));

        Document filtro = filtroCapturado();
        assertEquals(DONO, filtro.get("ownerUserId"));
        Document regex = filtro.get("nome", Document.class);
        assertEquals("carla", regex.getString("$regex"));
        assertEquals("i", regex.getString("$options"));
        assertTrue(filtro.get("nomesAlternativos") == null, "não deve filtrar pelo campo de nomes alternativos");
    }

    @Test
    @DisplayName("buscar por nomeAlternativo usa o campo 'nomesAlternativos'")
    void buscarPorNomeAlternativo() {
        PanacheMock.mock(Contact.class);
        Contact carla = contato("507f1f77bcf86cd799439011", "Carla Pereira", "carla@gmail.com", "BB", List.of("mãe"));
        Mockito.when(Contact.<Contact>find(Mockito.any(Document.class))).thenReturn(query);
        Mockito.when(query.list()).thenReturn(List.of(carla));

        assertEquals(List.of(carla), service.buscar(DONO, "mãe", "nomeAlternativo"));

        Document filtro = filtroCapturado();
        assertEquals(DONO, filtro.get("ownerUserId"));
        assertEquals("mãe", filtro.get("nomesAlternativos", Document.class).getString("$regex"));
    }

    @Test
    @DisplayName("buscar escapa metacaracteres: o termo é tratado como texto literal")
    void buscarEscapaRegex() {
        PanacheMock.mock(Contact.class);
        Mockito.when(Contact.<Contact>find(Mockito.any(Document.class))).thenReturn(query);
        Mockito.when(query.list()).thenReturn(List.of());

        service.buscar(DONO, "a.b*c(d)", "nome");

        assertEquals("a\\.b\\*c\\(d\\)", filtroCapturado().get("nome", Document.class).getString("$regex"));
    }

    private Document filtroCapturado() {
        ArgumentCaptor<Document> filtro = ArgumentCaptor.forClass(Document.class);
        PanacheMock.verify(Contact.class).find(filtro.capture());
        return filtro.getValue();
    }

    // ------------------------------------------------------------------
    // salvarAoEnviar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("salvarAoEnviar cria o contato quando a chave ainda não está na lista")
    void salvarContatoNovo() {
        PanacheMock.mock(Contact.class);
        Mockito.when(Contact.<Contact>find(POR_CHAVE, DONO, "ana@outlook.com")).thenReturn(query);
        Mockito.when(query.<Contact>firstResult()).thenReturn(null);

        Contact salvo = service.salvarAoEnviar(DONO,
                new SaveContactRequest("Ana Souza", "ana@outlook.com", "Itaú", List.of("prima")));

        assertEquals(DONO, salvo.ownerUserId);
        assertEquals("Ana Souza", salvo.nome);
        assertEquals("ana@outlook.com", salvo.chavePix);
        assertEquals("Itaú", salvo.banco);
        assertEquals(List.of("prima"), salvo.nomesAlternativos);
        assertNotNull(salvo.criadoEm);
        assertNotNull(salvo.usadoPorUltimoEm);
    }

    @Test
    @DisplayName("salvarAoEnviar aceita contato novo sem nomes alternativos (lista vazia, nunca nula)")
    void salvarContatoNovoSemNomesAlternativos() {
        PanacheMock.mock(Contact.class);
        Mockito.when(Contact.<Contact>find(POR_CHAVE, DONO, "ana@outlook.com")).thenReturn(query);
        Mockito.when(query.<Contact>firstResult()).thenReturn(null);

        Contact salvo = service.salvarAoEnviar(DONO,
                new SaveContactRequest("Ana Souza", "ana@outlook.com", "Itaú", null));

        assertNotNull(salvo.nomesAlternativos);
        assertTrue(salvo.nomesAlternativos.isEmpty());
    }

    @Test
    @DisplayName("salvarAoEnviar atualiza o contato existente e agrega novos nomes alternativos sem duplicar")
    void salvarContatoExistente() {
        PanacheMock.mock(Contact.class);
        Contact existente = Mockito.spy(
                contato("507f1f77bcf86cd799439011", "Rafaela", "rafa@gmail.com", "Nubank", List.of("irmã", "rafa")));
        Mockito.doNothing().when(existente).update();
        Instant usoAnterior = Instant.now().minus(3, ChronoUnit.DAYS);
        existente.usadoPorUltimoEm = usoAnterior;
        Mockito.when(Contact.<Contact>find(POR_CHAVE, DONO, "rafa@gmail.com")).thenReturn(query);
        Mockito.when(query.<Contact>firstResult()).thenReturn(existente);

        Contact salvo = service.salvarAoEnviar(DONO,
                new SaveContactRequest("Rafaela Fernandes", "rafa@gmail.com", "Inter", List.of("rafa", "madrinha")));

        assertSame(existente, salvo);
        assertEquals("Rafaela Fernandes", salvo.nome);
        assertEquals("Inter", salvo.banco);
        assertEquals(List.of("irmã", "rafa", "madrinha"), salvo.nomesAlternativos);
        assertTrue(salvo.usadoPorUltimoEm.isAfter(usoAnterior), "o 'usado por último' deve avançar");
        Mockito.verify(existente).update();
    }

    @Test
    @DisplayName("salvarAoEnviar mantém o banco atual quando o novo não é informado")
    void salvarContatoExistenteSemBanco() {
        PanacheMock.mock(Contact.class);
        Contact existente = Mockito.spy(
                contato("507f1f77bcf86cd799439011", "Rafaela", "rafa@gmail.com", "Nubank", List.of("irmã")));
        Mockito.doNothing().when(existente).update();
        Mockito.when(Contact.<Contact>find(POR_CHAVE, DONO, "rafa@gmail.com")).thenReturn(query);
        Mockito.when(query.<Contact>firstResult()).thenReturn(existente);

        Contact salvo = service.salvarAoEnviar(DONO,
                new SaveContactRequest("Rafaela Fernandes", "rafa@gmail.com", null, null));

        assertEquals("Nubank", salvo.banco);
        assertEquals(List.of("irmã"), salvo.nomesAlternativos);
    }

    @Test
    @DisplayName("salvarAoEnviar aceita contato existente sem lista de nomes alternativos")
    void salvarContatoExistenteSemNomesAlternativos() {
        PanacheMock.mock(Contact.class);
        Contact existente = Mockito.spy(
                contato("507f1f77bcf86cd799439011", "Rafaela", "rafa@gmail.com", "Nubank", List.of()));
        existente.nomesAlternativos = null;
        Mockito.doNothing().when(existente).update();
        Mockito.when(Contact.<Contact>find(POR_CHAVE, DONO, "rafa@gmail.com")).thenReturn(query);
        Mockito.when(query.<Contact>firstResult()).thenReturn(existente);

        Contact salvo = service.salvarAoEnviar(DONO,
                new SaveContactRequest("Rafaela Fernandes", "rafa@gmail.com", "Nubank", List.of("rafa")));

        assertEquals(List.of("rafa"), salvo.nomesAlternativos);
    }

    // ------------------------------------------------------------------
    // remover
    // ------------------------------------------------------------------

    @Test
    @DisplayName("remover apaga o contato do próprio usuário")
    void removerContato() {
        PanacheMock.mock(Contact.class);
        String id = "507f1f77bcf86cd799439011";
        Mockito.when(Contact.delete(REMOVER, DONO, new ObjectId(id))).thenReturn(1L);

        service.remover(DONO, id);

        PanacheMock.verify(Contact.class).delete(REMOVER, DONO, new ObjectId(id));
    }

    @Test
    @DisplayName("remover lança 404 quando o contato não é do usuário (ou não existe)")
    void removerContatoInexistente() {
        PanacheMock.mock(Contact.class);
        String id = "507f1f77bcf86cd799439011";
        Mockito.when(Contact.delete(REMOVER, DONO, new ObjectId(id))).thenReturn(0L);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.remover(DONO, id));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
        assertEquals("Contato " + id + " não encontrado para o usuário 1.", erro.getMessage());
    }
}
