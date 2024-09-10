package com.solo83.controller;

import com.solo83.dto.BreadCrumbDTO;
import com.solo83.service.BreadCrumbService;
import com.solo83.service.MinioService;
import com.solo83.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@AllArgsConstructor
public class HomeController {

    private final MinioService minioService;
    private final BreadCrumbService breadCrumbService;
    private final UserService userService;


    @RequestMapping("/home")
    public String home(@RequestParam(value = "path", required = false) String path, Model model) {
        return Optional.of(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .flatMap(userService::findByName)
                .map(user -> {
                    Long id = user.getId();
                    String userName = user.getName();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String resolvedPath = Optional.ofNullable(path).orElse("/");
                    String fullPath = appendUserRootFolder(resolvedPath, userRootFolder);
                    List<BreadCrumbDTO> breadCrumb = breadCrumbService.getBreadCrumbsChain(fullPath);
                    model.addAttribute("userName", userName);
                    model.addAttribute("userObjects", breadCrumb);
                    return "index";
                })
                .orElse("redirect:/"); // Return "index" if user is not found or not authenticated
    }


    @GetMapping(value = "/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createUserFolder() {
        return Optional.of(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(userService::findByName)
                .map(user -> {
                    Long id = user.getId();
                    String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    minioService.createEmptyFolder(userFolder);
                    return "redirect:/home";
                })
                .orElse("redirect:/"); // Default redirect if user is not found
    }


    @GetMapping(value = "/home/remove")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String removeUserObject(@RequestParam String pathToObject, HttpServletRequest request) {
        return Optional.of(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(userService::findByName)
                .map(user -> {
                    Long id = user.getId();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String fullPath = appendUserRootFolder(pathToObject, userRootFolder);
                    minioService.removeObject(fullPath);
                    return getPreviousPageByRequest(request).orElse("/home");
                })
                .orElse("/"); // Go to home page if user is not found
    }


    private String appendUserRootFolder(String path, String userRootFolder) {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (!userRootFolder.endsWith("/")) {
            userRootFolder = userRootFolder + "/";
        }
        if (decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }
        return userRootFolder + decodedPath;
    }

    private Optional<String> getPreviousPageByRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer")).map(requestUrl -> "redirect:" + requestUrl);
    }

}
