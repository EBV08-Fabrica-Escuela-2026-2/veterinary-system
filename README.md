# Veterinaria API

Backend API para el sistema de gestión de veterinarias. Proyecto de la Fábrica de Escuela 2026-2.

## Stack

- **Java 21** (LTS)
- **Spring Boot 4.0.3**
- **Spring Security** + JWT
- **Spring Data JPA** + PostgreSQL
- **Flyway** (migrations)
- **SpringDoc OpenAPI** (Swagger UI)

## Requisitos

- Java 21+
- PostgreSQL
- Maven

## Configuración

El archivo `application.yml` viene configurado para:

- Puerto: `8080`
- Base de datos: `postgresql://localhost:5435/veterinaria`


Para levantar la base de datos:

```bash
./setup-db.sh
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

## Endpoints

### Catálogo

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/catalogo` | Lista todos los veterinarios con sus servicios |
| GET | `/api/catalogo/veterinarios/{id}` | Detalle de un veterinario |

### Autenticación

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Login (pendiente implementación) |
| POST | `/api/auth/register` | Registro (pendiente implementación) |

### Documentación

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Estructura del proyecto

```
src/main/java/com/veterinaria/
├── config/           # SecurityConfig
├── controller/       # AuthController, CatalogoController
├── dto/              # Request/Response DTOs
├── exception/        # GlobalExceptionHandler, ResourceNotFoundException
├── model/            # Veterinario, Servicio
├── repository/       # JPA Repositories
├── service/          # CatalogoService + impl
└── util/             # COPCurrencyFormat

src/main/resources/
├── application.yml         # Config principal
├── application-dev.yml     # Config desarrollo
└── db/migration/           # Flyway migrations
```
