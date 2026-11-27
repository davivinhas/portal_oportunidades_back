package com.example.portal_oportunidades_back.administration.entity;

import jakarta.persistence.*;

@Entity
@Table(schema = "catalogo", name = "item", uniqueConstraints = @UniqueConstraint(name = "uk_catalog_category_code", columnNames = {"categoria", "codigo"}))
public class CatalogItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "categoria", nullable = false, length = 40) private String category;
    @Column(name = "codigo", nullable = false, length = 60) private String code;
    @Column(name = "rotulo", nullable = false, length = 120) private String label;
    @Column(name = "ativo", nullable = false) private boolean active = true;
    protected CatalogItem() { }
    public CatalogItem(String category, String code, String label) {
        this.category = category; this.code = code; rename(label);
    }
    public void rename(String label) {
        if (label == null || label.isBlank() || label.trim().length() > 120) throw new IllegalArgumentException("Label must contain 1 to 120 characters");
        this.label = label.trim();
    }
    public void setActive(boolean active) { this.active = active; }
    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public boolean isActive() { return active; }
}
