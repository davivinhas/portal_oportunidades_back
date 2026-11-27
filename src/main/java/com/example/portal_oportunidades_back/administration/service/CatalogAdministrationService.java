package com.example.portal_oportunidades_back.administration.service;

import com.example.portal_oportunidades_back.administration.dto.*;
import com.example.portal_oportunidades_back.administration.entity.CatalogItem;
import com.example.portal_oportunidades_back.administration.repository.CatalogItemRepository;
import com.example.portal_oportunidades_back.exception.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogAdministrationService {
    public static final String OPPORTUNITY_MODALITY = "OPPORTUNITY_MODALITY";
    public static final String RECRUITER_TYPE = "RECRUITER_TYPE";
    private final CatalogItemRepository items;
    public CatalogAdministrationService(CatalogItemRepository items) { this.items = items; }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> list(String category, boolean includeInactive) {
        validateCategory(category);
        return (includeInactive ? items.findAllByCategoryOrderByLabelAsc(category)
                : items.findAllByCategoryAndActiveTrueOrderByLabelAsc(category)).stream().map(this::response).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CatalogItemResponse create(String category, CatalogItemRequest request) {
        validateCategory(category);
        if (items.existsByCategoryAndCode(category, request.code())) throw new ConflictException("Catalog code already exists");
        return response(items.save(new CatalogItem(category, request.code(), request.label())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CatalogItemResponse update(String category, String code, CatalogItemUpdateRequest request) {
        validateCategory(category);
        CatalogItem item = items.findByCategoryAndCode(category, code)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found"));
        item.rename(request.label());
        if (request.active() != null) item.setActive(request.active());
        return response(item);
    }

    private void validateCategory(String category) {
        if (!OPPORTUNITY_MODALITY.equals(category) && !RECRUITER_TYPE.equals(category))
            throw new BadRequestException("Unsupported catalog category");
    }
    private CatalogItemResponse response(CatalogItem item) {
        return new CatalogItemResponse(item.getId(), item.getCategory(), item.getCode(), item.getLabel(), item.isActive());
    }
}
