package com.solo83.controller;

import com.solo83.config.AppUserDetails;
import com.solo83.dto.BreadCrumbDTO;
import com.solo83.service.BreadCrumbService;
import com.solo83.service.MinioService;
import com.solo83.service.PathService;
import com.solo83.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
                    model.addAttribute("currentPath", fullPath);
                    return "index";
                })
                .orElse("redirect:/");
    }


    @PostMapping(value = "/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createEmptyFolder(@RequestParam(value = "newFolderName") @NotNull @NotEmpty String newFolderName,
                                    @RequestParam(value = "currentPath", required = false) String currentPath, HttpServletRequest request, BindingResult result) {

        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    if (newFolderName!=null && !newFolderName.isEmpty()) {
                        String pathToNewFolder = pathService.getPathToNewFolder(currentPath, newFolderName);
                        minioService.createEmptyFolder(pathToNewFolder);
                        log.info("New folder was created: {}", pathToNewFolder);
                        return getPreviousPageByRequest(request).orElse("/home");
                    }
                    return "redirect:/home";
                })
                .orElse("/");
    }

    @GetMapping (value = "/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createUserFolder() {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    minioService.createEmptyFolder(userFolder);
                    return "redirect:/home";
                })
                .orElse("/");
    }

    @GetMapping("home/download")
    public void downloadFile(@RequestParam String pathToObject,
                             @RequestParam String objectName,
                             HttpServletResponse response) {
        try {
            Optional<AppUserDetails> appUserDetails = getAuthenticatedUserDetails();
            Long id = appUserDetails.get().getUserId();
            String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
            String fullPath = pathService.appendUserRootFolder(pathToObject,userFolder);
            minioService.downloadObject(objectName,fullPath,response);
        } catch (Exception e) {
            log.error("Error during file download for object: {}", objectName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file", e);
        }
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
    public String renameObject(@RequestParam("newName") String newName, @RequestParam("oldName") String oldName, HttpServletRequest request) {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    Long id = appUserDetails.getUserId();
                    String userRootFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
                    String oldPath = pathService.appendUserRootFolder(oldName, userRootFolder);
                    String newPath = pathService.updatePathWithNewName(oldPath, newName);
                    minioService.renameObject(oldPath, newPath);
                    return getPreviousPageByRequest(request).orElse("/home");
                })
                .orElse("/");
    }

    @PostMapping("/home/upload")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String uploadFile(@RequestParam(value = "uploadedFile", required = false) MultipartFile file,
                             @RequestParam(value = "uploadedFolder", required = false) MultipartFile[] uploadedFolder,
                             @RequestParam("currentPath") String currentPath, HttpServletRequest request) {
        return getAuthenticatedUserDetails()
                .map(appUserDetails -> {
                    boolean isFolder = uploadedFolder != null && uploadedFolder.length > 0;
                    boolean isFile = file != null;

                    if (isFolder){
                        for (MultipartFile uploadedFile : uploadedFolder) {
                            minioService.uploadFile(uploadedFile, currentPath);
                        }
                        return getPreviousPageByRequest(request).orElse("/home");
                    } else if(isFile) {
                        minioService.uploadFile(file, currentPath);
                        return getPreviousPageByRequest(request).orElse("/home");
                    }
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
