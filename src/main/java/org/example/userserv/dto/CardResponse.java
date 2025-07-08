package org.example.userserv.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CardResponse {
    private Long id;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private Long userId;
}
