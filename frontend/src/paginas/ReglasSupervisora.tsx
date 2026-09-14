import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  CabeceraTarjeta,
  Campo,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  actualizarRegla,
  crearRegla,
  darDeBajaRegla,
  listarProcesos,
  listarReglas,
  mensajeDeError,
} from "../api/procesos";
import type {
  AccionRegla,
  CambiosRegla,
  OperadorRegla,
  Proceso,
  ReglaSupervisora,
  SeveridadRegla,
} from "../api/procesos";
import type { Tono } from "../componentes/Interfaz";

const OPERADORES: { valor: OperadorRegla; texto: string }[] = [
  { valor: "MENOR", texto: "es menor que" },
  { valor: "MAYOR", texto: "es mayor que" },
];

const ACCIONES: { valor: AccionRegla; texto: string }[] = [
  { valor: "ADVERTIR", texto: "Advertir" },
  { valor: "SOLICITAR", texto: "Solicitar informacion" },
  { valor: "REVIEW", texto: "Marcar para revision" },
  { valor: "BLOQUEAR", texto: "Bloquear la instancia" },
];

const SEVERIDADES: { valor: SeveridadRegla; texto: string }[] = [
  { valor: "BAJA", texto: "Baja" },
  { valor: "MEDIA", texto: "Media" },
  { valor: "ALTA", texto: "Alta" },
  { valor: "CRITICA", texto: "Critica" },
];

const TONO_SEVERIDAD: Record<SeveridadRegla, Tono> = {
  BAJA: "neutro",
  MEDIA: "informacion",
  ALTA: "alerta",
  CRITICA: "rojo",
};

const UMBRAL_MAXIMO = 999.99;

export function ReglasSupervisora() {
  const [creando, setCreando] = useState(false);
  const [editando, setEditando] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["reglas-supervisora"],
    queryFn: () => listarReglas(),
  });

  const procesos = useQuery({
    queryKey: ["procesos"],
    queryFn: listarProcesos,
  });

  if (consulta.isPending) {
    return <Cargando filas={3} />;
  }

  if (consulta.isError) {
    return (
      <ErrorPanel
        mensaje={mensajeDeError(consulta.error)}
        reintentar={() => consulta.refetch()}
      />
    );
  }

  const reglas = consulta.data ?? [];

  return (
    <div className="grid gap-espacio-4">
      <Tarjeta>
        <CabeceraTarjeta
          titulo="Reglas de la IA Supervisora"
          descripcion="Cada regla compara un dato de la instancia contra un umbral. Se evaluan al completar cada tarea."
          acciones={
            <Boton
              variante={creando ? "secundario" : "primario"}
              onClick={() => {
                setCreando((actual) => !actual);
                setEditando(null);
              }}
            >
              {creando ? "Cancelar" : "Nueva regla"}
            </Boton>
          }
        />
        {creando ? (
          <FormularioRegla
            procesos={procesos.data ?? []}
            alCerrar={() => setCreando(false)}
          />
        ) : null}
      </Tarjeta>

      {reglas.length === 0 ? (
        <Vacio
          titulo="Todavia no hay reglas"
          detalle="Sin reglas la supervisora no genera hallazgos. Crea una para empezar a vigilar los datos de tus procesos."
        />
      ) : (
        reglas.map((regla, indice) => (
          <TarjetaRegla
            key={regla.id}
            regla={regla}
            indice={indice}
            procesos={procesos.data ?? []}
            editando={editando === regla.id}
            alEditar={() => {
              setEditando(editando === regla.id ? null : regla.id);
              setCreando(false);
            }}
            alCerrarEdicion={() => setEditando(null)}
          />
        ))
      )}
    </div>
  );
}

