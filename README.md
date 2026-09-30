# polizas-api

API REST para la gestión de pólizas de arrendamiento **individuales** y **colectivas**: consulta, renovación por IPC, cancelación en cascada y administración de riesgos. Cada cambio de estado se notifica al servicio de edición del CORE, que en este proyecto está simulado con un endpoint mock.

- Java 17 · Spring Boot 3.5 · Spring Data JPA · Bean Validation
- H2 en memoria, inicializada con `schema.sql` y `data.sql`
- OpenAPI / Swagger UI · Actuator
- JUnit 5 · AssertJ · MockMvc

El documento con el diseño de sistema, la optimización SQL y la estrategia de Git está en [docs/Juan_Restrepo_Prueba_Tecnica.pdf](docs/Juan_Restrepo_Prueba_Tecnica.pdf).

## Contenido

1. [Ejecución](#ejecución)
2. [Seguridad](#seguridad)
3. [Endpoints](#endpoints)
4. [Colección cURL](#colección-curl)
5. [Reglas de negocio](#reglas-de-negocio)
6. [Arquitectura](#arquitectura)
7. [Integración con el CORE](#integración-con-el-core)
8. [Manejo de errores](#manejo-de-errores)
9. [Pruebas](#pruebas)
10. [Configuración](#configuración)
11. [Fuera de alcance](#fuera-de-alcance)

## Ejecución

Requisitos: JDK 17 o superior. No hace falta tener Maven instalado porque el proyecto incluye Maven Wrapper.

```bash
./mvnw spring-boot:run
```

En Windows:

```bat
mvnw.cmd spring-boot:run
```

También se puede empaquetar y ejecutar el JAR:

```bash
./mvnw clean package
java -jar target/polizas-api-1.0.0.jar
```

| Recurso      | URL                                                        |
|--------------|------------------------------------------------------------|
| API          | http://localhost:8080                                      |
| Swagger UI   | http://localhost:8080/swagger-ui.html                      |
| OpenAPI JSON | http://localhost:8080/v3/api-docs                          |
| Consola H2   | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:polizas`, usuario `sa`, sin clave) |
| Health       | http://localhost:8080/actuator/health                      |

En Swagger UI se usa el botón **Authorize** para registrar la api-key una sola vez.

## Seguridad

Todas las rutas de negocio exigen el header:

```
api-key: 123456
```

Si falta o no coincide, la respuesta es `401 Unauthorized` en formato `application/problem+json`. La comparación se hace en tiempo constante y el valor se puede cambiar con la variable de entorno `API_KEY`. Swagger, la consola H2 y el health check quedan fuera del filtro.

Cada respuesta incluye `X-Correlation-ID`. Si el cliente lo envía, se reutiliza; si no, se genera. El mismo identificador aparece en los logs, en los errores y en la llamada al CORE.

## Endpoints

| Método | Ruta                       | Descripción                                                     | Éxito |
|--------|----------------------------|-----------------------------------------------------------------|-------|
| GET    | `/polizas?tipo=&estado=`   | Lista pólizas. `tipo`: `INDIVIDUAL`, `COLECTIVA`. `estado`: `ACTIVA`, `RENOVADA`, `CANCELADA`. Ambos filtros son opcionales. | 200 |
| GET    | `/polizas/{id}/riesgos`    | Riesgos de la póliza, activos y cancelados                      | 200   |
| POST   | `/polizas/{id}/renovar`    | Renueva: IPC sobre el canon, prima recalculada, nueva vigencia  | 200   |
| POST   | `/polizas/{id}/cancelar`   | Cancela la póliza y todos sus riesgos                           | 200   |
| POST   | `/polizas/{id}/riesgos`    | Agrega un riesgo (solo pólizas colectivas)                      | 201   |
| POST   | `/riesgos/{id}/cancelar`   | Cancela un riesgo sin afectar los demás                         | 200   |
| POST   | `/core-mock/evento`        | Mock del servicio de edición del CORE; registra el evento en el log | 202 |

### Datos iniciales

| id | Número          | Tipo       | Estado    | Tomador                                     | Riesgos (id)            |
|----|-----------------|------------|-----------|---------------------------------------------|-------------------------|
| 1  | ARR-IND-000101  | INDIVIDUAL | ACTIVA    | Laura Marcela Gómez Ríos                    | 1                       |
| 2  | ARR-IND-000102  | INDIVIDUAL | CANCELADA | Andrés Felipe Castaño Mora                  | 2 (cancelado)           |
| 3  | ARR-IND-000103  | INDIVIDUAL | RENOVADA  | María José Pérez Salazar                    | 3                       |
| 4  | ARR-COL-000201  | COLECTIVA  | ACTIVA    | Inmobiliaria Andina S.A.S.                  | 4, 5, 6                 |
| 5  | ARR-COL-000202  | COLECTIVA  | ACTIVA    | Conjunto Residencial Altos del Retiro P.H.  | 7, 8 (cancelado)        |
| 6  | ARR-COL-000203  | COLECTIVA  | CANCELADA | Arrendamientos del Caribe Ltda.             | 9, 10 (cancelados)      |

Los nombres y documentos son ficticios.

## Colección cURL

```bash
BASE=http://localhost:8080
KEY="api-key: 123456"

# Listar
curl -s -H "$KEY" "$BASE/polizas"
curl -s -H "$KEY" "$BASE/polizas?tipo=COLECTIVA&estado=ACTIVA"

# Riesgos de una póliza
curl -s -H "$KEY" "$BASE/polizas/4/riesgos"

# Renovar (1 → RENOVADA; canon 2.500.000 → 2.630.000, prima 30.000.000 → 31.560.000)
curl -s -X POST -H "$KEY" "$BASE/polizas/1/renovar"

# Renovar una póliza cancelada → 409 POLIZA_CANCELADA
curl -s -X POST -H "$KEY" "$BASE/polizas/2/renovar"

# Agregar riesgo a colectiva → 201
curl -s -X POST -H "$KEY" -H "Content-Type: application/json" "$BASE/polizas/4/riesgos" -d '{
  "direccionInmueble": "Carrera 11 # 86-32 Apto 601",
  "ciudad": "Bogotá D.C.",
  "asegurado":    { "tipoDocumento": "CC", "documento": "1019876543", "nombre": "Natalia Suárez Pinzón" },
  "beneficiario": { "tipoDocumento": "CC", "documento": "19876543",   "nombre": "Óscar Iván Beltrán Rey" },
  "canonMensual": 2400000
}'

# Agregar riesgo a individual → 422 TIPO_POLIZA_INVALIDO
curl -s -X POST -H "$KEY" -H "Content-Type: application/json" "$BASE/polizas/1/riesgos" -d '{
  "direccionInmueble": "Calle 1 # 2-3", "ciudad": "Bogotá D.C.",
  "asegurado":    { "tipoDocumento": "CC", "documento": "1019876543", "nombre": "Natalia Suárez Pinzón" },
  "beneficiario": { "tipoDocumento": "CC", "documento": "19876543",   "nombre": "Óscar Iván Beltrán Rey" },
  "canonMensual": 1000000
}'

# Cancelar un riesgo de colectiva
curl -s -X POST -H "$KEY" "$BASE/riesgos/5/cancelar"

# Cancelar el único riesgo activo → 409 ULTIMO_RIESGO_ACTIVO
curl -s -X POST -H "$KEY" "$BASE/riesgos/1/cancelar"

# Cancelar póliza (cascada sobre riesgos)
curl -s -X POST -H "$KEY" "$BASE/polizas/4/cancelar"

# Mock del CORE
curl -s -X POST -H "$KEY" -H "Content-Type: application/json" "$BASE/core-mock/evento" \
     -d '{"evento": "ACTUALIZACION", "polizaId": 555}'

# Sin api-key → 401
curl -s -i "$BASE/polizas"
```

Después de renovar o cancelar, el log de la aplicación muestra la llamada al CORE:

```
INFO [polizas-api,5b1c...] c.s.p.coremock.CoreMockController : [CORE-MOCK] Evento recibido para envío al CORE: evento=ACTUALIZACION polizaId=1 riesgoId=null operacion=RENOVACION_POLIZA
INFO [polizas-api,5b1c...] c.s.p.i.core.CoreEdicionRestAdapter : CORE notificado: operacion=RENOVACION_POLIZA polizaId=1 riesgoId=null
```

## Reglas de negocio

| Regla | Dónde se aplica |
|-------|-----------------|
| Prima = canon mensual × meses de vigencia | `Poliza.recalcularValores()` |
| Canon de la póliza = suma del canon de sus riesgos activos (en la individual, el de su único riesgo) | `Poliza.recalcularValores()` |
| Una póliza individual tiene exactamente un riesgo, y su tomador es el asegurado (arrendatario) | `Poliza.individual(...)`, `Poliza.agregarRiesgo(...)` |
| Solo las colectivas admiten nuevos riesgos | `Poliza.agregarRiesgo(...)` → `TipoPolizaInvalidaException` |
| No se renueva una póliza cancelada | `Poliza.renovar(...)` → `PolizaCanceladaException` |
| Renovar aplica el IPC al canon de cada riesgo activo, recalcula la prima, extiende la vigencia por el mismo número de meses y deja la póliza `RENOVADA` | `Poliza.renovar(...)` |
| Cancelar la póliza cancela todos sus riesgos | `Poliza.cancelar(...)` |
| Cancelar un riesgo no afecta a los demás y recalcula canon y prima | `Poliza.cancelarRiesgo(...)` |
| No se puede dejar una póliza vigente sin riesgos activos: el último riesgo se retira cancelando la póliza | `Poliza.cancelarRiesgo(...)` → `UltimoRiesgoActivoException` |
| Toda modificación de estado se notifica al CORE | `CoreSincronizacionListener` |

### Supuestos de diseño

El enunciado deja algunos puntos abiertos. Estas son las decisiones tomadas:

1. **Canon por riesgo.** Una colectiva cubre varios inmuebles con cánones distintos, así que el canon se guarda en cada riesgo y el de la póliza es su suma. Sin eso no se podría recalcular la prima al agregar o cancelar riesgos.
2. **"Canon y prima + IPC".** El IPC se aplica al canon y la prima se recalcula. El resultado porcentual es el mismo y se evita que el redondeo rompa `prima = canon × meses`.
3. **Valor del IPC.** No se especifica; es configurable (`IPC_PORCENTAJE`, por defecto `5.20`).
4. **Nueva vigencia.** Empieza en la fecha fin de la anterior y dura los mismos meses. Cada renovación queda en `historial_renovacion`.
5. **Último riesgo activo.** Cancelarlo dejaría una póliza vigente con prima cero, así que se rechaza con 409.
6. **Renovaciones repetidas.** Se permite renovar pólizas `ACTIVA` y `RENOVADA`. En producción habría una ventana de renovación y una llave de idempotencia por periodo.
7. **Versión de la API.** Las rutas son las del enunciado (`/polizas`). El prefijo `/api/v1` se publica en el API Gateway.

## Arquitectura

Arquitectura hexagonal (puertos y adaptadores) organizada en las capas controller → service → repository que pide el enunciado:

```
com.segurosbolivar.polizas
├── api                  Adaptador de entrada REST
│   ├── controller       PolizaController, RiesgoController
│   ├── dto              Records de entrada/salida con Bean Validation
│   ├── mapper           DTO ↔ dominio
│   └── error            GlobalExceptionHandler (RFC 7807)
├── application          Casos de uso
│   ├── port             CoreEdicionPort
│   └── service          PolizaService, RiesgoService, CoreSincronizacionListener
├── domain               Reglas de negocio, sin dependencias de infraestructura web o HTTP
│   ├── model            Poliza (raíz del agregado), Riesgo, Tercero, HistorialRenovacion
│   ├── exception        Excepciones de negocio con código estable
│   ├── event            PolizaModificadaEvent
│   └── repository       Puertos de persistencia
├── infrastructure       Adaptadores de salida y configuración
│   ├── persistence      Spring Data JPA
│   ├── core             Cliente HTTP del servicio de edición del CORE
│   ├── web              ApiKeyFilter, CorrelationIdFilter
│   └── config           Clock, OpenAPI
└── coremock             POST /core-mock/evento
```

Decisiones relevantes:

- **Modelo de dominio rico.** `Poliza` es la raíz del agregado y concentra todas las reglas. No hay setters públicos y los riesgos solo cambian de estado a través de la póliza, así que la consistencia entre canon, prima y riesgos no depende de que el servicio "se acuerde" de recalcular.
- **Entidades JPA en el dominio.** Es una concesión consciente: separar entidades de persistencia y de dominio duplicaría clases y mapeos sin beneficio para este alcance. Los repositorios del dominio son interfaces propias; Spring Data las implementa en `infrastructure.persistence`.
- **Dinero con `BigDecimal`**, escala 2 y redondeo `HALF_UP`.
- **`Clock` inyectado** en zona `America/Bogota`, para que las fechas no dependan de la zona del servidor y las pruebas sean deterministas.
- **Bloqueo optimista** (`@Version`) en póliza y riesgo. Un conflicto concurrente responde 409 `MODIFICACION_CONCURRENTE`.
- **`open-in-view` desactivado** y `ddl-auto=validate`: el esquema lo define `schema.sql` y Hibernate solo verifica que coincida con las entidades.

## Integración con el CORE

```
PolizaService ──publishEvent──▶ PolizaModificadaEvent
                                     │  (AFTER_COMMIT)
                                     ▼
                     CoreSincronizacionListener ──▶ CoreEdicionPort
                                                         │
                                                         ▼
                              CoreEdicionRestAdapter ──HTTP POST──▶ /core-mock/evento
```

- El CORE se notifica **después del commit**. Si la transacción se revierte, el CORE nunca recibe el cambio.
- El adaptador hace una llamada HTTP real, con timeouts, `api-key` y `X-Correlation-ID`, al endpoint configurado en `core.edicion.base-url`. Si no se configura, apunta al mock de la misma instancia.
- Payload enviado:

  ```json
  { "evento": "ACTUALIZACION", "polizaId": 4, "riesgoId": 5, "operacion": "CANCELACION_RIESGO" }
  ```

  `evento` y `polizaId` son los del contrato del enunciado. `riesgoId` y `operacion` son opcionales y le dicen a la capa media qué cambió.
- Una falla del CORE no revierte la operación de negocio: se registra en `ERROR` con el contexto necesario para reprocesarla. En producción este adaptador se reemplaza por un outbox transaccional con reintentos, circuit breaker y DLQ, como se describe en el documento de arquitectura. El cambio queda contenido en el adaptador porque el resto del código depende solo de `CoreEdicionPort`.

## Manejo de errores

Todas las respuestas de error siguen RFC 7807 (`application/problem+json`) y agregan `codigo`, `timestamp` y `correlationId`:

```json
{
  "type": "urn:polizas:error:poliza-cancelada",
  "title": "Conflicto con el estado actual",
  "status": 409,
  "detail": "No es posible renovar la póliza 2 porque está cancelada",
  "instance": "/polizas/2/renovar",
  "codigo": "POLIZA_CANCELADA",
  "timestamp": "2026-09-29T10:15:30.123-05:00",
  "correlationId": "c0a8012e-8f1d-4a4b-9a63-2f6d3f1b7e21"
}
```

| HTTP | Código                     | Caso |
|------|----------------------------|------|
| 400  | `SOLICITUD_INVALIDA`       | Cuerpo inválido; incluye `errores[]` con campo y mensaje |
| 400  | `PARAMETRO_INVALIDO`       | Valor de `tipo` o `estado` desconocido |
| 401  | `API_KEY_INVALIDA`         | Header `api-key` ausente o incorrecto |
| 404  | `POLIZA_NO_ENCONTRADA`, `RIESGO_NO_ENCONTRADO` | Recurso inexistente |
| 409  | `POLIZA_CANCELADA`         | Renovar, cancelar o modificar riesgos de una póliza cancelada |
| 409  | `RIESGO_CANCELADO`         | Cancelar un riesgo ya cancelado |
| 409  | `ULTIMO_RIESGO_ACTIVO`     | Cancelar el único riesgo activo de una póliza |
| 409  | `MODIFICACION_CONCURRENTE` | Conflicto de bloqueo optimista |
| 422  | `TIPO_POLIZA_INVALIDO`     | Agregar riesgos a una póliza individual |
| 500  | `ERROR_INTERNO`            | Error no controlado; no expone detalles internos |

## Pruebas

```bash
./mvnw test
```

| Suite | Tipo | Qué cubre |
|-------|------|-----------|
| `PolizaTest` | Unitaria, sin Spring | Cálculo de prima, renovación por IPC y vigencia, cancelación en cascada, restricciones por tipo, cancelación de riesgos |
| `PolizaApiIntegrationTest` | Integración con MockMvc y H2 | Cada endpoint: api-key, correlation id, filtros, validaciones, códigos HTTP, formato RFC 7807, notificación al CORE solo cuando la operación se confirma |
| `CoreEdicionIntegrationTest` | Extremo a extremo sobre puerto aleatorio | La cancelación dispara una llamada HTTP real al mock y queda registrada en el log |

Cada prueba de integración recarga los datos iniciales, así que son independientes entre sí y del orden de ejecución.

## Configuración

| Propiedad                          | Variable de entorno | Valor por defecto |
|------------------------------------|---------------------|-------------------|
| `server.port`                      | `PORT`              | `8080`            |
| `seguridad.api-key.valor`          | `API_KEY`           | `123456`          |
| `polizas.renovacion.ipc-porcentaje`| `IPC_PORCENTAJE`    | `5.20`            |
| `core.edicion.base-url`            | `CORE_EDICION_URL`  | vacío (usa el mock local) |
| `core.edicion.connect-timeout`     | —                   | `2s`              |
| `core.edicion.read-timeout`        | —                   | `5s`              |

## Fuera de alcance

- Creación y modificación de pólizas por API: el enunciado del ejercicio práctico no las incluye. Las fábricas `Poliza.individual(...)` y `Poliza.colectiva(...)` ya validan sus invariantes.
- Notificaciones por correo y SMS, outbox, reintentos y DLQ: están diseñados en el documento de arquitectura (Módulo 1).
- Paginación del listado de pólizas, liquidación de prima no devengada en cancelaciones y autenticación OAuth2 (el requisito pide api-key estática).
