import { localeActual } from "../i18n";

export function formatearFecha(valor?: string) {
  if (!valor) {
    return "—";
  }
  return new Date(valor).toLocaleString(localeActual(), {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatearHora(valor?: string) {
  if (!valor) {
    return "—";
  }
  return new Date(valor).toLocaleTimeString(localeActual(), {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

export function formatearDuracion(inicio?: string, fin?: string) {
  if (!inicio || !fin) {
    return "—";
  }
  const ms = Math.max(0, new Date(fin).getTime() - new Date(inicio).getTime());
  if (ms < 1000) {
    return `${ms} ms`;
  }
  const segundos = ms / 1000;
  if (segundos < 60) {
    return `${segundos.toFixed(1)} s`;
  }
  return `${Math.floor(segundos / 60)} min ${Math.round(segundos % 60)} s`;
}
