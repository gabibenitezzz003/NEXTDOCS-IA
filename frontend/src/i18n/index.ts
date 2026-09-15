import { es } from "./es";
import { en } from "./en";
import { pt } from "./pt";

export type Diccionario = typeof es;

export type Idioma = "es" | "en" | "pt";

export const IDIOMAS: Idioma[] = ["es", "en", "pt"];

export const NOMBRES_IDIOMA: Record<Idioma, string> = {
  es: "Español",
  en: "English",
  pt: "Português",
};

const DICCIONARIOS: Record<Idioma, Diccionario> = { es, en, pt };

const CLAVE_IDIOMA = "nextdocs.idioma";

let idiomaActual: Idioma = idiomaInicial();

function idiomaInicial(): Idioma {
  const guardado = localStorage.getItem(CLAVE_IDIOMA);
  if (esIdioma(guardado)) return guardado;
  const navegador = navigator.language?.slice(0, 2);
  return esIdioma(navegador) ? navegador : "es";
}

export function esIdioma(valor: string | null | undefined): valor is Idioma {
  return valor === "es" || valor === "en" || valor === "pt";
}

export function idiomaVigente(): Idioma {
  return idiomaActual;
}

export const LOCALES: Record<Idioma, string> = {
  es: "es-AR",
  en: "en-US",
  pt: "pt-BR",
};

export function localeActual(): string {
  return LOCALES[idiomaActual];
}

export function formatearNumero(valor: number): string {
  return valor.toLocaleString(localeActual());
}

export function fijarIdiomaActual(idioma: Idioma) {
  idiomaActual = idioma;
  localStorage.setItem(CLAVE_IDIOMA, idioma);
}

function buscar(diccionario: Diccionario, ruta: string): string | undefined {
  let nodo: unknown = diccionario;
  for (const parte of ruta.split(".")) {
    if (typeof nodo !== "object" || nodo === null) return undefined;
    nodo = (nodo as Record<string, unknown>)[parte];
  }
  return typeof nodo === "string" ? nodo : undefined;
}

export function traducir(
  ruta: string,
  params?: Record<string, string | number>,
  idioma: Idioma = idiomaActual,
): string {
  const texto = buscar(DICCIONARIOS[idioma], ruta) ?? buscar(es, ruta) ?? ruta;
  if (!params) return texto;
  return texto.replace(/\{(\w+)\}/g, (marca, clave) =>
    params[clave] === undefined ? marca : String(params[clave]),
  );
}
