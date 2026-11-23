package Gaea.Api.controller;

import Gaea.Api.dto.DocumentosLegaisResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/documentos-legais")
public class DocumentosLegaisController {

    @GetMapping
    public DocumentosLegaisResponse consultarVersoesAtuais() {
        return DocumentosLegaisResponse.atual();
    }
}
