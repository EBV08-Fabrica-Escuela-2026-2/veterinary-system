-- HU03 - Iniciar sesión
-- Cuenta de acceso unificada para Cliente, Veterinario y Administrador.
-- Reemplaza a cliente_credencial (sus datos se migran aquí; la tabla queda
-- obsoleta y sin uso en el código, se puede eliminar en una migración posterior).

CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    -- Se incrementa para invalidar los JWT ya emitidos (cierre de sesión forzado).
    version_token INTEGER NOT NULL DEFAULT 0,
    cliente_id BIGINT UNIQUE REFERENCES cliente(id) ON DELETE CASCADE,
    veterinario_id BIGINT UNIQUE REFERENCES veterinario(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_usuario_correo_lower ON usuario (LOWER(correo));

CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol VARCHAR(20) NOT NULL,
    PRIMARY KEY (usuario_id, rol),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('CLIENTE', 'VETERINARIO', 'ADMINISTRADOR'))
);

-- 1. Clientes que ya tenían cuenta (cliente_credencial) conservan su contraseña.
INSERT INTO usuario (nombre, correo, password_hash, activo, cliente_id, created_at)
SELECT c.nombre, c.correo, cc.password_hash, c.activo, c.id, cc.created_at
FROM cliente_credencial cc
JOIN cliente c ON c.id = cc.cliente_id;

INSERT INTO usuario_rol (usuario_id, rol)
SELECT u.id, 'CLIENTE'
FROM usuario u
WHERE u.cliente_id IS NOT NULL;

-- 2. Veterinario demo (misma contraseña demo que Carolina: Demo2026!).
INSERT INTO usuario (nombre, correo, password_hash, activo, veterinario_id)
SELECT v.nombre, v.correo, '$2y$10$RnNxY7Vyu9xDwJk0wjXKYeht3z7GDYVCsM0HEOWQh.E96j/ryyxiS', TRUE, v.id
FROM veterinario v
WHERE LOWER(v.correo) = 'camilo.duran@vetagenda.com'
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE LOWER(u.correo) = LOWER(v.correo));

INSERT INTO usuario_rol (usuario_id, rol)
SELECT u.id, 'VETERINARIO'
FROM usuario u
WHERE u.veterinario_id IS NOT NULL;

-- 3. Administrador inicial (contraseña demo Demo2026!; cambiarla en entornos compartidos).
INSERT INTO usuario (nombre, correo, password_hash, activo)
SELECT 'Administrador VetAgenda', 'admin@vetagenda.com',
       '$2y$10$RnNxY7Vyu9xDwJk0wjXKYeht3z7GDYVCsM0HEOWQh.E96j/ryyxiS', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE LOWER(u.correo) = 'admin@vetagenda.com');

INSERT INTO usuario_rol (usuario_id, rol)
SELECT u.id, 'ADMINISTRADOR'
FROM usuario u
WHERE LOWER(u.correo) = 'admin@vetagenda.com';
