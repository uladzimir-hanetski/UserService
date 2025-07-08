package org.example.userserv.controller;

import org.example.userserv.dto.CardRequest;
import org.example.userserv.dto.CardResponse;
import org.example.userserv.entity.Card;
import org.example.userserv.entity.User;
import org.example.userserv.repository.CardRepository;
import org.example.userserv.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardControllerIntegrationTest {
    private static final LocalDate EXPIRATION_DATE = LocalDate.of(2030, 1, 1);
    private static final String URL = "/v1/cards/";

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

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    private User testUser;
    private Card testCard;

    @BeforeEach
    void initialize() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setName("Test");
        user.setSurname("User");
        user.setEmail("test.user@example.com");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        testUser = userRepository.save(user);

        Card card = new Card();
        card.setNumber("1111");
        card.setHolder("Test Holder");
        card.setExpirationDate(EXPIRATION_DATE);
        card.setUser(testUser);
        testCard = cardRepository.save(card);
    }

    @Test
    void testCreateCard() {
        CardRequest cardRequest = new CardRequest();
        cardRequest.setNumber("5555");
        cardRequest.setHolder("Test User");
        cardRequest.setExpirationDate(EXPIRATION_DATE);
        cardRequest.setUserId(testUser.getId());

        ResponseEntity<CardResponse> response = restTemplate.postForEntity(
                "/v1/cards", cardRequest, CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CardResponse cardResponse = response.getBody();
        assertThat(cardResponse.getNumber()).isEqualTo(cardRequest.getNumber());
        assertThat(cardResponse.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    void testGetCachedCard() {
        restTemplate.getForEntity(URL + testCard.getId(), CardResponse.class);

        cardRepository.deleteById(testCard.getId());

        ResponseEntity<CardResponse> response = restTemplate.getForEntity(
                URL + testCard.getId(),
                CardResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNumber()).isEqualTo("1111");

        RedisConnection connection = redisConnectionFactory.getConnection();
        connection.flushAll();
        connection.close();

        ResponseEntity<CardResponse> exceptionResponse = restTemplate.getForEntity(
                URL + testCard.getId(),
                CardResponse.class
        );
        assertThat(exceptionResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCreateCardWithExistingNumber() {
        CardRequest cardRequest = new CardRequest();
        cardRequest.setNumber("1111");
        cardRequest.setHolder("Test User");
        cardRequest.setExpirationDate(EXPIRATION_DATE);
        cardRequest.setUserId(testUser.getId());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/cards", cardRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void testFindCardById() {
        ResponseEntity<CardResponse> response = restTemplate.getForEntity(
                URL + testCard.getId(), CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CardResponse cardResponse = response.getBody();
        assertThat(cardResponse.getId()).isEqualTo(testCard.getId());
        assertThat(cardResponse.getNumber()).isEqualTo(testCard.getNumber());
        assertThat(cardResponse.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    void testFindCardsByIds() {
        Card card2 = new Card();
        card2.setNumber("2222");
        card2.setHolder("Holder 2");
        card2.setExpirationDate(EXPIRATION_DATE);
        card2.setUser(testUser);
        Card testCard2 = cardRepository.save(card2);

        List<Long> ids = List.of(testCard.getId(), testCard2.getId());

        HttpEntity<List<Long>> requestEntity = new HttpEntity<>(ids);
        ResponseEntity<List<CardResponse>> response = restTemplate.exchange(
                URL + "ids",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CardResponse> cardResponses = response.getBody();
        assertThat(cardResponses).hasSize(2);
        assertThat(cardResponses.stream().map(CardResponse::getId))
                .containsExactlyInAnyOrder(testCard.getId(), testCard2.getId());
    }

    @Test
    void testUpdateCard() {
        CardRequest updateRequest = new CardRequest();
        updateRequest.setNumber("3333");
        updateRequest.setHolder("Updated Holder");
        updateRequest.setUserId(testUser.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CardRequest> requestEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<CardResponse> response = restTemplate.exchange(
                URL + testCard.getId(),
                HttpMethod.PUT,
                requestEntity,
                CardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CardResponse updatedCard = response.getBody();
        assertThat(updatedCard.getNumber()).isEqualTo(updateRequest.getNumber());
        assertThat(updatedCard.getHolder()).isEqualTo(updateRequest.getHolder());
    }

    @Test
    void testDeleteCard() {
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                URL + testCard.getId(),
                HttpMethod.DELETE,
                null,
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> response = restTemplate.getForEntity(
                URL + testCard.getId(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}