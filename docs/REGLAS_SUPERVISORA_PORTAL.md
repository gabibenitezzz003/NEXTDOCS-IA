# Reglas de la Supervisora en el portal

Pestaña **Reglas** dentro de Procesos, junto a Definiciones, Instancias y Tareas.

## Por qué el campo se llama "Dato a observar"

En el backend el campo se llama `tipo`, pero no es una categoría: es la **clave del dato** que
la supervisora busca en la instancia (`datos[regla.tipo]`). Llamarlo "tipo" en la pantalla
llevaba a elegirlo de una lista que no existe. El campo es de texto libre porque los datos de
una instancia dependen de cada proceso, y la ayuda del campo lo dice explícito.

Si la clave no coincide con ningún dato de la instancia, la regla nunca dispara y no deja
rastro. Es el error más fácil de cometer y el más difícil de notar.

## Umbral

El tope es 999.99 porque la columna es `NUMERIC(5,2)`. El formulario valida contra ese máximo
antes de habilitar el envío, así el error se ve en el campo y no como un 400 del backend.

## La baja pide confirmación

`Dar de baja` no borra en el primer clic: pinta un aviso y cambia el botón a `Confirmar baja`,
con un `Conservar` al lado para salir. Se hizo así porque la baja de una regla `BLOQUEAR`
cambia el comportamiento de instancias vivas, y porque el backend no expone un "deshacer": la
baja es lógica pero no hay endpoint para revertirla.

Mientras no se confirma, no se llama al backend.

## Alcance

El selector permite dejar la regla sin plantilla. En el backend eso significa que la regla
aplica a **todos** los procesos del tenant (`plantillaId IS NULL` en
`listarPorPlantillaOTenant`), no que esté incompleta. La pantalla lo muestra como
"Todas las plantillas" en vez de dejar el espacio vacío.

## Qué no hace

- No permite editar el `codigo` ni nada de la plantilla: eso vive en Definiciones.
- No dispara evaluaciones. El endpoint `evaluar` existe, pero la evaluación real ocurre sola al
  completar cada tarea; un botón de "evaluar ahora" sin una instancia elegida no significaría
  nada.
- No lista los hallazgos. Eso ya está en el detalle de la instancia.
