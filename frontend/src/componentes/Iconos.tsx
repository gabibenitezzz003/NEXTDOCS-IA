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

export function IconoMas(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M12 5v14" />
      <path d="M5 12h14" />
    </Base>
  );
}

export function IconoMenos(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M5 12h14" />
    </Base>
  );
}

export function IconoAjustar(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M15 3h6v6" />
      <path d="M9 21H3v-6" />
      <path d="M21 3l-7 7" />
      <path d="M3 21l7-7" />
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

export function IconoProceso(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="5" cy="12" r="2.2" />
      <circle cx="19" cy="6" r="2.2" />
      <circle cx="19" cy="18" r="2.2" />
      <path d="M7 11l9.7-4.3" />
      <path d="M7 13l9.7 4.3" />
    </Base>
  );
}

export function IconoTareas(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="m4 6 1.5 1.5L8 5" />
      <path d="M11 6.5h9" />
      <path d="m4 12.5 1.5 1.5L8 11.5" />
      <path d="M11 13h9" />
      <path d="m4 19 1.5 1.5L8 18" />
      <path d="M11 19.5h9" />
    </Base>
  );
}

export function IconoOperacion(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="m10 8.5 5.5 3.5-5.5 3.5z" />
    </Base>
  );
}

export function IconoSupervisora(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
      <circle cx="12" cy="12" r="3" />
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

export function IconoSol(props: PropsIcono) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </Base>
  );
}

export function IconoLuna(props: PropsIcono) {
  return (
    <Base {...props}>
      <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />
    </Base>
  );
}


export function IconoGoogle({ tamano = 18, ...resto }: PropsIcono) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      aria-hidden
      {...resto}
    >
      <path
        fill="#4285F4"
        d="M23.49 12.27c0-.79-.07-1.54-.19-2.27H12v4.51h6.47c-.29 1.48-1.14 2.73-2.4 3.58v3h3.86c2.26-2.09 3.56-5.17 3.56-8.82z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.86-3c-1.08.72-2.45 1.16-4.07 1.16-3.13 0-5.78-2.11-6.73-4.96H1.29v3.09C3.26 21.3 7.31 24 12 24z"
      />
      <path
        fill="#FBBC05"
        d="M5.27 14.29c-.25-.72-.38-1.49-.38-2.29s.14-1.57.38-2.29V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.98-3.09z"
      />
      <path
        fill="#EA4335"
        d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.98 3.09C6.22 6.86 8.87 4.75 12 4.75z"
      />
    </svg>
  );
}

export function IconoMicrosoft({ tamano = 18, ...resto }: PropsIcono) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      aria-hidden
      {...resto}
    >
      <rect x="1" y="1" width="10.5" height="10.5" fill="#F25022" />
      <rect x="12.5" y="1" width="10.5" height="10.5" fill="#7FBA00" />
      <rect x="1" y="12.5" width="10.5" height="10.5" fill="#00A4EF" />
      <rect x="12.5" y="12.5" width="10.5" height="10.5" fill="#FFB900" />
    </svg>
  );
}
