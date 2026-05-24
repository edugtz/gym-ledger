
---

## `ACCEPTANCE_CRITERIA.md`

```md
# GymLedger — Acceptance Criteria

## Global MVP acceptance

La app se considera MVP usable cuando:

- Instala correctamente como APK.
- Abre sin crash.
- Funciona offline.
- Permite crear ejercicios.
- Permite registrar entrenamientos.
- Permite registrar sets con reps, peso, RPE/RIR.
- Permite crear alimentos.
- Permite registrar comidas.
- Calcula calorías/macros diarios.
- Permite registrar peso corporal y medidas.
- Permite exportar backup JSON.
- Permite importar backup JSON.
- Permite exportar/importar CSV inicial.
- Permite tomar foto de comida.
- Permite crear una estimación aproximada editable desde foto + input del usuario.
- Los datos persisten después de cerrar la app.
- No hay backend ni login.

---

## Workout acceptance

- Crear sesión nueva desde cero.
- Agregar set a sesión.
- Editar set.
- Borrar set.
- Ver historial.
- Ver detalle.
- Persistencia correcta.
- Validación de reps > 0.
- Validación de weight >= 0.
- Validación de RPE 1-10 si existe.
- Validación de RIR >= 0 si existe.

---

## Routine acceptance

- Crear rutina.
- Agregar ejercicios.
- Definir sets/reps objetivo.
- Iniciar sesión desde rutina.
- La sesión generada es editable.
- Cambiar rutina no altera sesiones históricas.

---

## Nutrition acceptance

- Crear alimento manual.
- Crear comida.
- Agregar item a comida.
- Calcular macros por gramos.
- Editar macros manualmente.
- Ver resumen diario.
- Ver progreso contra objetivos.
- Persistencia correcta.

---

## Photo meal acceptance

- Tomar foto desde app.
- Asociar foto a una comida.
- Ver preview de foto.
- Crear estimación asistida.
- Mostrar warning de aproximación.
- Permitir editar antes de guardar.
- Guardar resultado como meal item.
- No presentar estimación como exacta.

---

## Body acceptance

- Registrar peso.
- Registrar medidas opcionales.
- Editar medición.
- Borrar medición.
- Mostrar último peso en Dashboard.

---

## Import/export acceptance

- Export JSON válido.
- Import JSON restaura datos.
- CSV exportable.
- CSV importable.
- Errores legibles por archivo/fila/campo.
- Import inválido no crashea app.
- Import inválido no deja datos corruptos.

---

## APK acceptance

Debe funcionar:

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk