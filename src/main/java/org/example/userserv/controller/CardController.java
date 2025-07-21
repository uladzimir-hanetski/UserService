package org.example.userserv.controller;

import lombok.RequiredArgsConstructor;
import org.example.userserv.dto.CardRequest;
import org.example.userserv.dto.CardResponse;
import org.example.userserv.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable("id") long id) {
        return ResponseEntity.ok(cardService.findById(id));
    }

    @PostMapping("/ids")
    public ResponseEntity<List<CardResponse>> getCardsByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(cardService.findByIds(ids));
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @RequestBody @Validated(CardRequest.CreateValidation.class) CardRequest cardRequest) {
        return ResponseEntity.ok(cardService.create(cardRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> updateCard(@PathVariable("id") long id,
                                                   @RequestBody @Validated(CardRequest.UpdateValidation.class)
                                                   CardRequest cardRequest) {
        return ResponseEntity.ok(cardService.update(id, cardRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable("id") long id) {
        cardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
