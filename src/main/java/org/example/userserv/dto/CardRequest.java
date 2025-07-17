package org.example.userserv.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CardRequest {
    public interface CreateValidation extends Default {}
    public interface UpdateValidation extends Default {}

    @NotBlank(message = "Number can't be blank", groups = CreateValidation.class)
    @Size(max = 32, message = "Number should be less than 32 characters",
    groups = {CreateValidation.class, UpdateValidation.class})
    private String number;

    @NotBlank(message = "Holder can't be blank", groups = CreateValidation.class)
    @Size(max = 32, message = "Holder should be less than 32 characters",
    groups = {CreateValidation.class, UpdateValidation.class})
    private String holder;

    @NotNull(message = "Expiration date can't be null", groups = CreateValidation.class)
    @Future(message = "Incorrect date",
    groups = {CreateValidation.class, UpdateValidation.class})
    private LocalDate expirationDate;

    @NotNull(message = "User id can't be null", groups = CreateValidation.class)
    private UUID userId;
}
