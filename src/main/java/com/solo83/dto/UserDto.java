package com.solo83.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto
{
    private Long id;
    @NotNull
    @NotEmpty(message = "Name should not be empty")
    private String name;

    @NotNull
    @NotEmpty(message = "Password should not be empty")
    private String password;
}
