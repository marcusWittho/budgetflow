package com.lwv.budgetflow.accounts.controller;

import com.lwv.budgetflow.accounts.dto.LookupResponse;
import com.lwv.budgetflow.accounts.service.LookupService;
import com.lwv.budgetflow.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @GetMapping
    public LookupResponse listar(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        return lookupService.listarContas(userId);
    }
}
