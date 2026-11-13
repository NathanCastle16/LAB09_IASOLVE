CREATE TABLE IF NOT EXISTS stf_puzzle_sv (
    stf_game_board_structure VARCHAR(2000) NOT NULL,
    image LONGTEXT NOT NULL
);
INSERT INTO stf_puzzle_sv (stf_game_board_structure, image)
SELECT '', '' WHERE NOT EXISTS (SELECT 1 FROM stf_puzzle_sv);
