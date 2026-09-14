import { BotonIcono } from "./Interfaz";
import { IconoLuna, IconoSol } from "./Iconos";
import { useTema } from "../contextos/ProveedorTema";

export function AlternarTema({ className = "" }: { className?: string }) {
  const { tema, alternar } = useTema();
  const oscuro = tema === "dark";
  return (
    <BotonIcono
      variante="secundario"
      className={className}
      aria-label={oscuro ? "Activar tema claro" : "Activar tema oscuro"}
      aria-pressed={oscuro}
      title={oscuro ? "Tema claro" : "Tema oscuro"}
      onClick={alternar}
    >
      {oscuro ? <IconoSol tamano={18} /> : <IconoLuna tamano={18} />}
    </BotonIcono>
  );
}
