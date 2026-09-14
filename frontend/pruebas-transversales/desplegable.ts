import { expect, type Page } from "@playwright/test";

export async function elegirEnDesplegable(
  pagina: Page,
  etiqueta: string,
  texto: string,
) {
  const control = pagina.getByRole("combobox", { name: etiqueta });
  await control.click();
  const lista = pagina.getByRole("listbox", { name: etiqueta });
  await expect(lista).toBeVisible();
  await lista.getByRole("option", { name: texto, exact: true }).click();
  await expect(lista).toBeHidden();
}

export function desplegable(pagina: Page, etiqueta: string) {
  return pagina.getByRole("combobox", { name: etiqueta });
}
