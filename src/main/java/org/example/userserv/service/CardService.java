package org.example.userserv.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.userserv.dto.CardRequest;
import org.example.userserv.dto.CardResponse;
import org.example.userserv.entity.Card;
import org.example.userserv.entity.User;
import org.example.userserv.exception.CardNotFoundException;
import org.example.userserv.exception.UserNotFoundException;
import org.example.userserv.exception.ValueAlreadyExistsException;
import org.example.userserv.mapper.CardMapper;
import org.example.userserv.repository.CardRepository;
import org.example.userserv.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {
    private static final String CACHE_USERS = "users";
    private static final String CACHE_CARDS = "cards";
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;
    private final RedisCacheManager cacheManager;

    @CacheEvict(value = CACHE_USERS, key = "#result.userId")
    public CardResponse create(CardRequest cardRequest) {
        if (cardRepository.existsByNumber(cardRequest.getNumber()))
            throw new ValueAlreadyExistsException("number", cardRequest.getNumber());

        User user = userRepository.findById(cardRequest.getUserId())
                .orElseThrow(UserNotFoundException::new);
        Card card = cardMapper.toEntity(cardRequest);
        card.setUser(user);

        Cache cache = cacheManager.getCache(CACHE_USERS);
        if (cache != null) cache.evict(card.getUser().getEmail());

        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Cacheable(value = CACHE_CARDS, key = "#id")
    public CardResponse findById(Long id) {
        return cardRepository.findById(id).map(cardMapper::toResponse)
                .orElseThrow(CardNotFoundException::new);
    }

    public List<CardResponse> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return cardRepository.findByIds(ids).stream().map(cardMapper::toResponse).toList();
    }

    @CachePut(value = CACHE_CARDS, key = "#id")
    @CacheEvict(value = CACHE_USERS, key = "#result.userId")
    @Transactional
    public CardResponse update(Long id, CardRequest cardRequest) {
        Card card = cardRepository.findById(id).orElseThrow(CardNotFoundException::new);

        if (cardRequest.getHolder() != null) card.setHolder(cardRequest.getHolder());
        if (cardRequest.getExpirationDate() != null) card.setExpirationDate(cardRequest.getExpirationDate());
        if (cardRequest.getNumber() != null) {
            if (cardRepository.existsByNumber(cardRequest.getNumber())
                    && !cardRequest.getNumber().equals(card.getNumber()))
                throw new ValueAlreadyExistsException("number", cardRequest.getNumber());
            card.setNumber(cardRequest.getNumber());
        }

        Cache cache = cacheManager.getCache(CACHE_USERS);
        if (cache != null) cache.evict(card.getUser().getEmail());

        return cardMapper.toResponse(cardRepository.save(card));
    }

    @CacheEvict(value = CACHE_CARDS, key = "#id")
    @Transactional
    public void delete(Long id) {
        Card card = cardRepository.findById(id).orElseThrow(CardNotFoundException::new);
        Cache cache = cacheManager.getCache(CACHE_USERS);
        if (cache != null) {
            cache.evict(card.getUser().getId());
            cache.evict(card.getUser().getEmail());
        }

        cardRepository.deleteById(id);
    }
}
