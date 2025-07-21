package org.example.userserv.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CardResponse {
    private Long id;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private UUID userId;
}
