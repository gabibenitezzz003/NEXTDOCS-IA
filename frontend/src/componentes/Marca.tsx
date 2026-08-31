export function Isotipo({ tamano = 32, animado = false }: { tamano?: number; animado?: boolean }) {
  return (
    <svg width={tamano} height={tamano} viewBox="0 0 40 40" fill="none" aria-hidden>
      <defs>
        <linearGradient id="marcaFondo" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#8A63FF" />
          <stop offset="46%" stopColor="#6C38FF" />
          <stop offset="100%" stopColor="#FF1E1E" />
        </linearGradient>
        <linearGradient id="marcaBrillo" x1="0" y1="0" x2="0" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.34" />
          <stop offset="55%" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
      </defs>

      <rect width="40" height="40" rx="11" fill="url(#marcaFondo)" />
      <rect width="40" height="40" rx="11" fill="url(#marcaBrillo)" />
      <rect x="0.6" y="0.6" width="38.8" height="38.8" rx="10.4" stroke="#FFFFFF" strokeOpacity="0.22" strokeWidth="1.2" />

      <g fill="#FFFFFF">
        <rect x="10" y="11" width="13" height="2.6" rx="1.3" fillOpacity="0.95" />
        <rect x="10" y="18.7" width="9" height="2.6" rx="1.3" fillOpacity="0.72" />
        <rect x="10" y="26.4" width="6" height="2.6" rx="1.3" fillOpacity="0.5" />
      </g>

      <path
        d="M23.2 15.4 29.6 20l-6.4 4.6"
        stroke="#FFFFFF"
        strokeWidth="2.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {animado ? (
          <animate
            attributeName="opacity"
            values="0.45;1;0.45"
            dur="2.6s"
            repeatCount="indefinite"
          />
        ) : null}
      </path>
    </svg>
  );
}

export function Logotipo({
  claro = false,
  tamano = 32,
  escala = 1,
}: {
  claro?: boolean;
  tamano?: number;
  escala?: number;
}) {
  return (
    <div className="flex items-center" style={{ gap: 11 * escala }}>
      <Isotipo tamano={tamano} />
      <span className="flex items-baseline" style={{ gap: 6 * escala }}>
        <span
          className={`font-titulo font-bold leading-none tracking-[-0.035em] ${
            claro ? "text-white" : "text-grafito"
          }`}
          style={{ fontSize: 22 * escala }}
        >
          NEXT
        </span>
        <span
          className="font-titulo font-bold uppercase leading-none text-rojo"
          style={{ fontSize: 11.5 * escala, letterSpacing: 0.06 * escala + "em" }}
        >
          DOC AI
        </span>
      </span>
    </div>
  );
}
