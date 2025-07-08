package org.example.userserv.controller;

import org.example.userserv.dto.UserRequest;
import org.example.userserv.dto.UserResponse;
import org.example.userserv.entity.User;
import org.example.userserv.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2020, 1, 1);
    private static final String URL = "/v1/users/";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("database")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    User testUser = new User();

    @BeforeEach
    void initialize() {
        userRepository.deleteAll();

        User user = new User();
        user.setName("Name");
        user.setSurname("Surname");
        user.setBirthDate(BIRTH_DATE);
        user.setEmail("test@email.com");
        testUser = userRepository.save(user);
    }

    @Test
    void testCreateUser() {
        UserRequest userRequest = new UserRequest();
        userRequest.setName("Name");
        userRequest.setSurname("Surname");
        userRequest.setBirthDate(BIRTH_DATE);
        userRequest.setEmail("test@example.com");

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/v1/users", userRequest, UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse createdUser = response.getBody();
        assertThat(createdUser.getName()).isEqualTo(userRequest.getName());
        assertThat(createdUser.getEmail()).isEqualTo(userRequest.getEmail());
    }

    @Test
    void testGetCachedUser() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        connection.flushAll();
        connection.close();

        restTemplate.getForEntity( URL + "email/" + testUser.getEmail(), UserResponse.class);

        userRepository.deleteById(testUser.getId());

        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                URL + "email/" + testUser.getEmail(),
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(testUser.getId());

        connection.flushAll();
        connection.close();

        ResponseEntity<UserResponse> exceptionResponse = restTemplate.getForEntity(
                URL + "email/" + testUser.getEmail(),
                UserResponse.class
        );

        assertThat(exceptionResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testFindUserById() {
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                URL + testUser.getId(), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse responseUser = response.getBody();
        assertThat(responseUser.getName()).isEqualTo(testUser.getName());
        assertThat(responseUser.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void testFindUserByEmail() {
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                URL + "email/" + testUser.getEmail(), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse user = response.getBody();
        assertThat(user.getName()).isEqualTo(testUser.getName());
        assertThat(user.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void testFindUsersByIds() {
        User user = new User();
        user.setName("Name2");
        user.setSurname("Surname2");
        user.setBirthDate(BIRTH_DATE);
        user.setEmail("test2@example.com");
        User testUser2 = userRepository.save(user);

        List<Long> ids = List.of(testUser.getId(), testUser2.getId());
        HttpEntity<List<Long>> requestEntity = new HttpEntity<>(ids);
        ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                URL + "ids",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<UserResponse> users = response.getBody();
        assertThat(users).hasSize(2);
        assertThat(users.stream().map(UserResponse::getId))
                .containsExactlyInAnyOrder(testUser.getId(), testUser2.getId());
    }

    @Test
    void testUpdateUser() {
        UserRequest updateUser = new UserRequest();
        updateUser.setName("Updated Name");
        updateUser.setSurname("Updated Surname");
        updateUser.setEmail("test2@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(updateUser, headers);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                URL + testUser.getId(),
                HttpMethod.PUT,
                requestEntity,
                UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse updatedUser = response.getBody();
        assertThat(updatedUser.getName()).isEqualTo(updateUser.getName());
        assertThat(updatedUser.getSurname()).isEqualTo(updateUser.getSurname());
        assertThat(updatedUser.getEmail()).isEqualTo(updateUser.getEmail());
    }

    @Test
    void testDeleteUser() {
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                URL + testUser.getId(),
                HttpMethod.DELETE,
                null,
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> response = restTemplate.getForEntity(
                URL + testUser.getId(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}