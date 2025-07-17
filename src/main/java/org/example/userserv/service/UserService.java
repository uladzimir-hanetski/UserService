package org.example.userserv.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.userserv.dto.UserRequest;
import org.example.userserv.dto.UserResponse;
import org.example.userserv.entity.Card;
import org.example.userserv.entity.User;
import org.example.userserv.exception.UserNotFoundException;
import org.example.userserv.exception.ValueAlreadyExistsException;
import org.example.userserv.mapper.UserMapper;
import org.example.userserv.repository.CardRepository;
import org.example.userserv.repository.UserRepository;
import org.example.userserv.util.SecurityUtil;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String CACHE_USERS = "users";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CardRepository cardRepository;
    private final RedisCacheManager cacheManager;
    private final SecurityUtil securityUtil;

    public UserResponse create(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail()))
            throw new ValueAlreadyExistsException("email", userRequest.getEmail());

        User user = userMapper.toEntity(userRequest);
        user.setId(securityUtil.getCurrentUserId());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Cacheable(value = CACHE_USERS, key = "#id")
    public UserResponse findById(UUID id) {
        if (!securityUtil.getCurrentUserId().equals(id))
            throw new AccessDeniedException("Access denied");

        return userRepository.findById(id).map(userMapper::toResponse)
                .orElseThrow(UserNotFoundException::new);
    }

    public List<UserResponse> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return userRepository.findByIds(ids).stream().map(userMapper::toResponse).toList();
    }

    @Cacheable(value = CACHE_USERS, key = "#email")
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toResponse)
                .orElseThrow(UserNotFoundException::new);
    }

    @CachePut(value = CACHE_USERS, key = "#id")
    @Transactional
    public UserResponse update(UUID id, UserRequest userRequest) {
        if (!securityUtil.getCurrentUserId().equals(id))
            throw new AccessDeniedException("Access denied");

        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        updateUserFields(user, userRequest);

        return userMapper.toResponse(userRepository.save(user));
    }

    private void updateUserFields(User user, UserRequest userRequest) {
        if (userRequest.getBirthDate() != null) {
            user.setBirthDate(userRequest.getBirthDate());
        }
        if (userRequest.getName() != null) {
            user.setName(userRequest.getName());
        }
        if (userRequest.getSurname() != null) {
            user.setSurname(userRequest.getSurname());
        }
        if (userRequest.getEmail() != null) {
            updateUserEmail(user, userRequest.getEmail());
        }
    }

    private void updateUserEmail(User user, String email) {
        if (isUserEmailUnique(user, email)) {
            Optional.ofNullable(cacheManager.getCache(CACHE_USERS))
                    .ifPresent(c -> c.evict(user.getEmail()));

            user.setEmail(email);
        }
    }

    private boolean isUserEmailUnique(User user, String email) {
        if (userRepository.existsByEmail(email) && !user.getEmail().equals(email)) {
            throw new ValueAlreadyExistsException("email", email);
        }

        return true;
    }

    @CacheEvict(value = CACHE_USERS, key = "#id")
    @Transactional
    public void delete(UUID id) {
        if (!securityUtil.getCurrentUserId().equals(id))
            throw new AccessDeniedException("Access denied");

        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        List<Card> cards = cardRepository.findByUserId(id);
        Cache cardsCache = cacheManager.getCache("cards");
        if (cardsCache != null) {
            for (Card card : cards) cardsCache.evict(card.getId());
        }

        Optional.ofNullable(cacheManager.getCache(CACHE_USERS))
                .ifPresent(c -> c.evict(user.getEmail()));

        userRepository.deleteById(id);
    }
}
