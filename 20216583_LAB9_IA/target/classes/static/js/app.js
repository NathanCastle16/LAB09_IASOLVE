'use strict';

// Estas solicitudes HTTP son consumo de Web Services REST (Fetch API / AJAX).
const $ = id => document.getElementById(id);
const preview = $('preview');
const archivo = $('archivo');
const dimension = $('dimension');
const color = $('color');
const iniciar = $('iniciar');
const tablero = $('tablero');
const zonaJuego = $('zonaJuego');
const mensaje = $('mensaje');
const aviso = $('aviso');

let imagenActual = '/img/personaje-default.svg';
let estadoActual = [];
let dimensionActual = 0;
let bloqueado = false;

function validarDimension() {
  const valido = /^([3-9])x\1$/.test(dimension.value.trim().toLowerCase());
  iniciar.hidden = !valido;
  return valido;
}

function mostrarAviso(texto, esError = false) {
  aviso.textContent = texto;
  aviso.className = esError ? 'error' : '';
}

async function api(ruta, opciones = {}) {
  const respuesta = await fetch('/api/puzzle' + ruta, opciones);
  if (!respuesta.ok) {
    let detalle = `Error HTTP ${respuesta.status}`;
    try {
      const body = await respuesta.json();
      detalle = body.message || body.detail || detalle;
    } catch (_) { /* se conserva el mensaje HTTP */ }
    throw new Error(detalle);
  }
  return respuesta.json();
}

function jsonPost(ruta, datos) {
  return api(ruta, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos)
  });
}

function sonContiguos(a, b, n) {
  return Math.abs(Math.floor(a / n) - Math.floor(b / n))
    + Math.abs(a % n - b % n) === 1;
}

function dibujar(piezas, n) {
  estadoActual = [...piezas];
  dimensionActual = n;
  zonaJuego.hidden = false;
  tablero.replaceChildren();
  tablero.style.gridTemplateColumns = `repeat(${n},minmax(0,1fr))`;
  tablero.style.gridTemplateRows = `repeat(${n},minmax(0,1fr))`;
  const vacio = piezas.indexOf(0);

  piezas.forEach((valor, indice) => {
    const pieza = document.createElement('div');
    pieza.className = 'pieza';
    if (valor === 0) {
      pieza.classList.add('vacia');
      pieza.setAttribute('aria-label', 'Espacio vacío');
    } else {
      // Una sola imagen: mostramos el fragmento que corresponde a su posición ORIGINAL.
      const original = valor - 1;
      const fila = Math.floor(original / n);
      const col = original % n;
      pieza.style.backgroundImage = `url("${imagenActual}")`;
      pieza.style.backgroundSize = `${n * 100}% ${n * 100}%`;
      pieza.style.backgroundPosition = `${col * 100 / (n - 1)}% ${fila * 100 / (n - 1)}%`;
      pieza.setAttribute('aria-label', `Pieza ${valor}`);
      if (sonContiguos(indice, vacio, n)) {
        pieza.classList.add('puede-mover');
        pieza.setAttribute('role', 'button');
        pieza.tabIndex = 0;
        pieza.addEventListener('click', () => mover(indice));
        pieza.addEventListener('keydown', e => {
          if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); mover(indice); }
        });
      }
    }
    tablero.appendChild(pieza);
  });
}

async function mover(indice) {
  if (bloqueado) return;
  bloqueado = true;
  try {
    const resultado = await jsonPost('/mover', { index: indice });
    if (resultado.finished) {
      // Mostrar la solución durante unos instantes y luego el nuevo tablero.
      const n = resultado.size;
      const final = Array.from({ length: n*n }, (_, i) => i + 1);
      final[final.length - 1] = 0;
      dibujar(final, n);
      mensaje.textContent = '¡Juego terminado!';
      mostrarAviso('¡Felicidades! Se creó automáticamente otra partida.');
      setTimeout(() => {
        dibujar(resultado.board, n);
        mensaje.textContent = 'Nuevo juego iniciado';
        bloqueado = false;
      }, 1800);
    } else {
      dibujar(resultado.board, resultado.size);
      mensaje.textContent = '';
      bloqueado = false;
    }
  } catch (e) {
    mostrarAviso(e.message, true);
    bloqueado = false;
  }
}

$('seleccionar').addEventListener('click', () => archivo.click());

archivo.addEventListener('change', async () => {
  const foto = archivo.files[0];
  if (!foto) return;
  if (!foto.name.toLowerCase().endsWith('.jpg')) {
    mostrarAviso('Elige un archivo con extensión .jpg', true);
    archivo.value = '';
    return;
  }
  if (foto.size > 5 * 1024 * 1024) {
    mostrarAviso('La imagen no puede superar 5 MB.', true);
    archivo.value = '';
    return;
  }
  const datos = new FormData();
  datos.append('file', foto);
  try {
    const resultado = await api('/imagen', { method: 'POST', body: datos });
    imagenActual = 'data:image/jpeg;base64,' + resultado.image;
    preview.src = imagenActual;
    mostrarAviso('Imagen JPG subida y guardada en la base de datos.');
    if (estadoActual.length) dibujar(estadoActual, dimensionActual);
  } catch (e) {
    mostrarAviso('No se pudo subir la imagen: ' + e.message, true);
  }
});

dimension.addEventListener('input', validarDimension);
color.addEventListener('input', () => {
  document.documentElement.style.setProperty('--table-color', color.value);
  localStorage.setItem('stf-color-tablero', color.value);
});

iniciar.addEventListener('click', async () => {
  if (bloqueado || !validarDimension()) return;
  bloqueado = true;
  try {
    const n = Number(dimension.value.trim().charAt(0));
    const resultado = await jsonPost('/iniciar', { size: n });
    dibujar(resultado.board, resultado.size);
    mensaje.textContent = '';
    mostrarAviso('Partida nueva guardada en la base de datos.');
  } catch (e) {
    mostrarAviso(e.message, true);
  } finally {
    bloqueado = false;
  }
});

async function recuperar() {
  const colorGuardado = localStorage.getItem('stf-color-tablero');
  if (colorGuardado && /^#[0-9a-fA-F]{6}$/.test(colorGuardado)) {
    color.value = colorGuardado;
  }
  document.documentElement.style.setProperty('--table-color', color.value);
  validarDimension();
  try {
    const resultado = await api('/estado');
    if (resultado.image) {
      imagenActual = 'data:image/jpeg;base64,' + resultado.image;
      preview.src = imagenActual;
    }
    if (resultado.started) {
      dimension.value = `${resultado.size}x${resultado.size}`;
      validarDimension();
      dibujar(resultado.board, resultado.size);
      mostrarAviso('Partida recuperada automáticamente desde la base de datos.');
    }
  } catch (e) {
    mostrarAviso('No se pudo conectar al servicio REST: ' + e.message, true);
  }
}

recuperar();
