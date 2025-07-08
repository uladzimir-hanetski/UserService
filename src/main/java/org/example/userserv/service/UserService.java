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
public class UserService {
    private static final String CACHE_USERS = "users";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CardRepository cardRepository;
    private final RedisCacheManager cacheManager;

    public UserResponse create(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail()))
            throw new ValueAlreadyExistsException("email", userRequest.getEmail());

        return userMapper.toResponse(userRepository.save(userMapper.toEntity(userRequest)));
    }

    @Cacheable(value = CACHE_USERS, key = "#id")
    public UserResponse findById(Long id) {
        return userRepository.findById(id).map(userMapper::toResponse)
                .orElseThrow(UserNotFoundException::new);
    }

    public List<UserResponse> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return userRepository.findByIds(ids).stream().map(userMapper::toResponse).toList();
    }

    @Cacheable(value = CACHE_USERS, key = "#email")
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toResponse)
                .orElseThrow(UserNotFoundException::new);
    }

    @CachePut(value = CACHE_USERS, key = "#id")
    @Transactional
    public UserResponse update(Long id, UserRequest userRequest) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

        if (userRequest.getBirthDate() != null) user.setBirthDate(userRequest.getBirthDate());
        if (userRequest.getEmail() != null) {
            if (userRepository.existsByEmail(userRequest.getEmail())
                    && !user.getEmail().equals(userRequest.getEmail()))
                throw new ValueAlreadyExistsException("email", userRequest.getEmail());

            Cache cache = cacheManager.getCache(CACHE_USERS);
            if (cache != null) cache.evict(user.getEmail());

            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.getName() != null) user.setName(userRequest.getName());
        if (userRequest.getSurname() != null) user.setSurname(userRequest.getSurname());

        return userMapper.toResponse(userRepository.save(user));
    }

    @CacheEvict(value = CACHE_USERS, key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) throw new UserNotFoundException();

        List<Card> cards = cardRepository.findByUserId(id);
        Cache cache = cacheManager.getCache(CACHE_USERS);
        if (cache != null) {
            for (Card card : cards) cache.evict(card.getId());
        }
        
        userRepository.deleteById(id);
    }
}
