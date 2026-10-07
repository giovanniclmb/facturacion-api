# TP API REST con Spring Boot — Facturación

Trabajo práctico de Desarrollo de Software: servicio web **RESTful** construido con
**Spring Boot** que expone un reporte de facturas en formato JSON, con filtros
opcionales por fecha, estado y monto. La consulta proyecta directamente a un
**DTO** mediante una expresión de constructor en **JPQL**, sin cargar entidades
completas en memoria.

Reutiliza el modelo de dominio y la base de datos del TP anterior:
[jpa-facturacion](https://github.com/giovanniclmb/jpa-facturacion).

## Stack

| Tecnología | Rol |
| --- | --- |
| Java 21 | Lenguaje |
| Spring Boot 4.1 | Framework (Spring Web + Spring Data JPA) |
| Hibernate 7 | Implementación de JPA (`jakarta.persistence`) |
| PostgreSQL | Base de datos relacional |
| Lombok | Generación de getters, setters y constructores del DTO |
| Maven | Gestión de dependencias y build |

## Arquitectura

```
Cliente HTTP → FacturaRestController → FacturaService → EntityManager → PostgreSQL
                  (@RequestParam)       (JPQL dinámica)
```

| Clase | Paquete | Responsabilidad |
| --- | --- | --- |
| `FacturaRestController` | `controller` | Recibe el GET, convierte los parámetros y devuelve JSON |
| `FacturaService` | `service` | Arma la JPQL según los filtros recibidos y la ejecuta |
| `FacturaReporteDTO` | `dto` | Datos resumidos de cada factura para el reporte |
| Entidades JPA | `model` | Modelo de dominio (14 entidades, migradas de `javax` a `jakarta`) |

## Endpoint

### `GET /api/facturas`

Devuelve una lista de `FacturaReporteDTO`. Todos los parámetros son opcionales
y se combinan con `AND`.

| Parámetro | Tipo | Descripción |
| --- | --- | --- |
| `fechaDesde` | `yyyy-MM-dd` | Facturas emitidas desde esta fecha (inclusive) |
| `fechaHasta` | `yyyy-MM-dd` | Facturas emitidas hasta esta fecha (inclusive, día completo) |
| `estado` | texto | Estado exacto: `EMITIDA`, `PENDIENTE`, `ANULADA` |
| `montoMinimo` | número | Facturas con `importeTotal` mayor o igual al monto |

**Ejemplos:**

```
GET /api/facturas
GET /api/facturas?estado=EMITIDA
GET /api/facturas?fechaDesde=2026-02-01&fechaHasta=2026-03-31
GET /api/facturas?fechaDesde=2026-03-01&montoMinimo=200000
```

**Respuesta:**

```json
[
  {
    "numeroFactura": 5,
    "fechaEmision": "2026-03-15",
    "clienteDenominacion": "Distribuidora Mendoza SA",
    "condicionIva": "Responsable Inscripto",
    "puntoVentaDescripcion": "Sucursal Lujan",
    "importeTotal": 217800.0,
    "cantidadItems": 1
  }
]
```

Un parámetro con formato inválido (por ejemplo `fechaDesde=hola`) devuelve
`400 Bad Request`.

## Cómo ejecutarlo

**Requisitos:** JDK 21+, PostgreSQL y la base `facturacion` creada y cargada
con el proyecto [jpa-facturacion](https://github.com/giovanniclmb/jpa-facturacion).

**1. Clonar:**

```bash
git clone https://github.com/giovanniclmb/facturacion-api.git
cd facturacion-api
```

**2. Configurar la conexión.** El usuario se ajusta en
`src/main/resources/application.properties`; la clave se lee de una variable
de entorno:

```bash
export DB_PASSWORD=tu_clave
```

**3. Ejecutar:**

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080/api/facturas`.

## Decisiones de diseño

- **Proyección a DTO con `SELECT new`:** la consulta trae solo las 7 columnas
  del reporte. Evita exponer las entidades y el bucle de serialización
  `factura → detalles → factura`.
- **Consulta dinámica con `WHERE 1=1`:** cada filtro presente agrega un `AND`
  con parámetro nombrado; los valores nunca se concatenan al texto de la consulta.
- **`fechaHasta` inclusiva:** `fechaEmision` guarda fecha y hora, por lo que se
  compara con "menor que el día siguiente" para incluir el día completo.
- **`LEFT JOIN` a los detalles:** una factura sin ítems aparece con
  `cantidadItems: 0` en lugar de quedar fuera del reporte.
- **Estrategia de nombres física estándar:** se configura
  `PhysicalNamingStrategyStandardImpl` para reutilizar las tablas creadas por
  Hibernate 5 sin duplicar columnas.

## Conceptos aplicados

Inyección de dependencias por constructor · `@RestController` y `@GetMapping` ·
`@RequestParam` opcionales · conversión de fechas con `@DateTimeFormat` ·
`@PersistenceContext` · `@Transactional(readOnly = true)` · JPQL con expresión
de constructor, `COALESCE`, `COUNT` y `GROUP BY` · serialización JSON con
`@JsonFormat`.

## Autor

Giovanni — [github.com/giovanniclmb](https://github.com/giovanniclmb)