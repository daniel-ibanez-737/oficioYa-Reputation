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

### Datos de prueba para la demo
El perfil `datos-demo` carga `src/main/resources/db/demo/R__datos_demo.sql`: reseñas de los trabajadores 101 a 107
(los simulados en Search), el trabajador 108 pausado, reportes pendientes y las solicitudes 900001 y 900002 sin
calificar para el contratante 201. Se usa solo con una base de demo, nunca con una base real:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo,datos-demo
```

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
| `JWT_SECRET` | Secreto compartido con Identity, en Base64 (igual que Identity) | valor de desarrollo |
| `JWT_ROLES_CLAIM` | Claim del JWT con los roles | `roles` |
| `ORQUESTADOR_URL` | URL del Orchestrator | `http://localhost:8080` |
| `ADMIN_ID` | Id (UUID) del administrador que recibe el aviso de reseñas reportadas | `00000000-0000-0000-0000-000000000900` |
| `TIPO_AVISO_REPORTE` | Tipo de notificación del aviso (Notification aún no tiene uno para reseñas) | vacío |
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
| POST | `/api/v1/reputation/resenas` | TRABAJADOR, CONTRATANTE | Calificar (1–5) la otra parte de un servicio cumplido. |
| GET | `/api/v1/reputation/resenas/{codigo}` | cualquiera | Reseña por su código único. |
| GET | `/api/v1/reputation/trabajadores/{id}/resenas` | cualquiera | Reseñas visibles del trabajador (solo lectura). |
| GET | `/api/v1/reputation/trabajadores/{id}` | cualquiera | Promedio, penalización, reputación final y estado. |
| GET | `/api/v1/reputation/contratantes/{id}` | cualquiera | Promedio del contratante. |
| GET | `/api/v1/reputation/reputaciones?trabajadorIds={uuid},{uuid}` | cualquiera | Resumen para Search. |
| POST | `/api/v1/reputation/resenas/{codigo}/reportes` | TRABAJADOR, CONTRATANTE | Reportar una reseña (queda PENDIENTE). La `descripcion` es obligatoria, de 50 a 500 caracteres. Se avisa al administrador vía Orchestrator → Notification. |
| GET | `/api/v1/reputation/reportes?estado=PENDIENTE` | ADMIN | Reportes para moderar. |
| POST | `/api/v1/reputation/reportes/{id}/resolucion` | ADMIN | `RESENA_RETIRADA` o `RESENA_MANTENIDA`. |
| GET | `/api/v1/reputation/estadisticas?desde&hasta` | ADMIN | Datos agregados para Reporting. |
| POST | `/api/v1/reputation/eventos/servicios-cumplidos` | SISTEMA, ADMIN | Aviso de Service: la solicitud quedó Cumplida (habilita calificar). |
| POST | `/api/v1/reputation/eventos/cancelaciones-tardias` | SISTEMA, ADMIN | Aviso de Service: el trabajador canceló (aplica −0,5 si fue tardía). |

Errores: `400` validación, `401` sin JWT, `403` rol o usuario sin permiso, `404` no existe, `409` conflicto (por ejemplo, calificar dos veces).

Los ids de usuarios, trabajadores y solicitudes son UUID (los mismos de Identity, Worker y Service).

Cuando un trabajador se pausa, Reputation avisa a Worker a través del Orchestrator con su API real
(`PUT /api/v1/workers/{id}/status`, cuerpo `{"status": "PAUSED"}`). Cuando se reporta una reseña, avisa al
administrador con la API real de Notification (`POST /api/v1/notification`: `receiverId`, `title`, `description`,
`type`, `requestId`, `dateCreated`). Con el perfil `demo` esos avisos solo se escriben en el log.

## Formato del JWT (acordado con Identity, Squad 1)
- Firma HS256 con el secreto decodificado de Base64.
- `sub`: id (UUID) del usuario.
- `roles`: lista sin prefijo (`CONTRATANTE`, `TRABAJADOR`, `ADMIN`, `SISTEMA`).

## Supuestos pendientes de confirmar
- Service (Squad 4) todavía no envía los avisos de servicio cumplido y de cancelación tardía (está como TODO).
- Worker solo permite cambiar el estado con rol `ADMIN`; falta que acepte `SISTEMA`. El id que se usa es el del
  trabajador en Worker.
- Notification solo acepta `ADMIN` y no tiene un `type` para reseñas reportadas (se envía vacío hasta que exista).
- Una reseña reportada sigue visible hasta que se resuelva el reporte.
- El proceso de reactivación de un trabajador pausado aún no está definido (RF-05), así que no se implementó.


## Documentación de la API (Swagger)
Con el servicio en ejecución:
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI (JSON): `http://localhost:8082/v3/api-docs`

Para probar desde Swagger UI, se pulsa **Authorize** y se pega un JWT.
La colección de Postman del servicio (`postman/`) genera tokens de prueba para cada rol y se ejecuta sola con `bash postman/ejecutar-coleccion.sh`.
La documentación es pública y se puede apagar con `SWAGGER_ENABLED=false`.
