import type { SVGProps } from "react";

type PropsIcono = SVGProps<SVGSVGElement> & { tamano?: number };

function Base({ tamano = 18, children, ...resto }: PropsIcono) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      {...resto}
    >
      {children}
    </svg>
  );
}

export function IconoResumen(props: PropsIcono) {
  return (
    <Base {...props}>
      <rect x="3" y="3" width="7.5" height="8.5" rx="2" />
      <rect x="13.5" y="3" width="7.5" height="5" rx="2" />
      <rect x="13.5" y="11" width="7.5" height="10" rx="2" />
      <rect x="3" y="14.5" width="7.5" height="6.5" rx="2" />
    </Base>
  );
}

export function IconoPanel(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M4 20V13" />
      <path d="M10 20V5" />
      <path d="M16 20v-5" />
      <path d="M22 20H2" />
    </Base>
  );
}

export function IconoDocumentos(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6" />
      <path d="M9 17h4" />
    </Base>
  );
}

export function IconoExcepciones(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M10.3 4.3 2.6 17.4A1.9 1.9 0 0 0 4.3 20.3h15.4a1.9 1.9 0 0 0 1.7-2.9L13.7 4.3a1.9 1.9 0 0 0-3.4 0z" />
      <path d="M12 9.5v4" />
      <path d="M12 17h.01" />
    </Base>
  );
}

export function IconoPlantillas(props: PropsIcono) {
  return (
    <Base {...props}>
      <rect x="3" y="3" width="18" height="18" rx="2.5" />
      <path d="M3 9h18" />
      <path d="M9 21V9" />
    </Base>
  );
}

export function IconoBuscar(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.6-3.6" />
    </Base>
  );
}

export function IconoSubir(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 16V4" />
      <path d="m7 9 5-5 5 5" />
      <path d="M4 16v2.5A1.5 1.5 0 0 0 5.5 20h13a1.5 1.5 0 0 0 1.5-1.5V16" />
    </Base>
  );
}

export function IconoSalir(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M14 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h8" />
      <path d="m17 15 4-3-4-3" />
      <path d="M21 12H10" />
    </Base>
  );
}

export function IconoCerrar(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="m6 6 12 12" />
      <path d="M18 6 6 18" />
    </Base>
  );
}

export function IconoFlechaArriba(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 19V5" />
      <path d="m6 11 6-6 6 6" />
    </Base>
  );
}

export function IconoFlechaAbajo(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 5v14" />
      <path d="m6 13 6 6 6-6" />
    </Base>
  );
}

export function IconoIgual(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M5 10h14" />
      <path d="M5 14h14" />
    </Base>
  );
}

export function IconoDerecha(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="m9 5 7 7-7 7" />
    </Base>
  );
}

export function IconoIzquierda(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="m15 5-7 7 7 7" />
    </Base>
  );
}

export function IconoRecargar(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M20 11a8 8 0 0 0-13.7-5.3L3 9" />
      <path d="M3 4v5h5" />
      <path d="M4 13a8 8 0 0 0 13.7 5.3L21 15" />
      <path d="M21 20v-5h-5" />
    </Base>
  );
}

export function IconoDescargar(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 4v12" />
      <path d="m7 11 5 5 5-5" />
      <path d="M4 20h16" />
    </Base>
  );
}

export function IconoFiltro(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M3 5h18" />
      <path d="M6.5 12h11" />
      <path d="M10 19h4" />
    </Base>
  );
}

export function IconoInfo(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5" />
      <path d="M12 8h.01" />
    </Base>
  );
}

export function IconoVacio(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M4 8.5 12 4l8 4.5v7L12 20l-8-4.5z" />
      <path d="M4 8.5 12 13l8-4.5" />
      <path d="M12 13v7" />
    </Base>
  );
}

export function IconoCheck(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="m5 12.5 4.5 4.5L19 7" />
    </Base>
  );
}

export function IconoReloj(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5.2l3.2 2" />
    </Base>
  );
}

export function IconoCandado(props: PropsIcono) {
  return (
    <Base {...props}>
      <rect x="4" y="10" width="16" height="11" rx="2.5" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </Base>
  );
}

export function IconoCorreo(props: PropsIcono) {
  return (
    <Base {...props}>
      <rect x="2.5" y="4.5" width="19" height="15" rx="2.5" />
      <path d="M3.5 7 11 12.4a1.8 1.8 0 0 0 2 0L20.5 7" />
    </Base>
  );
}

export function IconoAdjunto(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M20 11.5 12.4 19a4.5 4.5 0 0 1-6.4-6.4l7.8-7.7a3 3 0 0 1 4.2 4.2l-7.7 7.8a1.5 1.5 0 0 1-2.2-2.1l7.1-7.1" />
    </Base>
  );
}

export function IconoEnlace(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M10 13.5a3.5 3.5 0 0 0 5 0l3-3a3.5 3.5 0 0 0-5-5l-1.5 1.5" />
      <path d="M14 10.5a3.5 3.5 0 0 0-5 0l-3 3a3.5 3.5 0 0 0 5 5L12.5 17" />
    </Base>
  );
}

export function IconoEscudo(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 3 5 6v5.5c0 4.2 2.9 7.8 7 9.5 4.1-1.7 7-5.3 7-9.5V6l-7-3Z" />
      <path d="m9.2 12 2 2 3.6-3.8" />
    </Base>
  );
}

export function IconoMas(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 5.5v13M5.5 12h13" />
    </Base>
  );
}

export function IconoWhatsapp(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M3.5 20.5 4.9 16A8.2 8.2 0 1 1 8 19.1l-4.5 1.4Z" />
      <path d="M9 9.2c.2 1 .7 2 1.5 2.8.8.8 1.8 1.3 2.8 1.5l.9-1.1 1.9.8-.3 1.4c-1.7.4-3.6-.3-5.1-1.8-1.5-1.5-2.2-3.4-1.8-5.1l1.4-.3.8 1.9-1.1.9Z" />
    </Base>
  );
}

export function IconoCopiar(props: PropsIcono) {
  return (
    <Base {...props}>
      <rect x="9" y="9" width="11" height="11" rx="2" />
      <path d="M15 6.5V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h1.5" />
    </Base>
  );
}
