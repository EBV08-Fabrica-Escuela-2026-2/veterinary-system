package com.veterinaria.controller;

import com.veterinaria.dto.CatalogoResponseDTO;
import com.veterinaria.dto.VeterinarioDetalleDTO;
import com.veterinaria.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Servicios", description = "API para consultar el catálogo de servicios de la veterinaria")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    @Operation(
        summary = "Consultar catálogo completo",
        description = "Retorna la lista de todos los veterinarios con sus servicios y precios en COP",
        responses = {
            @ApiResponse(responseCode = "200", description = "Catálogo obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
        }
    )
    public ResponseEntity<CatalogoResponseDTO> consultarCatalogo() {
        CatalogoResponseDTO catalogo = catalogoService.consultarCatalogo();
        return ResponseEntity.ok(catalogo);
    }

    @GetMapping("/veterinarios/{id}")
    @Operation(
        summary = "Consultar detalle de veterinario",
        description = "Retorna la ficha completa de un veterinario con su dirección, horario, teléfono y servicios",
        responses = {
            @ApiResponse(responseCode = "200", description = "Detalle obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Veterinario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
        }
    )
    public ResponseEntity<VeterinarioDetalleDTO> consultarDetalleVeterinario(
            @Parameter(description = "ID del veterinario a consultar") @PathVariable Long id) {
        VeterinarioDetalleDTO detalle = catalogoService.consultarDetalleVeterinario(id);
        return ResponseEntity.ok(detalle);
    }
}
