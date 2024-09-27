package com.solo83.controller;

import com.solo83.config.CustomUserDetails;
import com.solo83.dto.ItemDto;
import com.solo83.service.SearchService;
import com.solo83.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@AllArgsConstructor
public class SearchController {

    UserService userService;
    SearchService searchService;

    @GetMapping(value = "/search")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public String search(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, @RequestParam(value = "query") String query, RedirectAttributes redirectAttributes) {
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Search query can't be blank.");
            return "redirect:/home";
        }

        Long id = userDetails.getUserId();
        String userFolder = userService.getUserRootFolder(String.valueOf(id.intValue()));
        List<ItemDto> results = searchService.searchObjectsByName(userFolder, query);
        model.addAttribute("results", results);
        return "search";
    }
}
