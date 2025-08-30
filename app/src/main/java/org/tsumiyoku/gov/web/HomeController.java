package org.tsumiyoku.gov.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OAuth2User me, Model model) {
        if (me != null) {
            model.addAttribute("email", me.getAttribute("email"));
            model.addAttribute("citizenId", me.getAttribute("citizenId"));
        }
        return "home";
    }
}