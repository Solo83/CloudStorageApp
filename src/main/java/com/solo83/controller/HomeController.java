package com.solo83.controller;

import com.solo83.dto.BreadCrumbDTO;
import com.solo83.entity.User;
import com.solo83.service.BreadCrumbService;
import com.solo83.service.MinioService;
import com.solo83.service.UserService;
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

@Slf4j
@Controller
@AllArgsConstructor
public class HomeController {

    private final MinioService minioService;
    private final BreadCrumbService breadCrumbService;
    private final UserService userService;


    @RequestMapping("/home")
    public String home(@RequestParam(value = "path", required = false) String path, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated()) {
            User user = userService.findByName(auth.getName());
            if (user != null) {
                Long id = user.getId();
                String userName = auth.getName();
                String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                if (path == null) {
                    path = "/";
                }
                String fullPath = appendUserRootFolder(path, userRootFolder);
                List<BreadCrumbDTO> breadCrumb = breadCrumbService.getBreadCrumb(fullPath, userRootFolder);
                log.info("BreadCrumb: {}", breadCrumb);

                model.addAttribute("userName", userName);
                model.addAttribute("userObjects", breadCrumb);
            }
        }
        return "index";
    }


    @GetMapping(value = "/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createUserFolder() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long id = userService.findByName(auth.getName()).getId();
        String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
        minioService.createEmptyFolder(userFolder);
        return "redirect:/home";
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

}
