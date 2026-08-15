package br.edu.ufrj.tcc.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.user.dto.AddPixKeyRequest;
import br.edu.ufrj.tcc.user.dto.CreateUserRequest;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Testes unitários do {@link UserService}: as consultas (métodos estáticos do
 * Panache) são mockadas com {@link PanacheMock}, de modo que só as regras de
 * negócio do service são exercitadas.
 *
 * {@link TestTransaction} envolve cada teste numa transação com rollback ao
 * final: os {@code persist()} disparados dentro do service (que o PanacheMock
 * não intercepta, por serem métodos de instância) nunca chegam a ser
 * confirmados no banco, e um teste não enxerga o que o outro escreveu.
 */
@QuarkusTest
@TestTransaction
@DisplayName("UserService — regras de usuários e chaves Pix")
class UserServiceTest {

    @Inject
    UserService service;

    private static User usuario(long id, String email, BigDecimal saldo) {
        User user = new User("Felipe Augusto", email, "senha123", saldo);
        user.id = id;
        return user;
    }

    private static PixKey chave(long id, PixKeyType tipo, String valor, User dono) {
        PixKey chave = new PixKey(tipo, valor, dono);
        chave.id = id;
        return chave;
    }

    // ------------------------------------------------------------------
    // listarTodos / buscarPorId / saldoDe
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listarTodos devolve todos os usuários persistidos")
    void listarTodos() {
        PanacheMock.mock(User.class);
        User a = usuario(1L, "a@ufrj.br", new BigDecimal("10.00"));
        User b = usuario(2L, "b@ufrj.br", new BigDecimal("20.00"));
        Mockito.when(User.<User>listAll()).thenReturn(List.of(a, b));

        List<User> encontrados = service.listarTodos();

        assertEquals(List.of(a, b), encontrados);
    }

    @Test
    @DisplayName("listarTodos devolve lista vazia quando não há usuários")
    void listarTodosVazio() {
        PanacheMock.mock(User.class);
        Mockito.when(User.<User>listAll()).thenReturn(List.of());

        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("buscarPorId devolve o usuário quando ele existe")
    void buscarPorIdExistente() {
        PanacheMock.mock(User.class);
        User felipe = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        Mockito.when(User.<User>findById(1L)).thenReturn(felipe);

        assertSame(felipe, service.buscarPorId(1L));
    }

    @Test
    @DisplayName("buscarPorId lança 404 quando o usuário não existe")
    void buscarPorIdInexistente() {
        PanacheMock.mock(User.class);
        Mockito.when(User.<User>findById(99L)).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.buscarPorId(99L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
        assertEquals("Usuário 99 não encontrado.", erro.getMessage());
    }

    @Test
    @DisplayName("saldoDe devolve o saldo do usuário")
    void saldoDeUsuarioExistente() {
        PanacheMock.mock(User.class);
        Mockito.when(User.<User>findById(1L)).thenReturn(usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00")));

        assertEquals(new BigDecimal("2500.00"), service.saldoDe(1L));
    }

    @Test
    @DisplayName("saldoDe propaga o 404 de usuário inexistente")
    void saldoDeUsuarioInexistente() {
        PanacheMock.mock(User.class);
        Mockito.when(User.<User>findById(99L)).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.saldoDe(99L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
    }

    // ------------------------------------------------------------------
    // criar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("criar persiste o usuário com o saldo inicial informado")
    void criarComSaldoInicial() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail("maria@ufrj.br")).thenReturn(null);

        User criado = service.criar(
                new CreateUserRequest("Maria Silva", "maria@ufrj.br", "senha123", new BigDecimal("1500.75")));

        assertEquals("Maria Silva", criado.nome);
        assertEquals("maria@ufrj.br", criado.email);
        assertEquals("senha123", criado.senha);
        assertEquals(new BigDecimal("1500.75"), criado.saldo);
        assertNotNull(criado.criadoEm);
        assertTrue(criado.chavesPix.isEmpty());
    }

    @Test
    @DisplayName("criar assume saldo zero quando saldoInicial vem nulo")
    void criarSemSaldoInicialUsaZero() {
        PanacheMock.mock(User.class);
        Mockito.when(User.findByEmail("joao@ufrj.br")).thenReturn(null);

        User criado = service.criar(new CreateUserRequest("João", "joao@ufrj.br", "senha123", null));

        assertEquals(BigDecimal.ZERO, criado.saldo);
    }

    @Test
    @DisplayName("criar lança 409 quando o e-mail já está cadastrado")
    void criarComEmailDuplicado() {
        PanacheMock.mock(User.class);
        User existente = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        Mockito.when(User.findByEmail("felipe@poli.ufrj.br")).thenReturn(existente);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.criar(
                new CreateUserRequest("Outro Felipe", "felipe@poli.ufrj.br", "outrasenha", BigDecimal.TEN)));

        assertEquals(Response.Status.CONFLICT, erro.getStatus());
        assertEquals("Já existe um usuário com o e-mail felipe@poli.ufrj.br.", erro.getMessage());
    }

    // ------------------------------------------------------------------
    // adicionarChavePix / chavesDe
    // ------------------------------------------------------------------

    @Test
    @DisplayName("adicionarChavePix cria a chave, vincula ao dono e a inclui na lista do usuário")
    void adicionarChavePix() {
        PanacheMock.mock(User.class, PixKey.class);
        User felipe = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        Mockito.when(User.<User>findById(1L)).thenReturn(felipe);
        Mockito.when(PixKey.findByValor("09876543210")).thenReturn(null);

        PixKey criada = service.adicionarChavePix(1L, new AddPixKeyRequest(PixKeyType.CPF, "09876543210"));

        assertEquals(PixKeyType.CPF, criada.tipo);
        assertEquals("09876543210", criada.valor);
        assertSame(felipe, criada.user);
        assertNotNull(criada.criadoEm);
        assertEquals(List.of(criada), felipe.chavesPix);
    }

    @Test
    @DisplayName("adicionarChavePix lança 404 quando o usuário não existe")
    void adicionarChavePixUsuarioInexistente() {
        PanacheMock.mock(User.class, PixKey.class);
        Mockito.when(User.<User>findById(99L)).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class,
                () -> service.adicionarChavePix(99L, new AddPixKeyRequest(PixKeyType.EMAIL, "x@ufrj.br")));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
    }

