package com.solo83.service;

import com.solo83.dto.UserDto;
import com.solo83.entity.User;
import com.solo83.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getName());

        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
    }

    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    public String getUserRootFolder(String userId) {
        String folderPattern = env.getProperty("minio.user.directory.pattern");
        return String.format(folderPattern, userId);
    }

    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setName(user.getName());
        return userDto;
    }
}
