import { cliente } from "./cliente";

export interface PlantillaResumen {
  id: string;
  codigo: string;
  nombre: string;
  familia?: string;
  descripcion?: string;
  versionPublicadaId?: string;
  numeroVersionPublicada?: number;
  cantidadVersiones?: number;
}

export async function listarPlantillas(): Promise<PlantillaResumen[]> {
  const { data } = await cliente.get<PlantillaResumen[]>("/plantillas");
  return data;
}
