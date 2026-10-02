package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/opportunities")
@Tag(name = "Opportunities", description = "Opportunity management without authentication in this phase")
@ApiResponse(responseCode = "400", description = "Invalid request, filter or cursor",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
public class OpportunityController {
    private final OpportunityService service;

    public OpportunityController(OpportunityService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a complete draft opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft created"),
            @ApiResponse(responseCode = "404", description = "Recruiter not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody CreateOpportunityRequest request) {
        OpportunityResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List non-deleted opportunities using a forward cursor",
            description = "Sorted by createdAt DESC and id DESC. Keep filters unchanged when sending nextCursor.")
    public OpportunityCursorResponse list(
            @Parameter(description = "Opaque nextCursor from the previous response")
            @RequestParam(name = "cursor", required = false) @Size(max = 1024) String cursor,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(name = "title", required = false) @Size(max = 200) String title,
            @RequestParam(name = "modality", required = false) OpportunityModality modality,
            @RequestParam(name = "status", required = false) OpportunityStatus status,
            @RequestParam(name = "recruiterId", required = false) @Positive Long recruiterId) {
        return service.list(new OpportunityFilter(title, modality, status, recruiterId), cursor, limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a non-deleted opportunity")
    @ApiResponse(responseCode = "404", description = "Opportunity not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public OpportunityResponse findById(@PathVariable("id") @Positive Long id) { return service.findById(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Replace editable details of a draft or published opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse update(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody UpdateOpportunityRequest request) { return service.update(id, request); }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a draft whose registration end is in the future")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State, deadline or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse publish(@PathVariable("id") @Positive Long id) { return service.publish(id); }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a published opportunity")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "State or concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpportunityResponse close(@PathVariable("id") @Positive Long id) { return service.close(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete an opportunity without removing its applications")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opportunity soft deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "Opportunity not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(@PathVariable("id") @Positive Long id) { service.delete(id); }
}
