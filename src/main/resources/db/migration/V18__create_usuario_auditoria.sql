-- HU19 - Gestionar usuarios
-- Trazabilidad de los cambios de estado y de roles hechos por un administrador.

CREATE TABLE usuario_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    administrador_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    accion VARCHAR(30) NOT NULL,
    valor_anterior VARCHAR(255),
    valor_nuevo VARCHAR(255),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_auditoria_accion CHECK (accion IN ('CAMBIO_ESTADO', 'CAMBIO_ROLES'))
);

CREATE INDEX idx_usuario_auditoria_usuario_fecha ON usuario_auditoria (usuario_id, fecha DESC);
