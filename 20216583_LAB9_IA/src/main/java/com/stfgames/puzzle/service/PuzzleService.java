package com.stfgames.puzzle.service;

import com.stfgames.puzzle.repository.PuzzleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class PuzzleService {
    private final PuzzleRepository repo;

    // Una sola secuencia de ocho movimientos, aplicada al espacio vacío.
    // Al partir del tablero ordenado, el resultado SIEMPRE es solucionable.
    private static final String[] SECUENCIA = {"U", "L", "U", "L", "D", "D", "R", "R"};

    public PuzzleService(PuzzleRepository repo) {
        this.repo = repo;
    }

    public synchronized Map<String, Object> obtenerEstado() {
        PuzzleRepository.JuegoGuardado registro = repo.obtenerJuego();
        int[] tablero = deserializar(registro.tablero());
        int n = tablero == null ? 0 : (int) Math.sqrt(tablero.length);
        Map<String, Object> respuesta = crearRespuesta(tablero, n, false);
        respuesta.put("image", registro.imagen());
        return respuesta;
    }

    public synchronized Map<String, Object> iniciar(Integer dimension) {
        if (dimension == null || dimension < 3 || dimension > 9) {
            throw error("Ingrese una dimensión entre 3x3 y 9x9.");
        }
        int[] tablero = generarDesordenado(dimension);
        repo.guardarTablero(serializar(tablero, dimension));
        return crearRespuesta(tablero, dimension, false);
    }

    public synchronized Map<String, Object> mover(Integer indice) {
        if (indice == null) throw error("Debe indicar la casilla seleccionada.");
        int[] tablero = deserializar(repo.obtenerJuego().tablero());
        if (tablero == null) throw error("Primero debe iniciar el rompecabezas.");
        int n = (int) Math.sqrt(tablero.length);
        if (indice < 0 || indice >= tablero.length) throw error("La casilla no existe.");
        int vacio = posicionVacia(tablero);
        if (!sonContiguos(indice, vacio, n)) {
            throw error("Solo se pueden mover casillas contiguas al espacio vacío.");
        }
        intercambiar(tablero, indice, vacio);
        if (estaResuelto(tablero)) {
            // Cumple el requisito: una nueva partida en frontend y base de datos.
            int[] nuevo = generarDesordenado(n);
            repo.guardarTablero(serializar(nuevo, n));
            Map<String, Object> respuesta = crearRespuesta(nuevo, n, true);
            respuesta.put("message", "¡Juego terminado!");
            return respuesta;
        }
        repo.guardarTablero(serializar(tablero, n));
        return crearRespuesta(tablero, n, false);
    }

    public synchronized Map<String, Object> subirImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) throw error("Debe seleccionar una imagen JPG.");
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
            throw error("Solo se permite el formato .jpg");
        }
        if (archivo.getSize() > 5 * 1024 * 1024) throw error("La imagen supera el límite de 5 MB.");
        byte[] bytes = archivo.getBytes();
        if (bytes.length < 3 || (bytes[0] & 0xff) != 0xff ||
            (bytes[1] & 0xff) != 0xd8 || (bytes[2] & 0xff) != 0xff) {
            throw error("El contenido no corresponde a un JPG válido.");
        }
        BufferedImage origen = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (origen == null) throw error("No se pudo leer la imagen JPG.");
        // Recortar al centro para mantener las proporciones cuadradas del tablero.
        int lado = Math.min(origen.getWidth(), origen.getHeight());
        int x = (origen.getWidth() - lado) / 2;
        int y = (origen.getHeight() - lado) / 2;
        BufferedImage destino = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(origen, 0, 0, 600, 600, x, y, x + lado, y + lado, null);
        g.dispose();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(destino, "jpg", buffer)) throw error("No se pudo procesar la imagen JPG.");
        String imagenBase64 = Base64.getEncoder().encodeToString(buffer.toByteArray());
        repo.guardarImagen(imagenBase64);
        return Map.of("ok", true, "image", imagenBase64);
    }

    private int[] generarDesordenado(int n) {
        int[] tablero = new int[n * n];
        for (int i = 0; i < tablero.length - 1; i++) tablero[i] = i + 1;
        tablero[tablero.length - 1] = 0;
        int vacio = tablero.length - 1;
        for (String paso : SECUENCIA) {
            int fila = vacio / n;
            int columna = vacio % n;
            switch (paso) {
                case "U" -> fila--;
                case "D" -> fila++;
                case "L" -> columna--;
                case "R" -> columna++;
                default -> throw new IllegalStateException("Paso desconocido: " + paso);
            }
            if (fila < 0 || fila >= n || columna < 0 || columna >= n)
                throw new IllegalStateException("La secuencia produce un paso inválido.");
            int nueva = fila * n + columna;
            intercambiar(tablero, vacio, nueva);
            vacio = nueva;
        }
        return tablero;
    }

    private boolean sonContiguos(int a, int b, int n) {
        return Math.abs(a / n - b / n) + Math.abs(a % n - b % n) == 1;
    }

    private void intercambiar(int[] tablero, int a, int b) {
        int tmp = tablero[a];
        tablero[a] = tablero[b];
        tablero[b] = tmp;
    }

    private int posicionVacia(int[] tablero) {
        for (int i = 0; i < tablero.length; i++) if (tablero[i] == 0) return i;
        throw new IllegalStateException("La partida guardada no tiene casilla vacía.");
    }

    private boolean estaResuelto(int[] tablero) {
        for (int i = 0; i < tablero.length - 1; i++) if (tablero[i] != i + 1) return false;
        return tablero[tablero.length - 1] == 0;
    }

    private String serializar(int[] tablero, int n) {
        if (n == 3) {
            StringBuilder cadena = new StringBuilder();
            for (int v : tablero) cadena.append(v == 0 ? "X" : Integer.toString(v));
            return cadena.toString();
        }
        StringJoiner joiner = new StringJoiner(",");
        for (int v : tablero) joiner.add(v == 0 ? "X" : Integer.toString(v));
        return n + "|" + joiner;
    }

    private int[] deserializar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            int n;
            int[] tablero;
            if (texto.contains("|")) {
                String[] partes = texto.split("\\|", 2);
                n = Integer.parseInt(partes[0]);
                String[] valores = partes[1].split(",");
                tablero = new int[valores.length];
                for (int i = 0; i < valores.length; i++) {
                    tablero[i] = valores[i].equals("X") ? 0 : Integer.parseInt(valores[i]);
                }
            } else {
                n = 3;
                tablero = new int[texto.length()];
                for (int i = 0; i < texto.length(); i++) {
                    char ch = texto.charAt(i);
                    tablero[i] = ch == 'X' ? 0 : Character.digit(ch, 10);
                }
            }
            if (n < 3 || n > 9 || tablero.length != n * n) throw new IllegalArgumentException();
            boolean[] vistos = new boolean[tablero.length];
            for (int v : tablero) {
                if (v < 0 || v >= tablero.length || vistos[v]) throw new IllegalArgumentException();
                vistos[v] = true;
            }
            return tablero;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Estado del rompecabezas inválido en MySQL. Reinicie la fila si es necesario.", e);
        }
    }

    private Map<String, Object> crearRespuesta(int[] tablero, int n, boolean terminado) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("started", tablero != null);
        respuesta.put("size", n);
        respuesta.put("finished", terminado);
        respuesta.put("board", tablero == null ? List.of() : Arrays.stream(tablero).boxed().toList());
        return respuesta;
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
