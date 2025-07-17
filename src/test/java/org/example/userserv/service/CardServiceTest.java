package org.example.userserv.service;

import org.example.userserv.dto.CardRequest;
import org.example.userserv.dto.CardResponse;
import org.example.userserv.entity.Card;
import org.example.userserv.exception.CardNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cache.Cache;
import org.example.userserv.entity.User;
import org.example.userserv.exception.ValueAlreadyExistsException;
import org.example.userserv.mapper.CardMapper;
import org.example.userserv.repository.CardRepository;
import org.example.userserv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCacheManager;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {
    @Mock
    private RedisCacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    private final CardRequest cardRequest = new CardRequest();
    private final Card card = new Card();
    private final CardResponse cardResponse = new CardResponse();
    private final User user = new User();

    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void initialize() {
        LocalDate expiryDate = LocalDate.of(2027, 1, 1);

        user.setId(uuid);
        user.setEmail("test@example.com");

        card.setId(1L);
        card.setNumber("1111");
        card.setHolder("Holder");
        card.setExpirationDate(expiryDate);
        card.setUser(user);

        cardRequest.setNumber("1111");
        cardRequest.setHolder("Holder");
        cardRequest.setExpirationDate(expiryDate);
        cardRequest.setUserId(uuid);

        cardResponse.setId(1L);
        cardResponse.setNumber("1111");
        cardResponse.setHolder("Holder");
        cardResponse.setExpirationDate(expiryDate);
        cardResponse.setUserId(uuid);
    }

    @Test
    void testCreateCard() {
        cache.put(user.getEmail(), user);

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));
        when(cardRepository.existsByNumber("1111")).thenReturn(false);
        when(cardMapper.toEntity(cardRequest)).thenReturn(card);
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);
        when(cacheManager.getCache("users")).thenReturn(cache);

        CardResponse result = cardService.create(cardRequest);

        assertEquals("1111", result.getNumber());
    }

    @Test
    void testCreateCardNumberAlreadyExists() {
        when(cardRepository.existsByNumber("1111")).thenReturn(true);

        assertThrows(ValueAlreadyExistsException.class, () -> cardService.create(cardRequest));
    }

    @Test
    void testFindById() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.findById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void testFindByIdNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.findById(1L));
    }

    @Test
    void testUpdateCard() {
            CardRequest updateRequest = new CardRequest();
            updateRequest.setNumber("New number");
            updateRequest.setHolder("New holder");
            updateRequest.setExpirationDate(LocalDate.of(2030,1,1));

            when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(cardRepository.existsByNumber("New number")).thenReturn(false);

            Card updatedCard = new Card();
            updatedCard.setId(1L);
            updatedCard.setNumber("New number");
            updatedCard.setHolder("New holder");
            updatedCard.setExpirationDate(updateRequest.getExpirationDate());
            updatedCard.setUser(user);

            CardResponse updatedResponse = new CardResponse();
            updatedResponse.setId(1L);
            updatedResponse.setNumber("New number");
            updatedResponse.setHolder("New holder");
            updatedResponse.setExpirationDate(updateRequest.getExpirationDate());
            updatedResponse.setUserId(uuid);

            when(cardRepository.save(any(Card.class))).thenReturn(updatedCard);
            when(cardMapper.toResponse(updatedCard)).thenReturn(updatedResponse);
            when(cacheManager.getCache("users")).thenReturn(cache);

            CardResponse result = cardService.update(1L, updateRequest);

            assertEquals("New number", result.getNumber());
            assertEquals("New holder", result.getHolder());
            assertEquals(updateRequest.getExpirationDate(), result.getExpirationDate());
        }

    @Test
    void testUpdateCardNumberAlreadyExists() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.existsByNumber("New number")).thenReturn(true);

        CardRequest updateRequest = new CardRequest();
        updateRequest.setNumber("New number");

        assertThrows(ValueAlreadyExistsException.class, () -> cardService.update(1L, updateRequest));
    }

    @Test
    void testFindByIds() {
        List<Long> ids = List.of(1L, 2L);
        List<Card> cards = List.of(card, new Card());

        when(cardRepository.findByIds(ids)).thenReturn(cards);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        List<CardResponse> result = cardService.findByIds(ids);

        assertEquals(2, result.size());
    }

    @Test
    void testFindByIdsEmptyList() {
        List<CardResponse> result = cardService.findByIds(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cacheManager.getCache("users")).thenReturn(cache);

        cardService.delete(1L);

        verify(cardRepository).deleteById(1L);
        verify(cache).evict(card.getUser().getId());
    }

    @Test
    void testDeleteCardNotFound() {
        when(cardRepository.findById(1L)).thenThrow(CardNotFoundException.class);

        assertThrows(CardNotFoundException.class, () -> cardService.delete(1L));
    }
}