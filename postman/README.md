# Colección de Postman: Reputation

`OficioYa-Reputation.postman_collection.json` reúne los **casos de aceptación** del sprint 3 de Reputation, más una verificación de su documentación Swagger.

| Carpeta | Peticiones | Qué demuestra |
|---|---|---|
| Reputation | 18 | Avisos de Service, calificación mutua, regla de una por parte (409), promedio, reseñas de solo lectura, reporte y moderación, recálculo, penalización por cancelación tardía, estadísticas, 403 y 401. |
| Documentación (Swagger) | 1 | `/v3/api-docs` publicado, con el esquema JWT. |

Cada petición tiene pruebas (`pm.test`) que verifican el código HTTP y el contenido.

## Tokens
No hay que pegar tokens a mano. El *pre-request* de la colección genera un JWT HS256 para cada rol:
- `token_contratante`
- `token_trabajador`
- `token_admin`
- `token_sistema`

Para firmarlos usa la variable `jwtSecret`, que es el mismo secreto de desarrollo de `application.yml`.

Al empezar, la colección genera ids nuevos de trabajador, contratante y solicitudes, así que se puede ejecutar las veces que se quiera.

## Opción 1: Postman (app)
1. Importar `OficioYa-Reputation.postman_collection.json`.
2. Levantar el servicio: `docker compose up -d` (PostgreSQL) y luego `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`.
3. Hacer clic derecho en la colección y elegir **Run collection**.

Para apuntar a otro entorno, cambia las variables `reputationUrl` y `jwtSecret` de la colección.

## Opción 2: todo automático con Docker y Newman
Primero se compila el proyecto con `./mvnw verify`. Luego, desde la raíz del proyecto:

```bash
bash postman/ejecutar-coleccion.sh
```

El script hace lo siguiente:
1. Levanta PostgreSQL y el servicio en contenedores.
2. Ejecuta la colección con Newman.
3. Muestra los logs relevantes (calificaciones, moderación y penalizaciones).
4. Borra los contenedores.

Resultado esperado: **19 peticiones y 30 aserciones, 0 fallidas**.
