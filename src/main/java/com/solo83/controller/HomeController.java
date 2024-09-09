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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated()) {
            Optional<User> user = userService.findByName(auth.getName());
            if (user.isPresent()) {
                Long id = user.get().getId();
                String userName = auth.getName();
                String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                if (path == null) {
                    path = "/";
                }
                String fullPath = appendUserRootFolder(path, userRootFolder);
                List<BreadCrumbDTO> breadCrumb = breadCrumbService.getBreadCrumbsChain(fullPath);

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
        Optional<User> user = userService.findByName(auth.getName());
        if (user.isPresent()) {
            Long id = user.get().getId();
            String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
            minioService.createEmptyFolder(userFolder);

        }


   /*     minioService.createEmptyFolder("user-3-files/test1/test1subfolder1");
        minioService.createEmptyFolder("user-3-files/test2");
        minioService.createEmptyFolder("user-3-files/test2/test2subfolder1");
        minioService.createEmptyFolder("user-3-files/test2/test2subfolder1/subfolder1");
        minioService.createEmptyFolder("user-3-files/test5/test2subfolder1");

*/
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
