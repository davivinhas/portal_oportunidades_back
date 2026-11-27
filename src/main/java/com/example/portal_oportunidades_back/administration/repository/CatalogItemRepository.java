package com.example.portal_oportunidades_back.administration.repository;

import com.example.portal_oportunidades_back.administration.entity.CatalogItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findAllByCategoryOrderByLabelAsc(String category);
    List<CatalogItem> findAllByCategoryAndActiveTrueOrderByLabelAsc(String category);
    Optional<CatalogItem> findByCategoryAndCode(String category, String code);
    boolean existsByCategoryAndCode(String category, String code);
    boolean existsByCategoryAndCodeAndActiveTrue(String category, String code);
}
