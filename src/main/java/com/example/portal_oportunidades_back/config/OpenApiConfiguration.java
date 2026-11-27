package com.example.portal_oportunidades_back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean OpenAPI openAPI() {
        return new OpenAPI().components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("access_token")));
    }

    @Bean OpenApiCustomizer csrfHeaderDocumentation() {
        return api -> {
            if (api.getPaths() == null) return;
            api.getPaths().values().forEach(path -> {
                addCsrfHeader(path.getPost());
                addCsrfHeader(path.getPut());
                addCsrfHeader(path.getPatch());
                addCsrfHeader(path.getDelete());
            });
        };
    }

    private void addCsrfHeader(Operation operation) {
        if (operation == null) return;
        operation.addParametersItem(new Parameter().name("X-XSRF-TOKEN").in("header").required(true)
                .description("Copy the token returned by GET /api/auth/csrf; keep its cookie in the browser.")
                .schema(new StringSchema()));
    }
}
