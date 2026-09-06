import { cliente } from "./cliente";
import type { TipoPropuesto } from "../tipos/api";

export async function listarTiposPropuestos(estado?: string): Promise<TipoPropuesto[]> {
  const { data } = await cliente.get<TipoPropuesto[]>("/administracion/tipos-propuestos", {
    params: { estado },
  });
  return data;
}

export async function aprobarTipoPropuesto(propuestoId: string): Promise<TipoPropuesto> {
  const { data } = await cliente.post<TipoPropuesto>(
    `/administracion/tipos-propuestos/${propuestoId}/aprobar`,
  );
  return data;
}

export async function descartarTipoPropuesto(propuestoId: string): Promise<TipoPropuesto> {
  const { data } = await cliente.post<TipoPropuesto>(
    `/administracion/tipos-propuestos/${propuestoId}/descartar`,
  );
  return data;
}