function TarjetaRegla({
  regla,
  indice,
  procesos,
  editando,
  alEditar,
  alCerrarEdicion,
}: {
  regla: ReglaSupervisora;
  indice: number;
  procesos: Proceso[];
  editando: boolean;
  alEditar: () => void;
  alCerrarEdicion: () => void;
}) {
  const clienteConsultas = useQueryClient();
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const baja = useMutation({
    mutationFn: () => darDeBajaRegla(regla.id),
    onSuccess: () => {
      setError(null);
      setConfirmando(false);
      clienteConsultas.invalidateQueries({ queryKey: ["reglas-supervisora"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const severidad = regla.severidad ?? "MEDIA";
  const alcance = procesos.find((proceso) => proceso.id === regla.plantillaId);

  return (
    <Tarjeta indice={indice}>
      <CabeceraTarjeta
        titulo={regla.nombre}
        descripcion={textoCondicion(regla)}
        acciones={
          <div className="flex flex-wrap items-center gap-espacio-2">
            <Pastilla tono={TONO_SEVERIDAD[severidad]}>{severidad}</Pastilla>
            {regla.accion === "BLOQUEAR" ? (
              <Pastilla tono="rojo" solido>
                Bloquea
              </Pastilla>
            ) : (
              <Pastilla tono="neutro">{textoAccion(regla.accion)}</Pastilla>
            )}
            <Boton variante="secundario" onClick={alEditar}>
              {editando ? "Cerrar" : "Editar"}
            </Boton>
            {confirmando ? (
              <Boton
                variante="secundario"
                disabled={baja.isPending}
                onClick={() => setConfirmando(false)}
              >
                Conservar
              </Boton>
            ) : null}
            <Boton
              variante={confirmando ? "peligro" : "fantasma"}
              cargando={baja.isPending}
              disabled={baja.isPending}
              onClick={() => (confirmando ? baja.mutate() : setConfirmando(true))}
            >
              {confirmando ? "Confirmar baja" : "Dar de baja"}
            </Boton>
          </div>
        }
      />

      <div className="mt-espacio-3 flex flex-wrap items-center gap-espacio-2">
        <Pastilla tono="violeta">
          {alcance ? alcance.nombre : "Todas las plantillas"}
        </Pastilla>
        <span className="text-pequeno text-tinta-suave">
          Dato observado: <code className="font-semibold text-tinta">{regla.tipo}</code>
        </span>
      </div>

      {regla.mensaje ? (
        <p className="mt-espacio-3 text-pequeno text-tinta-suave">{regla.mensaje}</p>
      ) : null}

      {confirmando && !baja.isPending ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-alerta-borde bg-alerta-tenue px-espacio-3 py-espacio-2 text-pequeno text-alerta-texto"
        >
          La regla deja de evaluarse. Los hallazgos que ya genero se conservan. Volve a tocar
          el boton para confirmar.
        </div>
      ) : null}

      {error ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-3 py-espacio-2 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}

      {editando ? (
        <FormularioRegla regla={regla} procesos={procesos} alCerrar={alCerrarEdicion} />
      ) : null}
    </Tarjeta>
  );
}

function FormularioRegla({
  regla,
  procesos,
  alCerrar,
}: {
  regla?: ReglaSupervisora;
  procesos: Proceso[];
  alCerrar: () => void;
}) {
  const clienteConsultas = useQueryClient();
  const [plantillaId, setPlantillaId] = useState(regla?.plantillaId ?? "");
  const [nombre, setNombre] = useState(regla?.nombre ?? "");
  const [tipo, setTipo] = useState(regla?.tipo ?? "");
  const [umbral, setUmbral] = useState(regla == null ? "" : String(regla.umbral));
  const [operador, setOperador] = useState<OperadorRegla>(regla?.operador ?? "MENOR");
  const [accion, setAccion] = useState<AccionRegla>(regla?.accion ?? "ADVERTIR");
  const [severidad, setSeveridad] = useState<SeveridadRegla>(regla?.severidad ?? "MEDIA");
  const [mensaje, setMensaje] = useState(regla?.mensaje ?? "");
  const [error, setError] = useState<string | null>(null);

  const valorUmbral = Number(umbral.trim());
  const umbralValido =
    umbral.trim() !== "" && Number.isFinite(valorUmbral) && valorUmbral <= UMBRAL_MAXIMO;
  const errorUmbral =
    umbral.trim() !== "" && !umbralValido
      ? `Tiene que ser un numero de hasta ${UMBRAL_MAXIMO}`
      : undefined;

  const guardar = useMutation({
    mutationFn: () => {
      const cambios: CambiosRegla = {
        plantillaId: plantillaId || undefined,
        nombre: nombre.trim(),
        tipo: tipo.trim(),
        umbral: valorUmbral,
        operador,
        accion,
        severidad,
        mensaje: mensaje.trim() || undefined,
      };
      return regla ? actualizarRegla(regla.id, cambios) : crearRegla(cambios);
    },
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["reglas-supervisora"] });
      alCerrar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const completo = nombre.trim() !== "" && tipo.trim() !== "" && umbralValido;

  return (
    <div className="mt-espacio-4 border-t border-borde pt-espacio-4">
      {error ? (
        <div
          role="alert"
          className="mb-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-3 py-espacio-2 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}

      <div className="grid gap-espacio-4 md:grid-cols-2">
        <Campo
          etiqueta="Nombre de la regla"
          value={nombre}
          onChange={(evento) => setNombre(evento.target.value)}
          maxLength={128}
        />
        <Selector
          etiqueta="Alcance"
          value={plantillaId}
          onChange={(evento) => setPlantillaId(evento.target.value)}
          ayuda="Sin plantilla la regla vigila todos los procesos del tenant."
        >
          <option value="">Todas las plantillas</option>
          {procesos.map((proceso) => (
            <option key={proceso.id} value={proceso.id}>
              {proceso.nombre}
            </option>
          ))}
        </Selector>
      </div>

      <div className="mt-espacio-4 grid gap-espacio-4 md:grid-cols-3">
        <Campo
          etiqueta="Dato a observar"
          value={tipo}
          onChange={(evento) => setTipo(evento.target.value)}
          maxLength={64}
          ayuda="La clave exacta del dato en la instancia, por ejemplo confidence."
        />
        <Selector
          etiqueta="Condicion"
          value={operador}
          onChange={(evento) => setOperador(evento.target.value as OperadorRegla)}
        >
          {OPERADORES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {opcion.texto}
            </option>
          ))}
        </Selector>
        <Campo
          etiqueta="Umbral"
          type="number"
          step="0.01"
          max={UMBRAL_MAXIMO}
          value={umbral}
          onChange={(evento) => setUmbral(evento.target.value)}
          error={errorUmbral}
        />
      </div>

      <div className="mt-espacio-4 grid gap-espacio-4 md:grid-cols-2">
        <Selector
          etiqueta="Que hace la supervisora"
          value={accion}
          onChange={(evento) => setAccion(evento.target.value as AccionRegla)}
          ayuda={
            accion === "BLOQUEAR"
              ? "Bloquear detiene la instancia activa apenas se dispara la regla."
              : undefined
          }
        >
          {ACCIONES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {opcion.texto}
            </option>
          ))}
        </Selector>
        <Selector
          etiqueta="Severidad del hallazgo"
          value={severidad}
          onChange={(evento) => setSeveridad(evento.target.value as SeveridadRegla)}
        >
          {SEVERIDADES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {opcion.texto}
            </option>
          ))}
        </Selector>
      </div>

      <div className="mt-espacio-4">
        <Campo
          etiqueta="Mensaje del hallazgo"
          value={mensaje}
          onChange={(evento) => setMensaje(evento.target.value)}
          maxLength={2048}
          placeholder="Opcional"
        />
      </div>

      <div className="mt-espacio-4 flex justify-end gap-espacio-2">
        <Boton variante="secundario" onClick={alCerrar} disabled={guardar.isPending}>
          Cancelar
        </Boton>
        <Boton
          variante="primario"
          cargando={guardar.isPending}
          disabled={guardar.isPending || !completo}
          onClick={() => guardar.mutate()}
        >
          {regla ? "Guardar cambios" : "Crear regla"}
        </Boton>
      </div>
    </div>
  );
}

function textoCondicion(regla: ReglaSupervisora): string {
  const comparacion = regla.operador === "MAYOR" ? "es mayor que" : "es menor que";
  return `Si ${regla.tipo} ${comparacion} ${regla.umbral}`;
}

function textoAccion(accion?: AccionRegla): string {
  const encontrada = ACCIONES.find((opcion) => opcion.valor === accion);
  return encontrada ? encontrada.texto : "Advertir";
}
