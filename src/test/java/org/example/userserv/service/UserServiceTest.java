package org.example.userserv.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
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
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private RedisCacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UserService userService;

    private final UserRequest userRequest = new UserRequest();
    private final User user = new User();
    private final UserResponse userResponse = new UserResponse();

    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void initialize() {
        user.setId(uuid);
        user.setEmail("test@example.com");

        userRequest.setEmail("test@example.com");
        userRequest.setName("Just");
        userRequest.setSurname("Test");
        userRequest.setBirthDate(LocalDate.of(2000, 1, 1));

        userResponse.setId(uuid);
        userResponse.setEmail("test@example.com");
    }

    @Test
    void testCreateUser() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userMapper.toEntity(userRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(securityUtil.getUserIdFromToken(anyString())).thenReturn(uuid);

        UserResponse result = userService.create(userRequest);

        assertEquals(userResponse, result);
    }

    @Test
    void testCreateUserEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ValueAlreadyExistsException.class, () ->
                userService.create(userRequest));
    }

    @Test
    void testFindById() {
        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.findById(uuid);

        assertEquals(userResponse, result);
    }

    @Test
    void testFindByIdNotFound() {
        when(userRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(uuid));
    }

    @Test
    void testFindByIds() {
        UUID uuid2 = UUID.randomUUID();
        List<UUID> ids = List.of(uuid, uuid2);
        User user2 = new User();
        user2.setId(uuid2);

        when(userRepository.findByIds(ids)).thenReturn(List.of(user, user2));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(userMapper.toResponse(user2)).thenReturn(new UserResponse());

        List<UserResponse> result = userService.findByIds(ids);

        assertEquals(2, result.size());
    }

    @Test
    void testFindByIdsEmptyList() {
        List<UserResponse> result = userService.findByIds(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.findByEmail("test@example.com");

        assertEquals(userResponse, result);
    }

    @Test
    void testFindByEmailNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.findByEmail("test@example.com"));
    }

    @Test
    void testUpdateUser() {
        LocalDate birthDate = LocalDate.of(2022, 2, 2);

        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("new@example.com");
        updateRequest.setName("New name");
        updateRequest.setSurname("New surname");
        updateRequest.setBirthDate(birthDate);

        User updatedUser = new User();
        updatedUser.setId(uuid);
        updatedUser.setEmail("new@example.com");
        updatedUser.setName("New name");
        updatedUser.setSurname("New surname");
        updatedUser.setBirthDate(birthDate);

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(uuid);
        updatedResponse.setEmail("new@example.com");
        updatedResponse.setName("New name");
        updatedResponse.setSurname("New surname");
        updatedResponse.setBirthDate(birthDate);

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);
        when(cacheManager.getCache("users")).thenReturn(cache);

        UserResponse result = userService.update(uuid, updateRequest);

        assertEquals("New name", result.getName());
        assertEquals("New surname", result.getSurname());
        assertEquals(birthDate, result.getBirthDate());
    }

    @Test
    void testUpdateUserEmailAlreadyExists() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("new@example.com");

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThrows(ValueAlreadyExistsException.class,
                () -> userService.update(uuid, updateRequest));
    }

    @Test
    void testDeleteUser() {
        Card card = new Card();
        card.setId(1L);
        cache.put(1L, card);

        when(userRepository.existsById(uuid)).thenReturn(true);
        when(cardRepository.findByUserId(uuid)).thenReturn(List.of(new Card()));
        when(cacheManager.getCache("users")).thenReturn(cache);

        userService.delete(uuid);

        verify(userRepository).deleteById(uuid);
    }

    @Test
    void testDeleteUserNotFound() {
        when(userRepository.existsById(uuid)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.delete(uuid));
    }
}
