package com.solo83.controller;

import com.solo83.config.CustomUserDetails;
import com.solo83.dto.ItemDto;
import com.solo83.service.BreadCrumbService;
import com.solo83.service.MinioService;
import com.solo83.service.PathService;
import com.solo83.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String home(@RequestParam(value = "path", required = false) String path, Model model,
                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        String fullPath = resolveFullPath(path, userDetails);
        List<ItemDto> breadCrumb = breadCrumbService.getBreadCrumbsChain(fullPath);

        model.addAttribute("userObjects", breadCrumb);
        model.addAttribute("currentPath", fullPath);
        return "index";
    }

    @PostMapping("/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createEmptyFolder(@RequestParam(value = "newFolderName") String newFolderName,
                                    @RequestParam(value = "currentPath", required = false) String currentPath,
                                    HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (newFolderName == null || newFolderName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Folder name can't be blank.");
            return "redirect:/home";
        }

        String pathToNewFolder = pathService.getPathToNewFolder(currentPath, newFolderName);
        minioService.createEmptyFolder(pathToNewFolder);
        log.info("New folder was created: {}", pathToNewFolder);
        return getPreviousPageByRequest(request).orElse("/home");
    }

    @GetMapping("/home/create")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String createUserFolder(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String userFolder = userService.getUserRootFolder(userDetails.getUserId().toString());
        minioService.createEmptyFolder(userFolder);
        return "redirect:/home";
    }

    @GetMapping("home/download")
    public void downloadFile(@RequestParam String pathToObject,
                             @RequestParam String objectName,
                             HttpServletResponse response,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String fullPath = resolveFullPath(pathToObject, userDetails);
            minioService.downloadObject(objectName, fullPath, response);
        } catch (Exception e) {
            log.error("Error during file download for object: {}", objectName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while downloading file", e);
        }
    }

    @DeleteMapping("/home")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String removeUserObject(@RequestParam String pathToObject, HttpServletRequest request,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        String fullPath = resolveFullPath(pathToObject, userDetails);
        minioService.removeObject(fullPath);
        return getPreviousPageByRequest(request).orElse("/home");
    }

    @PostMapping("/home/rename")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String renameObject(@RequestParam("newName") String newName,
                               @RequestParam("oldName") String oldName,
                               HttpServletRequest request,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userRootFolder = userService.getUserRootFolder(userDetails.getUserId().toString());
        String oldPath = pathService.appendUserRootFolder(oldName, userRootFolder);
        String newPath = pathService.updatePathWithNewName(oldPath, newName);
        minioService.renameObject(oldPath, newPath);
        return getPreviousPageByRequest(request).orElse("/home");
    }

    @PostMapping("/home/upload")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String uploadFile(@RequestParam(value = "uploadedFile", required = false) MultipartFile file,
                             @RequestParam(value = "uploadedFolder", required = false) MultipartFile[] uploadedFolder,
                             @RequestParam("currentPath") String currentPath, HttpServletRequest request) {
        if (uploadedFolder != null && uploadedFolder.length > 0) {
            for (MultipartFile uploadedFile : uploadedFolder) {
                minioService.uploadFile(uploadedFile, currentPath);
            }
        } else if (file != null) {
            minioService.uploadFile(file, currentPath);
        }
        return getPreviousPageByRequest(request).orElse("/home");
    }
    private String resolveFullPath(String path, CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        String userRootFolder = userService.getUserRootFolder(userId.toString());
        String resolvedPath = Optional.ofNullable(path).orElse("/");
        return pathService.appendUserRootFolder(resolvedPath, userRootFolder);
    }

    private Optional<String> getPreviousPageByRequest(HttpServletRequest request) {
        log.info("Referer is {}", request.getHeader("Referer"));
        return Optional.ofNullable(request.getHeader("Referer")).map(requestUrl -> "redirect:" + requestUrl);
    }
}
