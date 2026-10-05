# hospital-flow · bed-management

Microservicio de ejemplo para **gestión de camas hospitalarias**, construido como preparación para la entrevista con Heuristik.

> **Aviso:** no es la arquitectura real de Heuristik (no es pública). Es una propuesta razonada a partir de lo que pedía su oferta: Spring Boot, Java 17/Kotlin, microservicios, Kafka, PostgreSQL/MongoDB, AWS + Kubernetes, despliegues on-premise en hospitales, reintentos, trazabilidad, evolución de contratos entre sistemas y rendimiento.

## Caso de negocio

Cuando el sistema de admisiones del hospital (HIS/ADT) registra un ingreso, publica un evento. Este servicio asigna automáticamente una cama libre de la unidad pedida y publica `BedAssigned`. Al dar el alta, la cama pasa a **limpieza** y solo vuelve a **disponible** cuando el personal de limpieza la marca. Otros servicios (cuadro de mando de ocupación, limpieza, planificación de altas) consumen esos eventos.

```
 AVAILABLE ──assignTo──▶ OCCUPIED ──release──▶ CLEANING ──markCleaned──▶ AVAILABLE
```

## Arquitectura (hexagonal + eventos)

```
               ┌──────────────── infrastructure (Spring) ────────────────┐
  HTTP ───────▶│ rest/BedController ─┐                                    │
               │                     │   ┌──── application (Java puro) ──┐│
  Kafka ──────▶│ messaging/          ├──▶│ port.in  ◀── service ──▶ port.out ├──▶ persistence/ (JPA, PostgreSQL)
 his.admission-│ AdmissionRequested- │   │          ▲                    ││    messaging/outbox/ (tabla outbox)
 requested     │ Listener (upcasting)┘   │      domain (Java puro)     ││              │
               │                          └──────────────────────────────┘│     OutboxRelay ──▶ Kafka beds.bed-events
               └─────────────────────────────────────────────────────────┘
```

- **domain**: agregado `Bed` con sus reglas y eventos (`BedAssigned`, `BedReleased`, `BedBecameAvailable`). No depende de ningún framework.
- **application**: casos de uso (`BedManagementService`, `AdmissionHandler`) y puertos. Tampoco depende de Spring; la transacción la pone un decorador (`TransactionalUseCases`).
- **infrastructure**: adaptadores REST, JPA, Kafka, outbox, observabilidad y configuración.

## Decisiones técnicas (puntos para la entrevista)

| Requisito de la oferta | Cómo se resuelve aquí |
|---|---|
| **Consistencia BD ↔ Kafka** | Patrón **outbox transaccional**: el cambio de estado y el evento se guardan en la misma transacción; `OutboxRelay` los envía después. Nunca hay una cama asignada sin su evento (ni al revés). |
| **Reintentos** | `DefaultErrorHandler` con backoff exponencial (3 reintentos) para errores transitorios. Los errores de negocio o de contrato van directos a la **DLT** (`his.admission-requested.DLT`): reintentarlos no cambia el resultado. |
| **Idempotencia** | Kafka entrega "al menos una vez". La tabla `processed_messages` deduplica por id de admisión dentro de la misma transacción que la asignación. |
| **Concurrencia** | Bloqueo optimista (`@Version`): dos admisiones simultáneas no pueden quedarse con la misma cama; la perdedora se reintenta y coge otra. Particionado por clave para mantener el orden por cama. |
| **Varias réplicas** | El relay usa `SELECT … FOR UPDATE SKIP LOCKED`, así que cada pod publica un lote distinto. |
| **Evolución de contratos** | Cabecera `schemaVersion` en cada mensaje. `AdmissionContractReader` convierte v1 → v2 (*upcasting*), de modo que el productor puede migrar sin coordinar el despliegue. Versión desconocida → DLT. |
| **Trazabilidad** | `X-Correlation-Id` (HTTP) → MDC → columna de la outbox → cabecera Kafka → logs. Una admisión se puede seguir de extremo a extremo. |
| **Errores de API** | RFC 7807 (`ProblemDetail`): 404 cama inexistente, 409 regla de negocio o conflicto de concurrencia, 400 validación. |
| **Datos sensibles** | El servicio solo guarda un id de paciente seudonimizado, nunca datos clínicos (RGPD, datos de salud). |
| **Cloud y on-premise** | Una única imagen (JRE mínima, usuario sin root) y el mismo manifiesto de Kubernetes sirven para EKS y para un k3s dentro del hospital; lo específico de cada entorno va en ConfigMap/Secret. Probes de liveness/readiness y métricas Prometheus. |

### Qué mejoraría en producción (para comentar si preguntan)

- **Debezium (CDC)** sobre la outbox en lugar del sondeo, para reducir latencia y carga en la BD.
- **Schema Registry** (Avro/Protobuf) con reglas de compatibilidad en vez de JSON con versión manual.
- **OpenTelemetry** (Micrometer Tracing) para trazas distribuidas además del id de correlación.
- **Integración HL7 v2 / FHIR** en un adaptador anticorrupción separado que traduzca los mensajes ADT del HIS a eventos de dominio.
- Limpieza periódica de `outbox_events` y `processed_messages`.
- Tests de integración con **Testcontainers** (PostgreSQL y Kafka reales).
- **Kotlin**: el dominio se puede pasar a Kotlin (data classes, `sealed class` con `when` exhaustivo) sin tocar la arquitectura.

## Estructura

```
src/main/java/com/hospitalflow/beds
├── domain/            model/ (Bed, ids, estados) · event/ · exception/
├── application/       port/in · port/out · service/
└── infrastructure/    rest/ · persistence/ · messaging/{contract,outbox} · observability/ · config/
src/main/resources/db/migration   Flyway (esquema + camas de demo)
deploy/k8s                        Deployment + Service + ConfigMap
```

## Cómo ejecutarlo

Requisitos: JDK 17+, Maven y Docker.

```bash
mvn test                 # tests de dominio, aplicación, contrato y API (H2, sin Kafka)
docker compose up --build
```

```bash
# Admisión síncrona
curl -i -X POST localhost:8080/api/admissions \
  -H 'Content-Type: application/json' -H 'X-Correlation-Id: demo-1' \
  -d '{"patientId":"P-1","wardId":"UCI"}'

# Estado de la unidad
curl localhost:8080/api/wards/UCI/beds

# Alta y limpieza
curl -X POST localhost:8080/api/beds/UCI-01/release
curl -X POST localhost:8080/api/beds/UCI-01/cleaned

# Admisión asíncrona (contrato v2) desde el HIS simulado
docker compose exec kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic his.admission-requested \
  --property parse.headers=true --property headers.delimiter='\t' --property headers.separator=','
# y escribir (tabulador entre cabeceras y cuerpo):
# schemaVersion:2,messageId:m-1	{"admissionId":"ADM-1","patientId":"P-9","wardCode":"MED-INT","requestedAt":"2026-10-06T11:30:00Z"}
```
