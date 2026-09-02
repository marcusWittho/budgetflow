package com.lwv.budgetflow.ledger.service;

import com.lwv.budgetflow.ledger.domain.EntryGroup;
import com.lwv.budgetflow.ledger.repository.EntryGroupRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Substitui o botao "renovar +6" do app em Python.
 *
 * Toda madrugada, cada assinatura ativa e estendida ate ter HORIZONTE meses
 * gravados a frente. Ninguem precisa lembrar de renovar nada.
 *
 * EXIGE @EnableScheduling na classe BudgetflowApplication. Sem isso o
 * metodo simplesmente nunca roda, e nao ha erro nenhum avisando.
 */
@Component
public class RecurringMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringMaintenanceJob.class);
    private static final int HORIZONTE = 6;

    private final EntryGroupRepository groupRepository;
    private final EntryScheduleService scheduleService;

    public RecurringMaintenanceJob(EntryGroupRepository groupRepository,
                                   EntryScheduleService scheduleService) {
        this.groupRepository = groupRepository;
        this.scheduleService = scheduleService;
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void estenderAssinaturas() {
        LocalDate hoje = LocalDate.now();
        List<EntryGroup> ativas = groupRepository.findByKindAndActiveTrue(EntryGroup.RECURRING);

        int criadas = 0;
        for (EntryGroup grupo : ativas) {
            criadas += scheduleService.estenderRecorrente(grupo, HORIZONTE, hoje);
        }

        if (criadas > 0) {
            log.info("Recorrencias estendidas: {} lancamentos novos em {} assinaturas.",
                    criadas, ativas.size());
        }
    }
}
