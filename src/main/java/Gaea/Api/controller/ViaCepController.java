package Gaea.Api.controller;

import Gaea.Api.model.ViaCepResponse;
import Gaea.Api.service.ViaCepService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cep")
@CrossOrigin(origins = "http://localhost:5173")
public class ViaCepController {

    private final ViaCepService viaCepService;

    public ViaCepController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

    @GetMapping("/{cep}")
    public ViaCepResponse buscarCep(@PathVariable String cep) {
        return viaCepService.buscarCep(cep);
    }
}