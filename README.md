# Veterinaria API

Backend REST para el sistema de gestión veterinaria desarrollado en la Fábrica Escuela 2026-2. Actualmente permite consultar el catálogo, registrar veterinarios, clientes y servicios, y registrar o consultar las mascotas asociadas a un cliente.

## Tecnologías

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Bean Validation
- SpringDoc OpenAPI / Swagger UI
- Maven

El proyecto incluye las dependencias de JWT, pero la autenticación y la emisión de tokens todavía están pendientes de implementación.

## Requisitos

### Con Docker (recomendado)
Solo necesitas:
- **Docker Desktop** con Docker Compose
- **Git**
- Tener clonado el repo [vetagenda-frontend](https://github.com/EBV08-Fabrica-Escuela-2026-2/vetagenda-frontend) en una carpeta hermana (el `docker-compose.yml` vive allí)

### En modo desarrollo local
- JDK 21
- Maven 3.9+
- PostgreSQL 16

## Configuración local

La configuración actual se encuentra en `src/main/resources/application.yml`:

| Propiedad | Valor de desarrollo |
|---|---|
| Puerto de la API | `8080` |
| URL de PostgreSQL | `jdbc:postgresql://localhost:5432/veterinaria` |
| Usuario | `postgres` |
| Contraseña | `123456` |
| Perfil activo | `dev` |

## Ejecutar la aplicación

### Opción 1: Docker Compose (recomendado)

El orquestador vive en el repo [vetagenda-frontend](https://github.com/EBV08-Fabrica-Escuela-2026-2/vetagenda-frontend). Clona ambos repos en carpetas hermanas y ejecuta:

```bash
# Desde la carpeta VetAgenda/
docker compose up --build

# Siguientes veces (sin reconstruir)
docker compose up
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 (DB: `vetagenda`) |

```bash
# Detener
docker compose down

# Detener y borrar la BD (necesario si cambia el esquema)
docker compose down -v
```

### Opción 2: Desarrollo local

Crea la base de datos `veterinaria` en PostgreSQL y aplica los scripts en orden:

```bash
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V1__create_tables_and_seed_data.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V2__add_documento_correo_veterinario.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V3__create_cliente.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V4__create_mascota.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V5__add_campos_veterinario.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V6__add_mascota_sexo.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V7__add_duracion_minutos_servicio.sql
```

Luego inicia la aplicación:

```bash
mvn spring-boot:run
```

La API queda disponible en [http://localhost:8080](http://localhost:8080).

## Endpoints disponibles

### Catálogo

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/catalogo` | Lista los veterinarios activos con sus servicios activos |
| `GET` | `/api/catalogo/veterinarios/{id}` | Consulta el detalle de un veterinario |

### Clientes

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/clientes` | Registra un cliente activo |

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana Gómez",
    "documentoIdentidad": "1020304050",
    "telefono": "3001234567",
    "correo": "ana@example.com",
    "direccion": "Calle 10 # 20-30"
  }'
```

### Veterinarios

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/veterinarios` | Registra un veterinario activo y valida documento y correo duplicados |

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/veterinarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Laura Gómez",
    "documentoIdentidad": "123456789",
    "telefono": "3001234567",
    "correo": "laura@example.com",
    "direccion": "Calle 10 # 20-30",
    "horarioAtencion": "Lunes a viernes de 8:00 a 17:00"
  }'
```

### Servicios

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/servicios` | Registra un servicio para un veterinario existente y activo |

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/servicios \
  -H "Content-Type: application/json" \
  -d '{
    "veterinarioId": 1,
    "nombre": "Consulta general",
    "descripcion": "Valoración médica de la mascota",
    "precio": 65000
  }'
```

### Mascotas

El cliente se identifica mediante el parámetro de consulta `documentoIdentidad`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/mascotas?documentoIdentidad={documento}` | Registra una mascota para un cliente existente y activo |
| `GET` | `/api/mascotas?documentoIdentidad={documento}` | Lista las mascotas asociadas al cliente |

Ejemplo de registro:

```bash
curl -X POST "http://localhost:8080/api/mascotas?documentoIdentidad=1020304050" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Luna",
    "especie": "Perro",
    "raza": "Labrador",
    "edad": 4,
    "observaciones": "Vacunación al día"
  }'
```

Ejemplo de consulta:

```bash
curl "http://localhost:8080/api/mascotas?documentoIdentidad=1020304050"
```

### Autenticación

| Método | Ruta | Estado |
|---|---|---|
| `POST` | `/api/auth/login` | Pendiente; devuelve un token de ejemplo |
| `POST` | `/api/auth/register` | Pendiente de implementación |

Los endpoints actuales de catálogo, clientes, veterinarios, servicios y mascotas son públicos. Todavía no existe un filtro JWT que proteja las solicitudes.

## Documentación de la API

Con la aplicación en ejecución:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Especificación OpenAPI: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Pruebas

Ejecute la suite con:

```bash
mvn test
```

Las pruebas usan el perfil `test` y una base de datos H2 en memoria. Actualmente existe cobertura de controlador para el registro y la consulta de mascotas.

## Estructura principal

```text
src/
├── main/
│   ├── java/com/veterinaria/
│   │   ├── config/       # Seguridad y CORS
│   │   ├── controller/   # Endpoints REST
│   │   ├── dto/          # Datos de entrada y salida
│   │   ├── exception/    # Manejo global de errores
│   │   ├── model/        # Entidades JPA
│   │   ├── repository/   # Acceso a datos
│   │   ├── service/      # Interfaces y lógica de negocio
│   │   └── util/         # Utilidades de formato
│   └── resources/
│       ├── application.yml
│       └── db/migration/ # Scripts SQL versionados
└── test/
    ├── java/             # Pruebas automatizadas
    └── resources/        # Configuración del perfil test
```

## Estado actual

Implementado:

- Consulta del catálogo de veterinarios y servicios.
- Registro de veterinarios con validaciones y control de duplicados.
- Registro de clientes con validaciones y control de duplicados.
- Registro de servicios asociados a veterinarios.
- Registro y consulta de mascotas por documento del cliente.
- Respuestas uniformes para errores de validación y recursos no encontrados.
- Documentación interactiva con Swagger UI.
