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

La autenticación usa JWT y controla el acceso a las rutas según el rol de la sesión.

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

### Orientación inteligente

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/orientacion` | Analiza síntomas con Gemini y sugiere hasta tres servicios activos relacionados, junto con veterinarios que los ofrecen |

La ruta requiere un token de cliente (`Authorization: Bearer <token>`). El backend verifica que la mascota pertenezca al cliente autenticado y solo envía a Gemini la especie, edad, síntomas, duración y opciones del catálogo; no envía nombres, identificadores ni datos de contacto. La IA evalúa la urgencia según los síntomas y el tiempo que llevan presentes, y solo sugiere servicios activos razonablemente relacionados. La respuesta nunca acepta servicios ni profesionales fuera del catálogo activo. No es un diagnóstico y las señales de alarma muestran una indicación de buscar atención inmediata.

Configura `GEMINI_API_KEY` como variable secreta del backend. `GEMINI_MODEL` permite cambiar el modelo y usa `gemini-2.5-flash` por defecto. Sin clave, ante un error del proveedor o cuando Gemini no devuelve opciones válidas, la API marca `modoRespaldo` y busca relaciones entre síntomas y especialidades del catálogo (por ejemplo, picazón con dermatología, fracturas con cirugía u ortopedia y dolor abdominal con ecografía si está disponible). Para síntomas digestivos también prioriza consulta general; si no hay una opción relacionada, sugiere consulta general cuando esté activa y, si tampoco existe, no recomienda servicios. No se debe configurar la clave en el frontend ni versionarla.

Ejemplo del cuerpo:

```json
{
  "mascotaId": 12,
  "sintomas": "Presenta irritación en la piel desde ayer",
  "duracionValor": 1,
  "duracionUnidad": "dias"
}
```

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

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/register` | Crea la cuenta (rol `CLIENTE`) de un cliente ya registrado |
| `POST` | `/api/auth/login` | Valida correo y contraseña de un Cliente, Veterinario o Administrador y entrega un JWT |
| `POST` | `/api/auth/veterinario/demo` | Token del veterinario demo (revisiones académicas) |
| `GET` | `/api/auth/me` | Perfil del cliente autenticado |

Todas las cuentas viven en la tabla `usuario` (migración `V17`), con uno o varios roles (`CLIENTE`, `VETERINARIO`, `ADMINISTRADOR`) en `usuario_rol`. Las contraseñas se guardan con BCrypt. Las cuentas de `cliente_credencial` se migraron a `usuario` conservando su contraseña.

Para registrar una cuenta de cliente, el correo y el documento deben coincidir con un cliente ya registrado:

```json
{ "correo": "alex@gmail.com", "documentoIdentidad": "12345678", "password": "ClaveSegura123" }
```

Inicio de sesión (`POST /api/auth/login`):

```json
{ "correo": "alex@gmail.com", "password": "ClaveSegura123" }
```

```json
{
  "mensaje": "Inicio de sesión exitoso",
  "token": "<jwt>",
  "tipoToken": "Bearer",
  "expiraEnMs": 86400000,
  "rol": "CLIENTE",
  "roles": ["CLIENTE"]
}
```

`rol` es el rol principal (prioridad `ADMINISTRADOR` > `VETERINARIO` > `CLIENTE`) para redirigir al panel correspondiente. El JWT lleva como `subject` el correo y los claims `uid`, `roles`, `rol`, `tv` (versión de token) y `exp`. En Swagger, pulsa **Authorize** y pega únicamente el valor de `token`.

En cada petición el filtro JWT valida firma y expiración y vuelve a leer el usuario de la base de datos: si la cuenta está inactiva o su versión de token cambió, el token deja de ser válido y la API responde `401`.

| Código | Cuándo |
|---|---|
| `401` | Correo no registrado, contraseña errónea o cuenta inactiva. Siempre con el mensaje genérico `Credenciales incorrectas` |
| `429` | 5 intentos fallidos consecutivos para la misma cuenta o desde la misma IP. Se bloquean los intentos durante 15 minutos (incluso con la contraseña correcta). La respuesta trae el encabezado `Retry-After` y `segundosRestantes` |

Configuración:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `APP_JWT_SECRET` | valor de desarrollo | Secreto de firma. Obligatorio en entornos compartidos o de producción |
| `APP_JWT_EXPIRATION_MS` | `86400000` | Duración del token |
| `APP_LOGIN_MAX_INTENTOS` | `5` | Intentos fallidos consecutivos antes del bloqueo |
| `APP_LOGIN_BLOQUEO_MINUTOS` | `15` | Duración del bloqueo |

