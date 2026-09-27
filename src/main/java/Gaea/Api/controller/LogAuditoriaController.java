package Gaea.Api.controller;

import Gaea.Api.model.LogAuditoria;
import Gaea.Api.service.LogAuditoriaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "http://localhost:5173")
public class LogAuditoriaController {

    private final LogAuditoriaService logAuditoriaService;

    public LogAuditoriaController(LogAuditoriaService logAuditoriaService) {
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping
    public List<LogAuditoria> listar() {
        return logAuditoriaService.listarTodos();
    }
}