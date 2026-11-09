package com.veterinaria.model;

/**
 * Roles de acceso a la plataforma. El orden define la prioridad para elegir el
 * rol principal de un usuario con varios roles (el de mayor prioridad va primero).
 */
public enum Rol {
    ADMINISTRADOR,
    VETERINARIO,
    CLIENTE
}
