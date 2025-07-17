package org.example.userserv.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String email;
    private List<CardResponse> cards = new ArrayList<>();
}
