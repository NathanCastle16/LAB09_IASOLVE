
CREATE DATABASE IF NOT EXISTS stf_games_20216583
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stf_games_20216583;

CREATE TABLE IF NOT EXISTS stf_puzzle_sv (
    stf_game_board_structure VARCHAR(2000) NOT NULL,
    image LONGTEXT NOT NULL
);

-- La aplicacion SOLO actualiza esta fila: no crea filas adicionales.
INSERT INTO stf_puzzle_sv(stf_game_board_structure, image)
SELECT '', '' WHERE NOT EXISTS (SELECT 1 FROM stf_puzzle_sv);

-- Comprobaciones:
SELECT * FROM stf_puzzle_sv;
SELECT COUNT(*) AS total_filas FROM stf_puzzle_sv;
SHOW COLUMNS FROM stf_puzzle_sv;
