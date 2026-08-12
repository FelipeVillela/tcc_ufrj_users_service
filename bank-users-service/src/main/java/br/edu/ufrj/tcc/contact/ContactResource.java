package br.edu.ufrj.tcc.contact;

import java.util.List;

import br.edu.ufrj.tcc.contact.dto.ContactResponse;
import br.edu.ufrj.tcc.contact.dto.SaveContactRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Lista de contatos recentes de um usuário.
 * A tela de contatos exibe as colunas nome e chave Pix (ver {@link ContactResponse}).
 */
@Path("/users/{userId}/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContactResource {

    private final ContactService service;

    public ContactResource(ContactService service) {
        this.service = service;
    }

    /** Contatos recentes (mais recentes primeiro). */
    @GET
    public List<ContactResponse> listar(@PathParam("userId") Long userId) {
        return service.listarRecentes(userId).stream().map(ContactResponse::from).toList();
    }

    /**
     * Busca contatos por 'nome' ou por palavra-chave de 'nomeAlternativo'.
     * Ex.: /users/1/contacts/search?termo=mãe&por=nomesAlternativos
     */
    @GET
    @Path("/search")
    public List<ContactResponse> buscar(
            @PathParam("userId") Long userId,
            @QueryParam("termo") String termo,
            @QueryParam("por") String por) {
        return service.buscar(userId, termo, por).stream().map(ContactResponse::from).toList();
    }

    /** Salva/atualiza o contato ao enviar um Pix (com permissão do usuário). */
    @POST
    public Response salvar(@PathParam("userId") Long userId, @Valid SaveContactRequest req) {
        Contact contato = service.salvarAoEnviar(userId, req);
        return Response.status(Response.Status.CREATED).entity(ContactResponse.from(contato)).build();
    }

    @DELETE
    @Path("/{contactId}")
    public Response remover(@PathParam("userId") Long userId, @PathParam("contactId") String contactId) {
        service.remover(userId, contactId);
        return Response.noContent().build();
    }
}
