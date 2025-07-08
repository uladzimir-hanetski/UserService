package org.example.userserv.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserRequest {
    public interface CreateValidation extends Default {}
    public interface UpdateValidation extends Default {}

    @NotBlank(message = "Name can't be blank", groups = CreateValidation.class)
    @Size(min = 2, max = 32, message = "Name should be between 2 and 32 characters",
    groups = {CreateValidation.class, UpdateValidation.class})
    private String name;

    @NotBlank(message = "Surname can't be blank", groups = CreateValidation.class)
    @Size(min = 2, max = 64, message = "Surname should be between 2 and 64 characters",
    groups = {CreateValidation.class, UpdateValidation.class})
    private String surname;

    @NotNull(message = "Birth date can't be null", groups = CreateValidation.class)
    @Past(message = "Incorrect date", groups = {CreateValidation.class, UpdateValidation.class})
    private LocalDate birthDate;

    @NotBlank(message = "Email can't be blank", groups = CreateValidation.class)
    @Email(message = "Incorrect email format", groups = {CreateValidation.class, UpdateValidation.class})
    @Size(max = 64, message = "Email should be less than 64 characters")
    private String email;
}
