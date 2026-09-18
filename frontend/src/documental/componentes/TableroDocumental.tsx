import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  obtenerBandeja,
  obtenerEventos,
  obtenerExcepcionesMotor,
  obtenerResumenDocumental,
  type DocumentoMotor,
  type EstadoDocumentoMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import { Tarjeta, CabeceraTarjeta } from "../../componentes/Interfaz";
import { Cargando, ErrorPanel } from "../../componentes/Estados";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { comoFecha, motivoHumano } from "../dominio";

const COLORES_ESTADO: Record<EstadoDocumentoMotor, string> = {
  RECIBIDO: "#98a2b3",
  PROCESANDO: "#0ea5e9",
  EXTRAIDO: "#7b4dff",
  VALIDADO: "#8b5cf6",
  OBSERVADO: "#f59e0b",
  APROBADO: "#10b981",
  RECHAZADO: "#ef4444",
  DIVIDIDO: "#0ea5e9",
  ELIMINADO: "#98a2b3",
  CERRADO: "#475569",
};

const ORDEN_ESTADOS: EstadoDocumentoMotor[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
  "OBSERVADO",
  "APROBADO",
  "RECHAZADO",
  "DIVIDIDO",
  "CERRADO",
];

const ESTILO_TOOLTIP = {
  backgroundColor: "var(--color-superficie, #ffffff)",
  border: "1px solid var(--color-borde, #e2e7ef)",
  borderRadius: "10px",
  fontSize: "12px",
  boxShadow: "0 8px 24px rgba(13,15,18,.12)",
};

function TarjetaKpi({
  etiqueta,
  valor,
  detalle,
  tono,
  gradiente,
}: {
  etiqueta: string;
  valor: string | number;
  detalle: string;
  tono: string;
  gradiente: string;
}) {
  return (
    <div
      className="relative overflow-hidden rounded-tarjeta border border-borde p-espacio-4"
      style={{ background: gradiente }}
    >
      <div
        className="absolute -right-6 -top-6 size-24 rounded-full opacity-20"
        style={{ background: tono }}
      />
      <p className="text-micro font-bold uppercase tracking-wider opacity-80"
         style={{ color: tono }}>
        {etiqueta}
      </p>
      <p className="mt-espacio-2 text-4xl font-bold tracking-tight"
         style={{ color: tono }}>
        {valor}
      </p>
      <p className="mt-espacio-1 text-micro font-medium opacity-70"
         style={{ color: tono }}>
        {detalle}
      </p>
    </div>
  );
}

