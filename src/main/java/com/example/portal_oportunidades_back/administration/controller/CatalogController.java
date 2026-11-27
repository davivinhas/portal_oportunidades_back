package com.example.portal_oportunidades_back.administration.controller;

import com.example.portal_oportunidades_back.administration.dto.*;
import com.example.portal_oportunidades_back.administration.service.CatalogAdministrationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogs")
public class CatalogController {
    private final CatalogAdministrationService service;
    public CatalogController(CatalogAdministrationService service) { this.service = service; }
    @GetMapping("/{category}")
    public List<CatalogItemResponse> list(@PathVariable String category,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(category, includeInactive);
    }
    @PostMapping("/{category}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public CatalogItemResponse create(@PathVariable String category, @Valid @RequestBody CatalogItemRequest request) {
        return service.create(category, request);
    }
    @PatchMapping("/{category}/{code}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public CatalogItemResponse update(@PathVariable String category, @PathVariable String code,
            @Valid @RequestBody CatalogItemUpdateRequest request) {
        return service.update(category, code, request);
    }
}
