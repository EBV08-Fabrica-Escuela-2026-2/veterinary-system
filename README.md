# Veterinaria API

Backend REST para el sistema de gestión veterinaria desarrollado en la Fábrica Escuela 2026-2. Actualmente permite consultar el catálogo de veterinarios, registrar clientes y servicios, y registrar o consultar las mascotas asociadas a un cliente.

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

- JDK 21
- Maven 3.9+
- PostgreSQL
- Una base de datos llamada `veterinaria`

> Nota: el repositorio contiene los scripts `mvnw` y `mvnw.cmd`, pero no incluye actualmente los archivos de `.mvn/wrapper`. Hasta que se complete el Maven Wrapper, use una instalación local de Maven.

## Configuración local

La configuración actual se encuentra en `src/main/resources/application.yml`:

| Propiedad | Valor de desarrollo |
|---|---|
| Puerto de la API | `8080` |
| URL de PostgreSQL | `jdbc:postgresql://localhost:5432/veterinaria` |
| Usuario | `postgres` |
| Contraseña | `123456` |
| Perfil activo | `dev` |

Antes de iniciar la aplicación, cree la base de datos y ejecute en orden los scripts SQL ubicados en `src/main/resources/db/migration`:

```bash
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V1__create_tables_and_seed_data.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V3__create_cliente.sql
psql -U postgres -d veterinaria -f src/main/resources/db/migration/V4__create_mascota.sql
```

> Nota: los archivos usan la convención de Flyway, pero Flyway todavía no está incluido como dependencia. Por ahora las migraciones deben aplicarse manualmente. El script `setup-db.sh` solo carga `V1` y usa las credenciales `vetsa_user` / `vetsa_password`; si se utiliza, también se deben aplicar `V3` y `V4` y ajustar `application.yml`.

## Ejecutar la aplicación

En la raíz del repositorio:

```bash
mvn spring-boot:run
```

La API quedará disponible en [http://localhost:8080](http://localhost:8080).

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

Los endpoints actuales de catálogo, clientes, servicios y mascotas son públicos. Todavía no existe un filtro JWT que proteja las solicitudes.

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
- Registro de clientes con validaciones y control de duplicados.
- Registro de servicios asociados a veterinarios.
- Registro y consulta de mascotas por documento del cliente.
- Respuestas uniformes para errores de validación y recursos no encontrados.
- Documentación interactiva con Swagger UI.

Pendiente:

- Autenticación real y validación de JWT.
- Automatización de las migraciones de base de datos.
- Registro y administración de veterinarios.
- Gestión de citas veterinarias.
- Ampliación de la cobertura de pruebas.
