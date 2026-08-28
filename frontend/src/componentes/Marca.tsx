export function Isotipo({ tamano = 28 }: { tamano?: number }) {
  return (
    <svg width={tamano} height={tamano} viewBox="0 0 48 48" fill="none" aria-hidden>
      <defs>
        <linearGradient id="degradadoMarca" x1="0" y1="0" x2="48" y2="48">
          <stop offset="0%" stopColor="#6C3BFF" />
          <stop offset="100%" stopColor="#FF1E1E" />
        </linearGradient>
      </defs>
      <path d="M22 4h12l10 10v30H22z" fill="currentColor" />
      <path d="M34 4v10h10z" fill="#FFFFFF" fillOpacity="0.28" />
      <path
        d="M2 16h16l-6 6h-10zM2 24h22l-6 6H2zM2 32h14l-6 6H2z"
        fill="url(#degradadoMarca)"
      />
    </svg>
  );
}

export function Logotipo({ claro = false }: { claro?: boolean }) {
  return (
    <div className="flex items-center gap-2.5">
      <span className={claro ? "text-white" : "text-grafito"}>
        <Isotipo />
      </span>
      <span className="font-titulo text-lg leading-none tracking-tight">
        <span className={claro ? "text-white" : "text-grafito"}>NEXT</span>{" "}
        <span className={claro ? "text-white/70" : "text-tinta-suave"}>DOC</span>{" "}
        <span className="text-rojo">AI</span>
      </span>
    </div>
  );
}
