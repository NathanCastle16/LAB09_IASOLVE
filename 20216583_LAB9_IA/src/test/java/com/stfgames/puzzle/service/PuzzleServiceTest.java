package com.stfgames.puzzle.service;

import com.stfgames.puzzle.repository.PuzzleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PuzzleServiceTest {
    @Test
    void generaTablero3x3MedianteOchoMovimientos() {
        PuzzleRepository repo = mock(PuzzleRepository.class);
        PuzzleService service = new PuzzleService(repo);
        Map<String, Object> resultado = service.iniciar(3);
        assertEquals(List.of(4,1,3,7,2,5,8,6,0), resultado.get("board"));
        verify(repo).guardarTablero("41372586X");
    }

    @Test
    void rechazaCasillaNoContigua() {
        PuzzleRepository repo = mock(PuzzleRepository.class);
        when(repo.obtenerJuego()).thenReturn(new PuzzleRepository.JuegoGuardado("41372586X", ""));
        PuzzleService service = new PuzzleService(repo);
        assertThrows(ResponseStatusException.class, () -> service.mover(0));
        verify(repo, never()).guardarTablero(anyString());
    }

    @Test
    void aceptaMovimientoYLoGuardaEnBaseDeDatos() {
        PuzzleRepository repo = mock(PuzzleRepository.class);
        when(repo.obtenerJuego()).thenReturn(new PuzzleRepository.JuegoGuardado("41372586X", ""));
        PuzzleService service = new PuzzleService(repo);
        Map<String, Object> resultado = service.mover(7);
        assertEquals(List.of(4,1,3,7,2,5,8,0,6), resultado.get("board"));
        verify(repo).guardarTablero("4137258X6");
    }

    @Test
    void recuperaPartidaPersistida() {
        PuzzleRepository repo = mock(PuzzleRepository.class);
        when(repo.obtenerJuego()).thenReturn(new PuzzleRepository.JuegoGuardado("41372586X", ""));
        PuzzleService service = new PuzzleService(repo);
        Map<String, Object> estado = service.obtenerEstado();
        assertEquals(true, estado.get("started"));
        assertEquals(List.of(4,1,3,7,2,5,8,6,0), estado.get("board"));
    }
}
