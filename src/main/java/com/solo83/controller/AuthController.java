package com.solo83.controller;

import com.solo83.dto.UserDto;
import com.solo83.entity.User;
import com.solo83.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/home")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = auth.getName();
        model.addAttribute("userName", userName);
        return "index";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model){
        // create model object to store form data
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    @PostMapping("/register/save")
    public String registration(
            @RequestParam String passwordConfirm,
            @ModelAttribute("user") @Valid UserDto userDto,
                               BindingResult result,
                               Model model){
        User existingUser = userService.findByName(userDto.getName());
        if(existingUser != null && existingUser.getName() != null && !existingUser.getName().isEmpty()){
            result.rejectValue("name", "error.name",
                    "There is already an account registered with the same username");
        }

        if(!userDto.getPassword().equals(passwordConfirm)){
            result.rejectValue("password", "password",
                    "Password confirmation mismatch");
        }

        if(result.hasErrors()){
            model.addAttribute("user", userDto);
            return "/register";
        }
        userService.saveUser(userDto);
        return "redirect:/register?success";
    }

    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String pageForAdmins(){
        return "This is page for only admins";
    }

}

