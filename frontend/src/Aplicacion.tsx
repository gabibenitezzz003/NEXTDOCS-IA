import { Navigate, Route, Routes, useParams } from "react-router-dom";
import { Disposicion } from "./componentes/Disposicion";
import { Ingresar } from "./paginas/Ingresar";
import { Resumen } from "./paginas/Resumen";
import { Panel } from "./paginas/Panel";
import { Documentos } from "./paginas/Documentos";
import { Excepciones } from "./paginas/Excepciones";
import { TiposPropuestos } from "./paginas/TiposPropuestos";
import { Tareas } from "./paginas/Tareas";
import { Operacion } from "./paginas/Operacion";
import { Studio } from "./paginas/Studio";
import { Supervisora } from "./paginas/Supervisora";
import { Partners } from "./paginas/Partners";
import { Marketplace } from "./paginas/Marketplace";
import { Externo } from "./paginas/Externo";
import { useSesion } from "./contextos/ProveedorSesion";
import { Logotipo } from "./componentes/Marca";
import { Tarjeta } from "./componentes/Interfaz";

export function Aplicacion() {
  const { sesion, cargando } = useSesion();

  if (cargando) {
    return (
      <main className="grid min-h-dvh place-items-center bg-lienzo p-espacio-6">
        <Tarjeta padding="px-espacio-8 py-espacio-10" className="max-w-full">
          <div className="flex flex-col items-center gap-espacio-6">
            <Logotipo />
            <div
              role="status"
              aria-atomic="true"
              className="flex flex-col items-center gap-espacio-6"
            >
              <div aria-hidden="true" className="flex gap-espacio-2">
                <span className="h-espacio-1 w-espacio-12 animate-pulse rounded-insignia bg-violeta motion-reduce:animate-none" />
                <span className="h-espacio-1 w-espacio-12 rounded-insignia bg-borde" />
                <span className="h-espacio-1 w-espacio-12 rounded-insignia bg-borde" />
              </div>
              <p className="text-pequeno font-semibold text-tinta-media">
                Restaurando sesión…
              </p>
            </div>
          </div>
        </Tarjeta>
      </main>
    );
  }

  if (!sesion) {
    return (
      <Routes>
        <Route path="/externo/:token" element={<Externo />} />
        <Route path="/ingresar" element={<Ingresar />} />
        <Route path="*" element={<Navigate to="/ingresar" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/externo/:token" element={<Externo />} />
      <Route path="/ingresar" element={<Navigate to="/resumen" replace />} />
      <Route element={<Disposicion />}>
        <Route path="/resumen" element={<Resumen />} />
        <Route path="/panel" element={<Panel />} />
        <Route path="/documentos" element={<Documentos />} />
        <Route path="/excepciones" element={<Excepciones />} />
        <Route path="/tipos-propuestos" element={<TiposPropuestos />} />
        <Route path="/tareas" element={<Tareas />} />
        <Route path="/operacion" element={<Operacion />} />
        <Route path="/operacion/instancias/:instanciaId" element={<Operacion />} />
        <Route path="/workflow" element={<Studio />} />
        <Route path="/workflow/:procesoId" element={<Studio />} />
        <Route path="/supervisora" element={<Supervisora />} />
        <Route path="/partners" element={<Partners />} />
        <Route path="/marketplace" element={<Marketplace />} />
        <Route path="/studio" element={<Navigate to="/workflow" replace />} />
        <Route path="/studio/:procesoId" element={<RedirigirWorkflow />} />
        <Route path="/procesos" element={<Navigate to="/workflow" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/resumen" replace />} />
    </Routes>
  );
}

function RedirigirWorkflow() {
  const { procesoId } = useParams();
  return (
    <Navigate
      to={procesoId ? `/workflow/${procesoId}` : "/workflow"}
      replace
    />
  );
}
