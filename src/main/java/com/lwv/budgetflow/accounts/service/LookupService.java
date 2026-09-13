package com.lwv.budgetflow.accounts.service;

import com.lwv.budgetflow.accounts.dto.LookupResponse;
import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final AccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public LookupResponse listarContas(UUID userId) {
        return new LookupResponse(
                accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)
                        .stream().map(a -> new LookupResponse.Item(a.getId(), a.getName()))
                        .toList(),
                paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(userId)
                        .stream().map(p -> new LookupResponse.Item(p.getId(), p.getName()))
                        .toList());
    }
}
