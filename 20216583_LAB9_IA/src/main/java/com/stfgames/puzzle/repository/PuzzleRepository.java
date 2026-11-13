package com.stfgames.puzzle.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PuzzleRepository {
    private final JdbcTemplate jdbc;

    public record JuegoGuardado(String tablero, String imagen) {}

    public PuzzleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public JuegoGuardado obtenerJuego() {
        List<JuegoGuardado> filas = jdbc.query(
            "SELECT stf_game_board_structure, image FROM stf_puzzle_sv LIMIT 2",
            (rs, rowNum) -> new JuegoGuardado(
                rs.getString("stf_game_board_structure"), rs.getString("image"))
        );
        if (filas.isEmpty()) {
            throw new IllegalStateException("La tabla stf_puzzle_sv no tiene su fila inicial. Ejecuta CODIGO_LAB09.sql.");
        }
        if (filas.size() != 1) {
            throw new IllegalStateException("La tabla stf_puzzle_sv debe contener exactamente UNA fila.");
        }
        return filas.get(0);
    }

    public void guardarTablero(String tablero) {
        int filas = jdbc.update("UPDATE stf_puzzle_sv SET stf_game_board_structure = ?", tablero);
        if (filas != 1) throw new IllegalStateException("Se esperaba actualizar exactamente una fila.");
    }

    public void guardarImagen(String imagenBase64) {
        int filas = jdbc.update("UPDATE stf_puzzle_sv SET image = ?", imagenBase64);
        if (filas != 1) throw new IllegalStateException("Se esperaba actualizar exactamente una fila.");
    }
}