function DistribucionEstados({ porEstado }: { porEstado: Record<string, number> }) {
  const { t } = useIdioma();

  const datos = ORDEN_ESTADOS.map((estado) => ({
    nombre: t(`documental.estado.${estado}`),
    valor: Number(porEstado[estado] || 0),
    color: COLORES_ESTADO[estado],
  })).filter((item) => item.valor > 0);

  const total = datos.reduce((suma, item) => suma + item.valor, 0);

  if (!total) {
    return (
      <p className="py-espacio-6 text-center text-pequeno text-tinta-suave">
        {t("documental.tablero.sinDatos")}
      </p>
    );
  }

  return (
    <div className="flex flex-col items-center gap-espacio-4 lg:flex-row">
      <div className="relative h-56 w-56 shrink-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={datos}
              dataKey="valor"
              nameKey="nombre"
              cx="50%"
              cy="50%"
              innerRadius={62}
              outerRadius={95}
              paddingAngle={3}
              cornerRadius={5}
              strokeWidth={0}
              animationBegin={100}
              animationDuration={900}
            >
              {datos.map((item) => (
                <Cell key={item.nombre} fill={item.color} />
              ))}
            </Pie>
            <Tooltip contentStyle={ESTILO_TOOLTIP} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 grid place-items-center">
          <div className="text-center">
            <p className="text-3xl font-bold text-tinta">{total}</p>
            <p className="text-micro font-semibold uppercase tracking-wider text-tinta-suave">
              {t("documental.kpi.total")}
            </p>
          </div>
        </div>
      </div>
      <div className="grid w-full grid-cols-2 gap-x-espacio-4 gap-y-espacio-2">
        {datos.map((item) => (
          <div key={item.nombre} className="flex items-center gap-espacio-2">
            <span
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: item.color }}
            />
            <span className="min-w-0 flex-1 truncate text-micro text-tinta-suave">
              {item.nombre}
            </span>
            <span className="text-pequeno font-bold text-tinta">
              {item.valor}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function VolumenPorDia({ documentos }: { documentos: DocumentoMotor[] }) {
  const { t, idioma } = useIdioma();

  const datos = useMemo(() => {
    const porDia = new Map<string, number>();
    documentos.forEach((documento) => {
      const dia = documento.creado_en.slice(0, 10);
      porDia.set(dia, (porDia.get(dia) || 0) + 1);
    });
    return [...porDia.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .slice(-14)
      .map(([dia, cantidad]) => ({
        dia: new Date(`${dia}T12:00:00`).toLocaleDateString(
          idioma === "en" ? "en-US" : idioma === "pt" ? "pt-BR" : "es-AR",
          { day: "numeric", month: "short" },
        ),
        cantidad,
      }));
  }, [documentos, idioma]);

  if (datos.length < 2) {
    return (
      <p className="py-espacio-6 text-center text-pequeno text-tinta-suave">
        {t("documental.tablero.sinDatos")}
      </p>
    );
  }

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={datos} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
          <defs>
            <linearGradient id="gradVolumen" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#6c36ff" stopOpacity={0.35} />
              <stop offset="100%" stopColor="#6c36ff" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-borde, #e2e7ef)" vertical={false} />
          <XAxis
            dataKey="dia"
            tick={{ fontSize: 11, fill: "var(--color-tinta-suave, #667085)" }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 11, fill: "var(--color-tinta-suave, #667085)" }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip contentStyle={ESTILO_TOOLTIP} />
          <Area
            type="monotone"
            dataKey="cantidad"
            name={t("documental.kpi.total")}
            stroke="#6c36ff"
            strokeWidth={2.5}
            fill="url(#gradVolumen)"
            animationDuration={1000}
            dot={{ r: 3, fill: "#6c36ff", strokeWidth: 0 }}
            activeDot={{ r: 5, fill: "#6c36ff" }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

function ConfianzaPromedio({ documentos }: { documentos: DocumentoMotor[] }) {
  const { t } = useIdioma();

  const datos = useMemo(() => {
    const porTipo = new Map<string, { total: number; cantidad: number }>();
    documentos.forEach((documento) => {
      if (documento.confianza === null) return;
      const tipo = documento.plantilla_codigo || "—";
      const acc = porTipo.get(tipo) || { total: 0, cantidad: 0 };
      acc.total += documento.confianza;
      acc.cantidad += 1;
      porTipo.set(tipo, acc);
    });
    return [...porTipo.entries()]
      .map(([tipo, acc]) => ({
        tipo,
        confianza: Math.round((acc.total / acc.cantidad) * 100),
      }))
      .sort((a, b) => b.confianza - a.confianza)
      .slice(0, 8);
  }, [documentos]);

  if (!datos.length) {
    return (
      <p className="py-espacio-6 text-center text-pequeno text-tinta-suave">
        {t("documental.tablero.sinDatos")}
      </p>
    );
  }

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={datos}
          layout="vertical"
          margin={{ top: 4, right: 12, left: 8, bottom: 0 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-borde, #e2e7ef)" horizontal={false} />
          <XAxis
            type="number"
            domain={[0, 100]}
            tick={{ fontSize: 11, fill: "var(--color-tinta-suave, #667085)" }}
            axisLine={false}
            tickLine={false}
            unit="%"
          />
          <YAxis
            type="category"
            dataKey="tipo"
            width={110}
            tick={{ fontSize: 11, fill: "var(--color-tinta, #111827)" }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            contentStyle={ESTILO_TOOLTIP}
            formatter={(valor) => [`${valor}%`, t("documental.bandeja.colConfianza")]}
          />
          <Bar
            dataKey="confianza"
            radius={[0, 6, 6, 0]}
            animationDuration={900}
            barSize={16}
          >
            {datos.map((item) => (
              <Cell
                key={item.tipo}
                fill={
                  item.confianza >= 80
                    ? "#10b981"
                    : item.confianza >= 50
                      ? "#f59e0b"
                      : "#ef4444"
                }
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

function TopMotivos() {
  const { t } = useIdioma();
  const excepciones = useQuery({
    queryKey: ["documental-excepciones", { estado: "ABIERTA", limite: 200 }],
    queryFn: () => obtenerExcepcionesMotor({ estado: "ABIERTA", limite: 200 }),
    staleTime: 15000,
    refetchInterval: 30000,
  });

  const datos = useMemo(() => {
    const acumulado = new Map<string, number>();
    (excepciones.data || []).forEach((excepcion) => {
      acumulado.set(
        excepcion.codigo_motivo,
        (acumulado.get(excepcion.codigo_motivo) || 0) + 1,
      );
    });
    return [...acumulado.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([motivo, cantidad]) => ({
        motivo: motivoHumano(motivo, t),
        cantidad,
      }));
  }, [excepciones.data, t]);

  if (!datos.length) {
    return (
      <p className="py-espacio-6 text-center text-pequeno text-tinta-suave">
        {t("documental.tablero.sinExcepciones")}
      </p>
    );
  }

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={datos} margin={{ top: 8, right: 8, left: -18, bottom: 40 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-borde, #e2e7ef)" vertical={false} />
          <XAxis
            dataKey="motivo"
            interval={0}
            angle={-25}
            textAnchor="end"
            height={60}
            tick={{ fontSize: 10, fill: "var(--color-tinta-suave, #667085)" }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 11, fill: "var(--color-tinta-suave, #667085)" }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip contentStyle={ESTILO_TOOLTIP} />
          <Bar dataKey="cantidad" radius={[6, 6, 0, 0]} animationDuration={900} barSize={30}>
            {datos.map((item, indice) => (
              <Cell
                key={item.motivo}
                fill={["#f59e0b", "#6c36ff", "#0ea5e9", "#ef4444", "#10b981", "#8b5cf6"][indice % 6]}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function TableroDocumental() {
  const { t, idioma } = useIdioma();

  const resumen = useQuery({
    queryKey: ["documental-resumen"],
    queryFn: obtenerResumenDocumental,
    staleTime: 15000,
    refetchInterval: 30000,
  });

  const bandeja = useQuery({
    queryKey: ["documental-bandeja-tablero"],
    queryFn: () => obtenerBandeja({ limite: 200 }),
    staleTime: 15000,
    refetchInterval: 30000,
  });

  const eventos = useQuery({
    queryKey: ["documental-eventos", "PENDIENTE"],
    queryFn: () => obtenerEventos({ estado: "PENDIENTE", limite: 50 }),
    staleTime: 15000,
    refetchInterval: 30000,
  });

  if (resumen.isLoading) return <Cargando />;
  if (resumen.isError) {
    return (
      <ErrorPanel
        error={resumen.error}
        mensaje={mensajeDeError(resumen.error)}
        reintentar={() => resumen.refetch()}
      />
    );
  }

  const porEstado = resumen.data || {};
  const documentos = bandeja.data?.documentos || [];
  const pendientes = (eventos.data || []).slice(0, 10);

  const total = Object.values(porEstado).reduce(
    (suma, n) => suma + Number(n || 0),
    0,
  );
  const aprobados = Number(porEstado.APROBADO || 0);
  const observados = Number(porEstado.OBSERVADO || 0);
  const enCurso =
    Number(porEstado.RECIBIDO || 0) +
    Number(porEstado.PROCESANDO || 0) +
    Number(porEstado.EXTRAIDO || 0) +
    Number(porEstado.VALIDADO || 0);
  const resueltos = aprobados + observados + Number(porEstado.RECHAZADO || 0);
  const automatizacion = resueltos
    ? Math.round((aprobados / resueltos) * 100)
    : null;

  return (
    <div
      className="flex flex-col gap-espacio-4 overflow-auto"
      data-testid="documental-tablero"
    >
      <div className="grid grid-cols-2 gap-espacio-3 lg:grid-cols-4">
        <TarjetaKpi
          etiqueta={t("documental.kpi.total")}
          valor={total}
          detalle={`${enCurso} ${t("documental.kpi.enCurso").toLowerCase()}`}
          tono="#6c36ff"
          gradiente="linear-gradient(135deg, #f4f0ff 0%, #ffffff 100%)"
        />
        <TarjetaKpi
          etiqueta={t("documental.estado.APROBADO")}
          valor={aprobados}
          detalle={total ? `${Math.round((aprobados / total) * 100)}% ${t("documental.tablero.delTotal")}` : "—"}
          tono="#059669"
          gradiente="linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%)"
        />
        <TarjetaKpi
          etiqueta={t("documental.estado.OBSERVADO")}
          valor={observados}
          detalle={t("documental.kpi.observados")}
          tono="#d97706"
          gradiente="linear-gradient(135deg, #fffbeb 0%, #ffffff 100%)"
        />
        <TarjetaKpi
          etiqueta={t("documental.kpi.automatizacion")}
          valor={automatizacion === null ? "—" : `${automatizacion}%`}
          detalle={t("documental.kpi.sinTocar")}
          tono="#0ea5e9"
          gradiente="linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%)"
        />
      </div>

      <div className="grid grid-cols-1 gap-espacio-4 lg:grid-cols-2">
        <Tarjeta>
          <CabeceraTarjeta titulo={t("documental.tablero.embudo")} />
          <div className="mt-espacio-4">
            <DistribucionEstados porEstado={porEstado} />
          </div>
        </Tarjeta>

        <Tarjeta>
          <CabeceraTarjeta titulo={t("documental.tablero.volumenDia")} />
          <div className="mt-espacio-4">
            <VolumenPorDia documentos={documentos} />
          </div>
        </Tarjeta>

        <Tarjeta>
          <CabeceraTarjeta titulo={t("documental.tablero.confianzaTipo")} />
          <div className="mt-espacio-4">
            <ConfianzaPromedio documentos={documentos} />
          </div>
        </Tarjeta>

        <Tarjeta>
          <CabeceraTarjeta titulo={t("documental.tablero.topMotivos")} />
          <div className="mt-espacio-4">
            <TopMotivos />
          </div>
        </Tarjeta>
      </div>

      <Tarjeta>
        <CabeceraTarjeta titulo={t("documental.tablero.eventosPendientes")} />
        <div className="mt-espacio-4 flex flex-col">
          {pendientes.length ? (
            pendientes.map((evento) => (
              <div
                key={evento.id}
                className="flex items-center gap-espacio-3 border-b border-borde py-espacio-2 last:border-b-0"
              >
                <span className="min-w-32 text-micro text-tinta-suave">
                  {comoFecha(evento.creado_en, idioma)}
                </span>
                <span className="flex-1 text-pequeno font-semibold text-tinta">
                  {evento.tipo_evento}
                </span>
                <span className="text-micro text-tinta-suave">
                  {t("documental.tablero.intentos")} {evento.intentos ?? 0}
                </span>
              </div>
            ))
          ) : (
            <p className="text-pequeno text-tinta-suave">
              {t("documental.tablero.sinPendientes")}
            </p>
          )}
        </div>
      </Tarjeta>
    </div>
  );
}
