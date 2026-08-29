import { cliente } from "./cliente";
import type { Documento, KpiPlantilla, KpiResumen } from "../tipos/api";

export interface RangoKpi {
  desde?: string;
  hasta?: string;
}

export async function obtenerKpi(rango: RangoKpi): Promise<KpiResumen> {
  const { data } = await cliente.get<KpiResumen>("/kpi/resumen", { params: rango });
  return data;
}

export async function listarKpiPorPlantilla(rango: RangoKpi): Promise<KpiPlantilla[]> {
  const { data } = await cliente.get<KpiPlantilla[]>("/kpi/plantillas", { params: rango });
  return data;
}

export async function obtenerPoblacionKpi(indicador: string, rango: RangoKpi): Promise<Documento[]> {
  const { data } = await cliente.get<Documento[]>("/kpi/poblacion", {
    params: { indicador, ...rango },
  });
  return data;
}
