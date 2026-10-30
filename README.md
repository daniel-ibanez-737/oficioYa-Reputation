# oficioya-reputation

Microservicio del dominio **Reputation** de OficioYa (Squad 5).

Calificaciones mutuas, reseñas inmutables, reportes de reseñas, pausado automático y penalizaciones.

## Tecnología
- Java 21, Spring Boot 4.1.1, Maven
- Spring Security con JWT (validado también por el Orchestrator)
- JUnit 5 y JaCoCo (cobertura mínima del 80 %)

## Estructura por capas (`edu.eci.dosw.oficioya.reputation`)
| Paquete | Capa |
|---|---|
| `controller` | Presentación (API REST) |
| `service` | Aplicación (casos de uso) |
| `domain` | Dominio (modelo y reglas de negocio) |
| `dto` | Objetos de transferencia |
| `repository` | Persistencia (Spring Data JPA) |
| `client` | Infraestructura: `OrquestadorClient`, única salida hacia otros dominios o la IA |
| `config` | Configuración (seguridad, clientes HTTP) |
| `exception` | Excepciones y manejo de errores |

Toda comunicación con otros dominios o con la IA **entra y sale por el Orchestrator**.

## Base de datos
Usa PostgreSQL con migraciones de Flyway en `src/main/resources/db/migration`.
Para levantar una base local:

```bash
docker compose up -d
```

Las pruebas usan H2 en memoria (perfil `test`), así que no necesitan PostgreSQL.

## Compilar y probar
Con Java 21 instalado (no hace falta Maven: el proyecto trae Maven Wrapper):

```bash
./mvnw verify          # Linux, macOS o Git Bash
mvnw.cmd verify        # Windows (CMD o PowerShell)
```

Sin Java, con Docker:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn verify
```

`mvn verify` compila, ejecuta las pruebas y valida la cobertura mínima con JaCoCo
(reporte en `target/site/jacoco/index.html`).

## Variables de entorno
| Variable | Uso | Valor por defecto (desarrollo) |
|---|---|---|
| `JWT_SECRET` | Secreto para validar el JWT | valor de desarrollo |
| `JWT_ROLES_CLAIM` | Claim del JWT con los roles | `roles` |
| `ORQUESTADOR_URL` | URL del Orchestrator | `http://localhost:8080` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/oficioya_reputation`, `oficioya` |

## Reglas configurables (`oficioya.reputation.reglas`)
| Propiedad | Valor | Regla |
|---|---|---|
| `umbral-pausa` | 3.0 | Se pausa si el promedio de las últimas reseñas es inferior. |
| `resenas-evaluadas` | 5 | Cuántas reseñas recientes se evalúan. |
| `penalizacion-cancelacion-tardia` | 0.5 | Puntos que se descuentan por cancelación tardía. |
| `horas-limite-cancelacion` | 24 | Anticipación mínima para cancelar sin penalización. |

## Endpoints (todos requieren JWT)
| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/reputation/resenas` | TRABAJADOR, CONTRATANTE | Calificar (1–5) la otra parte de un servicio cumplido. |
| GET | `/api/reputation/resenas/{codigo}` | cualquiera | Reseña por su código único. |
| GET | `/api/reputation/trabajadores/{id}/resenas` | cualquiera | Reseñas visibles del trabajador (solo lectura). |
| GET | `/api/reputation/trabajadores/{id}` | cualquiera | Promedio, penalización, reputación final y estado. |
| GET | `/api/reputation/contratantes/{id}` | cualquiera | Promedio del contratante. |
| GET | `/api/reputation/reputaciones?trabajadorIds=1,2` | cualquiera | Resumen para Search. |
| POST | `/api/reputation/resenas/{codigo}/reportes` | TRABAJADOR, CONTRATANTE | Reportar una reseña (queda PENDIENTE). |
| GET | `/api/reputation/reportes?estado=PENDIENTE` | ADMINISTRADOR | Reportes para moderar. |
| POST | `/api/reputation/reportes/{id}/resolucion` | ADMINISTRADOR | `RESENA_RETIRADA` o `RESENA_MANTENIDA`. |
| GET | `/api/reputation/estadisticas?desde&hasta` | ADMINISTRADOR | Datos agregados para Reporting. |
| POST | `/api/reputation/eventos/servicios-cumplidos` | SISTEMA | Aviso de Service: la solicitud quedó Cumplida (habilita calificar). |
| POST | `/api/reputation/eventos/cancelaciones-tardias` | SISTEMA | Aviso de Service: el trabajador canceló (aplica −0,5 si fue tardía). |

Errores: `400` validación, `401` sin JWT, `403` rol o usuario sin permiso, `404` no existe, `409` conflicto (por ejemplo, calificar dos veces).

Cuando un trabajador se pausa, Reputation avisa a Worker a través del Orchestrator.
Con el perfil `demo` ese aviso solo se escribe en el log.

## Supuestos pendientes de confirmar
- El `sub` del JWT es el id numérico del usuario y el Orchestrator envía los avisos con `ROLE_SISTEMA` (Squad 1).
- Formato de los avisos de Service (Squad 3) y ruta hacia Worker (`oficioya.orquestador.rutas.estado-trabajador`).
- Una reseña reportada sigue visible hasta que se resuelva el reporte.
- El proceso de reactivación de un trabajador pausado aún no está definido (RF-05), así que no se implementó.


## Documentación de la API (Swagger)
Con el servicio en ejecución:
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI (JSON): `http://localhost:8082/v3/api-docs`

Para probar desde Swagger UI, se pulsa **Authorize** y se pega un JWT.
La colección de Postman del servicio (`postman/`) genera tokens de prueba para cada rol y se ejecuta sola con `bash postman/ejecutar-coleccion.sh` (ver `postman/README.md`).
La documentación es pública y se puede apagar con `SWAGGER_ENABLED=false`.
