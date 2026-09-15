import { BotonIcono } from "./Interfaz";
import { IconoLuna, IconoSol } from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { useTema } from "../contextos/ProveedorTema";

export function AlternarTema({ className = "" }: { className?: string }) {
  const { tema, alternar } = useTema();
  const { t } = useIdioma();
  const oscuro = tema === "dark";
  return (
    <BotonIcono
      variante="secundario"
      className={className}
      aria-label={oscuro ? t("comun.activarTemaClaro") : t("comun.activarTemaOscuro")}
      aria-pressed={oscuro}
      title={oscuro ? t("comun.temaClaro") : t("comun.temaOscuro")}
      onClick={alternar}
    >
      {oscuro ? <IconoSol tamano={18} /> : <IconoLuna tamano={18} />}
    </BotonIcono>
  );
}