    @Test
    @DisplayName("adicionarChavePix lança 409 quando a chave já existe (unicidade global do Pix)")
    void adicionarChavePixDuplicada() {
        PanacheMock.mock(User.class, PixKey.class);
        User felipe = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        User outro = usuario(2L, "outro@ufrj.br", BigDecimal.ZERO);
        Mockito.when(User.<User>findById(1L)).thenReturn(felipe);
        Mockito.when(PixKey.findByValor("ja@existe.br")).thenReturn(chave(7L, PixKeyType.EMAIL, "ja@existe.br", outro));

        BusinessException erro = assertThrows(BusinessException.class,
                () -> service.adicionarChavePix(1L, new AddPixKeyRequest(PixKeyType.EMAIL, "ja@existe.br")));

        assertEquals(Response.Status.CONFLICT, erro.getStatus());
        assertEquals("A chave pix 'ja@existe.br' já está cadastrada.", erro.getMessage());
        assertTrue(felipe.chavesPix.isEmpty(), "a chave não pode ser adicionada ao usuário quando há conflito");
    }

    @Test
    @DisplayName("chavesDe devolve as chaves do usuário")
    void chavesDeUsuario() {
        PanacheMock.mock(User.class);
        User felipe = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        PixKey email = chave(10L, PixKeyType.EMAIL, "felipe@poli.ufrj.br", felipe);
        PixKey cpf = chave(11L, PixKeyType.CPF, "09876543210", felipe);
        felipe.chavesPix.addAll(List.of(email, cpf));
        Mockito.when(User.<User>findById(1L)).thenReturn(felipe);

        assertEquals(List.of(email, cpf), service.chavesDe(1L));
    }

    @Test
    @DisplayName("chavesDe propaga o 404 de usuário inexistente")
    void chavesDeUsuarioInexistente() {
        PanacheMock.mock(User.class);
        Mockito.when(User.<User>findById(99L)).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.chavesDe(99L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
    }

    // ------------------------------------------------------------------
    // removerChavePix
    // ------------------------------------------------------------------

    @Test
    @DisplayName("removerChavePix apaga a chave quando ela pertence ao usuário")
    void removerChavePix() {
        PanacheMock.mock(PixKey.class);
        User felipe = usuario(1L, "felipe@poli.ufrj.br", new BigDecimal("2500.00"));
        PixKey alvo = Mockito.spy(chave(10L, PixKeyType.EMAIL, "felipe@poli.ufrj.br", felipe));
        Mockito.doNothing().when(alvo).delete();
        Mockito.when(PixKey.<PixKey>findById(10L)).thenReturn(alvo);

        service.removerChavePix(1L, 10L);

        Mockito.verify(alvo).delete();
    }

    @Test
    @DisplayName("removerChavePix lança 404 quando a chave não existe")
    void removerChavePixInexistente() {
        PanacheMock.mock(PixKey.class);
        Mockito.when(PixKey.<PixKey>findById(404L)).thenReturn(null);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.removerChavePix(1L, 404L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
        assertEquals("Chave pix 404 não encontrada para o usuário 1.", erro.getMessage());
    }

    @Test
    @DisplayName("removerChavePix lança 404 quando a chave é de outro usuário (não apaga)")
    void removerChavePixDeOutroUsuario() {
        PanacheMock.mock(PixKey.class);
        User outro = usuario(2L, "outro@ufrj.br", BigDecimal.ZERO);
        PixKey alvo = Mockito.spy(chave(10L, PixKeyType.EMAIL, "outro@ufrj.br", outro));
        Mockito.when(PixKey.<PixKey>findById(10L)).thenReturn(alvo);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.removerChavePix(1L, 10L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
        Mockito.verify(alvo, Mockito.never()).delete();
    }

    @Test
    @DisplayName("removerChavePix lança 404 quando a chave está órfã (sem dono)")
    void removerChavePixSemDono() {
        PanacheMock.mock(PixKey.class);
        PixKey alvo = Mockito.spy(chave(10L, PixKeyType.ALEATORIA, "sem-dono", null));
        Mockito.when(PixKey.<PixKey>findById(10L)).thenReturn(alvo);

        BusinessException erro = assertThrows(BusinessException.class, () -> service.removerChavePix(1L, 10L));

        assertEquals(Response.Status.NOT_FOUND, erro.getStatus());
        Mockito.verify(alvo, Mockito.never()).delete();
    }
}
