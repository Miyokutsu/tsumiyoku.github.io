package org.tsumiyoku.gov.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminPageController {
    private final AdminApprovalService service;

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/admin/approvals")
    public String approvals(Model model) {
        model.addAttribute("items", service.list(null));
        return "approvals";
    }
}