El control de intentos se guarda en memoria (suficiente para una sola instancia). Detrás de Render la IP real se toma de `X-Forwarded-For` (`server.forward-headers-strategy: native`).

Cuentas demo sembradas por las migraciones (contraseña `Demo2026!`; cámbiala en entornos compartidos):

| Correo | Rol |
|---|---|
| `carolina.restrepo@vetagenda.com` | `CLIENTE` |
| `camilo.duran@vetagenda.com` | `VETERINARIO` |
| `admin@vetagenda.com` | `ADMINISTRADOR` |

Los endpoints de catálogo, clientes, veterinarios, servicios y mascotas siguen siendo públicos. Las rutas de reserva y reprogramación de citas requieren un JWT válido.

### Administración de usuarios

Requieren el JWT de un administrador activo (`ROLE_ADMINISTRADOR`): sin token responden `401` y con otro rol `403`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/admin/usuarios?buscar=&rol=&activo=` | Lista los usuarios (filtros opcionales por nombre o correo, rol y estado) |
| `PATCH` | `/api/admin/usuarios/{id}/estado` | Activa o inactiva una cuenta. Cuerpo: `{ "activo": false }` |
| `PATCH` | `/api/admin/usuarios/{id}/roles` | Reemplaza los roles. Cuerpo: `{ "roles": ["VETERINARIO", "ADMINISTRADOR"] }` |
| `GET` | `/api/admin/usuarios/{id}/auditoria` | Historial de cambios: acción, valor anterior y nuevo, administrador y fecha |

Reglas:

- Al inactivar una cuenta se incrementa su versión de token: sus sesiones abiertas quedan desconectadas en la siguiente petición (`401`), y siguen invalidadas aunque luego se reactive.
- Los cambios de roles aplican de inmediato, porque el filtro JWT lee los roles de la base en cada petición.
- Cada cambio de estado o de roles queda registrado en `usuario_auditoria` (migración `V18`) con el administrador que lo hizo, la fecha y la hora.
- `409`: un administrador no puede desactivarse a sí mismo ni retirarse su propio rol de administrador, y ningún cambio puede dejar la clínica sin al menos un administrador activo. Las filas de los administradores activos se bloquean durante el cambio, así que dos administradores no pueden desactivarse mutuamente al mismo tiempo.
- `400`: sin roles, rol inexistente, o rol `VETERINARIO`/`CLIENTE` para una cuenta sin veterinario/cliente vinculado.

### Portal del veterinario: disponibilidad

Requieren el JWT de un veterinario activo (`ROLE_VETERINARIO`): sin token responden `401` y con el token de un cliente `403`. El veterinario siempre se toma del token, nunca del cuerpo. Los días viajan en español (`LUNES`, `MARTES`, `MIERCOLES`, `JUEVES`, `VIERNES`, `SABADO`, `DOMINGO`) y las horas como `HH:mm`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/veterinario-portal/disponibilidad` | Devuelve los siete días con sus franjas activas |
| `PUT` | `/api/veterinario-portal/disponibilidad` | Reemplaza todas las franjas (los ids cambian). Agrupa los días repetidos e ignora los días con `activo: false` |
| `POST` | `/api/veterinario-portal/disponibilidad/franjas` | Agrega una franja sin tocar las demás (`201`) |
| `PUT` | `/api/veterinario-portal/disponibilidad/franjas/{franjaId}` | Edita día y horas de una franja propia |
| `DELETE` | `/api/veterinario-portal/disponibilidad/franjas/{franjaId}` | Elimina una franja propia |

Ejemplo de alta y de edición (mismo cuerpo):

```bash
curl -X POST http://localhost:8080/api/veterinario-portal/disponibilidad/franjas \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{ "dia": "LUNES", "horaInicio": "14:00", "horaFin": "16:00" }'
```

```json
{ "id": 21, "dia": "LUNES", "horaInicio": "14:00:00", "horaFin": "16:00:00" }
```

Reglas y errores:

| Código | Cuándo |
|---|---|
| `400` | Día no válido, horas faltantes o mal formadas, o la hora final no es posterior a la inicial |
| `404` | La franja no existe o pertenece a otro veterinario |
| `409` | La franja se traslapa con otra franja activa del mismo veterinario y día |

Las franjas se manejan como intervalos `[inicio, fin)`: dos franjas contiguas (08:00-12:00 y 12:00-14:00) son válidas. La regla también está protegida en la base de datos con la restricción `no_traslape_horario` (migración `V15`), por lo que dos solicitudes simultáneas no pueden dejar franjas solapadas.

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
