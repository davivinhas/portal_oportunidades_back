package com.example.portal_oportunidades_back.administration.controller;

import com.example.portal_oportunidades_back.administration.dto.AdminUserResponse;
import com.example.portal_oportunidades_back.administration.service.UserAdministrationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class UserAdministrationController {
    private final UserAdministrationService service;
    public UserAdministrationController(UserAdministrationService service) { this.service = service; }

    @GetMapping
    public Page<AdminUserResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(page, size); }

    @PutMapping("/{userId}/suspension")
    public ResponseEntity<Void> suspend(@PathVariable Long userId) { service.suspend(userId); return ResponseEntity.noContent().build(); }

    @DeleteMapping("/{userId}/suspension")
    public ResponseEntity<Void> reactivate(@PathVariable Long userId) { service.reactivate(userId); return ResponseEntity.noContent().build(); }
}
