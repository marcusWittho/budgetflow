package com.lwv.budgetflow.accounts.web;

import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import com.lwv.budgetflow.security.userdetails.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public LookupController(AccountRepository accountRepository,
                            PaymentMethodRepository paymentMethodRepository) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @GetMapping
    public LookupResponse listar(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();

        return new LookupResponse(
                accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)
                        .stream().map(a -> new LookupResponse.Item(a.getId(), a.getName()))
                        .toList(),
                paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)
                        .stream().map(p -> new LookupResponse.Item(p.getId(), p.getName()))
                        .toList());
    }
}
