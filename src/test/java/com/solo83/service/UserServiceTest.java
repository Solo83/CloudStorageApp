package com.solo83.service;

import com.solo83.dto.UserDto;
import com.solo83.entity.User;
import com.solo83.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@SpringBootTest
@Testcontainers
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource dataSource;

    @Container
    @ServiceConnection
    private static final MySQLContainer mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("db_test")
            .withUsername("user")
            .withPassword("123")
            .withInitScript("init.sql");

    @Test
    void testMySQLContainerIsRunning() {
        assertThat(mySQLContainer.isCreated()).isTrue();
        assertThat(mySQLContainer.isRunning()).isTrue();
    }

    @Test
    void testTableExists() throws SQLException {
        try (Connection conn = dataSource.getConnection();
            ResultSet resultSet = conn.prepareStatement("SHOW TABLES").executeQuery()) {
            resultSet.next();
            String table = resultSet.getString(1);
            assertThat(table).isEqualTo("users");
        }
    }

    @Test
    void testSaveUser() {

        UserDto userDto = new UserDto();
        userDto.setName("TestUser");
        userDto.setPassword("password");

        userService.saveUser(userDto);

        User savedUser = userRepository.findByName("TestUser").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("TestUser");
        assertThat(passwordEncoder.matches("password", savedUser.getPassword())).isTrue();
    }

    @Test
    void testFindByName() {
        User foundUser = userService.findByName("TestUser");
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getName()).isEqualTo("TestUser");
    }

    @Test
    void testFindAllUsers() {

        UserDto userDto1 = new UserDto();
        userDto1.setName("User1");
        userDto1.setPassword("password1");
        userService.saveUser(userDto1);

        UserDto userDto2 = new UserDto();
        userDto2.setName("User2");
        userDto2.setPassword("password2");
        userService.saveUser(userDto2);

        List<UserDto> users = userService.findAllUsers();
        
        assertThat(users.size()).isEqualTo(3);
        assertThat(users.get(0).getName()).isIn("TestUser", "User1","User2");
        assertThat(users.get(1).getName()).isIn("TestUser", "User1","User2");
        assertThat(users.get(2).getName()).isIn("TestUser", "User1","User2");
    }

    @Test
    void testSaveUserWithNonUniqueUsernameThrowsException() {

        UserDto userDto1 = new UserDto();
        userDto1.setName("TestUser");
        userDto1.setPassword("password");

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> userService.saveUser(userDto1))
                .withCauseInstanceOf(ConstraintViolationException.class)
                .withMessageContaining("could not execute statement");
    }

}