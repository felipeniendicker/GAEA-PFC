package Gaea.Api.controller;

import Gaea.Api.dto.AnaliseDocumentoRequest;
import Gaea.Api.dto.ConteudoDocumento;
import Gaea.Api.dto.DocumentoProcessoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.DocumentoProcessoService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/processos/{processoId}/documentos")
public class DocumentoProcessoController {

    private final DocumentoProcessoService service;

    public DocumentoProcessoController(DocumentoProcessoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentoProcessoResponse enviar(
            @PathVariable Long processoId,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam(required = false) Long documentoAnteriorId,
            Authentication authentication) {
        return service.enviar(
                processoId,
                arquivo,
                documentoAnteriorId,
                (Usuario) authentication.getPrincipal());
    }

    @GetMapping
    public List<DocumentoProcessoResponse> listar(
            @PathVariable Long processoId,
            Authentication authentication) {
        return service.listar(processoId, (Usuario) authentication.getPrincipal());
    }

    @GetMapping("/{documentoId}/conteudo")
    public ResponseEntity<org.springframework.core.io.Resource> baixar(
            @PathVariable Long processoId,
            @PathVariable Long documentoId,
            Authentication authentication) {
        ConteudoDocumento conteudo = service.baixar(
                processoId, documentoId, (Usuario) authentication.getPrincipal());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(conteudo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(conteudo.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(conteudo.recurso());
    }

    @PutMapping("/{documentoId}/analise")
    public DocumentoProcessoResponse analisar(
            @PathVariable Long processoId,
            @PathVariable Long documentoId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AnaliseDocumentoRequest request,
            Authentication authentication) {
        return service.analisar(
                processoId,
                documentoId,
                request,
                (Usuario) authentication.getPrincipal());
    }
}
