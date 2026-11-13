package com.stfgames.puzzle.controller;

import com.stfgames.puzzle.service.PuzzleService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/puzzle")
public class PuzzleController {
    private final PuzzleService service;

    public PuzzleController(PuzzleService service) {
        this.service = service;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return service.obtenerEstado();
    }

    @PostMapping("/iniciar")
    public Map<String, Object> iniciar(@RequestBody Map<String, Integer> datos) {
        return service.iniciar(datos.get("size"));
    }

    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> imagen(@RequestParam("file") MultipartFile foto) throws IOException {
        return service.subirImagen(foto);
    }

    @PostMapping("/mover")
    public Map<String, Object> mover(@RequestBody Map<String, Integer> datos) {
        return service.mover(datos.get("index"));
    }
}
