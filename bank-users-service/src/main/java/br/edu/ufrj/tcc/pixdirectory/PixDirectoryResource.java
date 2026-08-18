package br.edu.ufrj.tcc.pixdirectory;

import br.edu.ufrj.tcc.common.BusinessException;
import br.edu.ufrj.tcc.pixdirectory.dto.PixOwnerResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Consulta do titular de uma chave Pix (nome e banco). Modela, de forma
 * mockada, o papel do DICT no arranjo Pix — ver {@link PixDirectoryService}.
 */
@Path("/pix-directory")
@Produces(MediaType.APPLICATION_JSON)
public class PixDirectoryResource {

    private final PixDirectoryService service;

    public PixDirectoryResource(PixDirectoryService service) {
        this.service = service;
    }

    /**
     * Resolve nome e banco a partir da chave Pix.
     * Ex.: /pix-directory/search?chave=padaria@pix.com
     * Responde 404 quando a chave não está no diretório.
     */
    @GET
    @Path("/search")
    public PixOwnerResponse lookup(@QueryParam("chave") String chave) {
        return service.consultar(chave)
                .orElseThrow(() -> BusinessException.notFound(
                        "Nenhum titular encontrado para a chave Pix informada."));
    }
}
