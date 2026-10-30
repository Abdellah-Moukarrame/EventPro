package org.example.eventpro.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.example.eventpro.dto.request.PromotionCreateRequest;
import org.example.eventpro.dto.request.PromotionUpdateRequest;
import org.example.eventpro.dto.response.PromotionResponseDTO;
import org.example.eventpro.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(
        name = "Promotions" ,
        description =  "Gestions des promos des evenements"
)

public class PromotionController {
    private final PromotionService promotionService ;
    @PostMapping
    @Operation(summary = "Creer une promotions")
    public ResponseEntity<PromotionResponseDTO>create(
            @Valid @RequestBody PromotionCreateRequest request
            ){
        PromotionResponseDTO response = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une promotion")
    public ResponseEntity<PromotionResponseDTO>update(
            @PathVariable Long id ,
            @Valid @RequestBody PromotionUpdateRequest request
    ){
        return ResponseEntity.ok(
                promotionService.update(id,request)
        );
    }

    @GetMapping("{/id}")
    @Operation(summary = "Recuperer une promo par id")
    public ResponseEntity<PromotionResponseDTO>findById(@PathVariable Long id){
        return ResponseEntity.ok(promotionService.findById(id));
    }
    @GetMapping("/evenement/{evenementId}")
    @Operation(summary = "afficher les promotions d'un evenement")
    public ResponseEntity<List<PromotionResponseDTO>>findByEvenementId(@PathVariable Long evenementId){
        return ResponseEntity.ok(promotionService.findByEvenementId(evenementId));
    }

    @DeleteMapping("{/id}")
    @Operation(summary = "supprimer un evenment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
