package com.solo83.controller;

import com.solo83.dto.UserDto;
import com.solo83.entity.User;
import com.solo83.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@AllArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            model.addAttribute("errorMessage", session.getAttribute("errorMessage"));
            session.removeAttribute("errorMessage");
            return "login";
        }
        return "redirect:/home";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            UserDto user = new UserDto();
            model.addAttribute("user", user);
            return "register";
        }
        return "redirect:/home";
    }

    @PostMapping("/register/save")
    public String registration(
            @RequestParam String passwordConfirm,
            @ModelAttribute("user") @Valid UserDto userDto,
            BindingResult result,
            Model model) {

        Optional<User> existingUserOpt = userService.findByName(userDto.getName());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getName() != null && !existingUser.getName().isEmpty()) {
                result.rejectValue("name", "error.name",
                        "There is already an account registered with the same username");
            }
        }

        if (!userDto.getPassword().equals(passwordConfirm)) {
            result.rejectValue("password", "password",
                    "Password confirmation mismatch");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDto);
            return "/register";
        }
        userService.saveUser(userDto);
        return "redirect:/login?success";
    }
}

