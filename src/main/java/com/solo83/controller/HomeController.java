package com.solo83.controller;

import com.solo83.config.AppUserDetails;
import com.solo83.dto.BreadCrumbDTO;
import com.solo83.service.BreadCrumbService;
import com.solo83.service.MinioService;
import com.solo83.service.PathService;
import com.solo83.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@AllArgsConstructor
public class HomeController {

    private final MinioService minioService;
    private final BreadCrumbService breadCrumbService;
    private final UserService userService;
    private final PathService pathService;


    @RequestMapping("/home")
    public String home(@RequestParam(value = "path", required = false) String path, Model model) {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userName = appUserDetails.getUsername();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String resolvedPath = Optional.ofNullable(path).orElse("/");
                    String fullPath = pathService.appendUserRootFolder(resolvedPath, userRootFolder);
                    log.info("Full path: {}", fullPath);
                    List<BreadCrumbDTO> breadCrumb = breadCrumbService.getBreadCrumbsChain(fullPath);
                    model.addAttribute("userName", userName);
                    model.addAttribute("userObjects", breadCrumb);
                    return "index";
                })
                .orElse("redirect:/"); // Return "/" if user is not found or not authenticated
    }


    @GetMapping(value = "/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createUserFolder() {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    minioService.createEmptyFolder(userFolder);

                    minioService.createEmptyFolder("user-3-files/test1/");
                    minioService.createEmptyFolder("user-3-files/test2/test2subfolder/subfolder1/subfolder2");
                    minioService.createEmptyFolder("user-3-files/test3/test3subfolder/subfolder1/subfolder2/subfolder3");
                    minioService.createEmptyFolder("user-3-files/test4/test4subfolder");

                    return "redirect:/home";
                })
                .orElse("redirect:/"); // Return "/" if user is not found or not authenticated
    }


    @DeleteMapping(value = "/home")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String removeUserObject(@RequestParam String pathToObject, HttpServletRequest request) {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String fullPath = pathService.appendUserRootFolder(pathToObject, userRootFolder);
                    minioService.removeObject(fullPath);
                    return getPreviousPageByRequest(request).orElse("/home");
                })
                .orElse("/");
    }

    @PostMapping("/home/rename")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @ResponseBody
    public String renameObject(@RequestParam("newName") String newName, @RequestParam("oldName") String oldName, HttpServletRequest request) {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String oldPath = pathService.appendUserRootFolder(oldName, userRootFolder);
                    String newPath = pathService.updatePathWithNewName(oldPath, newName);
                    log.info(oldPath);
                    log.info(newPath);
                    minioService.renameObject(oldPath, newPath);
                    return getPreviousPageByRequest(request).orElse("/home");
                })
                .orElse("/");
    }


    private Optional<AppUserDetails> getAuthenticatedUserDetails() {
        return Optional.of(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> principal instanceof AppUserDetails
                        ? Optional.of((AppUserDetails) principal)
                        : Optional.empty());
    }

    private Optional<String> getPreviousPageByRequest(HttpServletRequest request) {
        log.info("Referer is {}",request.getHeader("Referer"));
        return Optional.ofNullable(request.getHeader("Referer")).map(requestUrl -> "redirect:" + requestUrl);
    }

}
