# LAB09 – STF-Puzzle Simplified Version (GTICS)

Proyecto **Spring Boot + Java 17 + HTML/CSS/JavaScript + Web Services REST**. Implementa un rompecabezas NxN con subida de imagen **solo JPG**, validación, secuencia fija de ocho movimientos para desordenar, detección de victoria y persistencia de la partida.

## Requisitos

- Java **17 o superior**; IntelliJ IDEA (Community o Ultimate) con soporte Maven.
- Internet la primera vez para descargar dependencias Maven.
- Para la **entrega con MySQL**: MySQL Server y MySQL Workbench.

## Ejecución rápida (sin MySQL, para comprobar que abre)

1. Abre esta carpeta en IntelliJ: **File > Open** y selecciona **`pom.xml`** (importar proyecto Maven).
2. Espera que IntelliJ descargue las dependencias.
3. Abre `src/main/java/com/stfgames/puzzle/PuzzleApplication.java` y presiona **Run**.
4. Abre en el navegador **http://localhost:8080**.
5. El perfil por defecto `demo` usa H2 en archivo local: `data/stf_puzzle.mv.db` (también recuerda la partida).

> Para cumplir la entrega del profesor **se debe demostrar y utilizar el perfil MySQL** que sigue. H2 es únicamente para ejecutar de inmediato sin tener que configurar credenciales.

## Ejecución con MySQL (la entrega)

1. En MySQL Workbench, abre y ejecuta `CODIGO_LAB09.sql`.
2. **Reemplaza `CODIGO` por tu código** en el SQL y en `src/main/resources/application-mysql.properties` (variable `DB_NAME` o valor por defecto).
3. Configura la contraseña de MySQL: en IntelliJ, Run > Edit Configurations > Environment variables, agrega `DB_PASSWORD=tu_clave`. Si tu usuario no es root, agrega también `DB_USER=tu_usuario`. Si prefieres, modifica los valores por defecto de `application-mysql.properties`.
4. En IntelliJ: Run > Edit Configurations > Active profiles (si aparece) indica `mysql` **o** añade en VM Options `-Dspring.profiles.active=mysql`.
5. Ejecuta `PuzzleApplication.java` y abre **http://localhost:8080**.
6. En MySQL Workbench comprueba con:
   ```sql
   USE stf_games_CODIGO;
   SELECT stf_game_board_structure, LENGTH(image) FROM stf_puzzle_sv;
   SELECT COUNT(*) FROM stf_puzzle_sv;
   ```
   El contenido de `stf_game_board_structure` cambia en cada movimiento. `COUNT(*)` debe ser 1.
7. Actualiza el navegador o ciérralo y vuelve a abrirlo; se recupera el tablero actual.

**Alternativa Maven en la terminal** (desde la carpeta del proyecto):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

> No publiques tu contraseña en GitHub ni la agregues a este ZIP.

## Endpoints REST y cómo probarlos con Postman

| Método | URL | JSON/entrada | Función |
|---|---|---|---|
| GET | `/api/puzzle/estado` | ninguna | Recuperar tablero e imagen guardados |
| POST | `/api/puzzle/iniciar` | `{"size":3}` | Iniciar partida 3x3 |
| POST | `/api/puzzle/mover` | `{"index":7}` | Mover casilla de índice 0 a N²-1 si es contigua al vacío |
| POST | `/api/puzzle/imagen` | Form-data `file` = JPG | Guardar la imagen en base de datos |

El formulario web consume estos endpoints con **JavaScript Fetch** sin recargar la página. El backend responde JSON.

## Estructura de los paquetes

```text
src/main/java/com/stfgames/puzzle/
  PuzzleApplication.java
  controller/PuzzleController.java  --> HTTP / REST
  service/PuzzleService.java       --> reglas, movimientos, validación
  repository/PuzzleRepository.java --> JdbcTemplate / MySQL (dos columnas)
src/main/resources/
  application.properties
  application-demo.properties
  application-mysql.properties
  schema-demo.sql
  static/index.html                --> interfaz web
  static/css/app.css               --> estilo
  static/js/app.js                 --> consumo AJAX/Fetch
  static/img/personaje-default.svg
CODIGO_LAB09.sql                   --> SQL para entregar
CODIGO_LAB09.txt                   --> solución del rompecabezas para entregar
```

## Regla de negocio y BD

- Se guarda en `stf_puzzle_sv` **exactamente una fila con dos columnas**: `stf_game_board_structure` y `image`.
- La columna `stf_game_board_structure` registra por ejemplo `41372586X`. `X` es la casilla vacía. La única secuencia fija de 8 movimientos está en `PuzzleService.SE​CUENCIA` (U,L,U,L,D,D,R,R).
- En tableros mayores se usa `n|1,2,...,X` para representar piezas de varios dígitos.
- El backend solo permite movimientos a una casilla contigua al vacío. Cada movimiento se actualiza mediante SQL `UPDATE`.
- Al ganar, el backend registra otra partida nueva; el frontend muestra **¡Juego terminado!** y enseguida abre la nueva partida.
- Solo se permite una partida compartida, coherente con que el laboratorio exige una única fila. No existe autenticación porque el enunciado del LAB09 no la solicita.

## Archivos de entrega

1. **`CODIGO_LAB09.sql`**: SQL de creación de base de datos y tabla.
2. **Proyecto en GitHub**: sube el proyecto completo (sin `/data` y sin contraseñas).
3. **`CODIGO_LAB09.txt`**: explicación y secuencia de solución de las 8 jugadas.

## Pruebas

Con Maven disponible:
```bash
mvn test
```

Las pruebas verifican la secuencia de 8 movimientos, la reanudación y que se rechace mover una pieza no contigua.
