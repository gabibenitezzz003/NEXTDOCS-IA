import { defineConfig } from "@playwright/test";
import procesos from "./playwright.procesos.config";

export default defineConfig({
  ...procesos,
  testDir: "./pruebas-transversales",
});
