import { Navigate, Route, Routes } from "react-router-dom";
import { Disposicion } from "./componentes/Disposicion";
import { Ingresar } from "./paginas/Ingresar";
import { Resumen } from "./paginas/Resumen";
import { Panel } from "./paginas/Panel";
import { Documentos } from "./paginas/Documentos";
import { Excepciones } from "./paginas/Excepciones";
import { useSesion } from "./contextos/ProveedorSesion";
import { Isotipo } from "./componentes/Marca";

export function Aplicacion() {
  const { sesion, cargando } = useSesion();

  if (cargando) {
    return (
      <div className="flex h-full items-center justify-center bg-lienzo">
        <div className="flex flex-col items-center gap-3 text-tinta-suave">
          <span className="esqueleto text-grafito">
            <Isotipo tamano={40} />
          </span>
          <p className="text-sm">Restaurando sesion...</p>
        </div>
      </div>
    );
  }

  if (!sesion) {
    return (
      <Routes>
        <Route path="/ingresar" element={<Ingresar />} />
        <Route path="*" element={<Navigate to="/ingresar" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/ingresar" element={<Navigate to="/resumen" replace />} />
      <Route element={<Disposicion />}>
        <Route path="/resumen" element={<Resumen />} />
        <Route path="/panel" element={<Panel />} />
        <Route path="/documentos" element={<Documentos />} />
        <Route path="/excepciones" element={<Excepciones />} />
      </Route>
      <Route path="*" element={<Navigate to="/resumen" replace />} />
    </Routes>
  );
}
