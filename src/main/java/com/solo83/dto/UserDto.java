package com.solo83.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Name should not be empty")
    private String name;

    @NotBlank(message = "Password should not be empty")
    private String password;
}
