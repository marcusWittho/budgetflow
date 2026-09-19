package com.lwv.budgetflow.accounts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lwv.budgetflow.accounts.dto.LookupResponse;
import com.lwv.budgetflow.accounts.entity.AccountEntity;
import com.lwv.budgetflow.accounts.entity.PaymentMethodEntity;
import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LookupService")
class LookupServiceTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private PaymentMethodRepository paymentMethodRepository;

  @InjectMocks
  private LookupService lookupService;

  private UUID testUserId;
  private UUID accountId;
  private UUID paymentMethodId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    accountId = UUID.randomUUID();
    paymentMethodId = UUID.randomUUID();
  }

  @Test
  @DisplayName("deve listar contas e formas de pagamento do usuário")
  void testListarContasComSucesso() {
    AccountEntity account = new AccountEntity(testUserId, "Conta Corrente");
    PaymentMethodEntity paymentMethod = new PaymentMethodEntity(testUserId, "Débito");

    when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of(account));
    when(paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of(paymentMethod));

    LookupResponse response = lookupService.listarContas(testUserId);

    assertNotNull(response);
    assertEquals(1, response.accounts().size());
    assertEquals(1, response.paymentMethods().size());
    assertEquals("Conta Corrente", response.accounts().get(0).name());
    assertEquals("Débito", response.paymentMethods().get(0).name());

    verify(accountRepository).findByUserIdAndArchivedFalseOrderByNameAsc(testUserId);
    verify(paymentMethodRepository).findByUserIdAndArchivedFalseOrderByNameAsc(testUserId);
  }

  @Test
  @DisplayName("deve retornar listas vazias se usuário não tiver contas/formas")
  void testListarContasVazias() {
    when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of());
    when(paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of());

    LookupResponse response = lookupService.listarContas(testUserId);

    assertNotNull(response);
    assertTrue(response.accounts().isEmpty());
    assertTrue(response.paymentMethods().isEmpty());
  }

  @Test
  @DisplayName("deve listar múltiplas contas ordenadas alfabeticamente")
  void testListarMultiplasContas() {
    AccountEntity conta1 = new AccountEntity(testUserId, "Banco X");
    AccountEntity conta2 = new AccountEntity(testUserId, "Banco Y");

    when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of(conta1, conta2));
    when(paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of());

    LookupResponse response = lookupService.listarContas(testUserId);

    assertEquals(2, response.accounts().size());
    assertEquals("Banco X", response.accounts().get(0).name());
    assertEquals("Banco Y", response.accounts().get(1).name());
  }

  @Test
  @DisplayName("deve excluir contas arquivadas")
  void testListarContasExclueArquivadas() {
    AccountEntity ativa = new AccountEntity(testUserId, "Ativa");

    when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of(ativa));
    when(paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of());

    LookupResponse response = lookupService.listarContas(testUserId);

    assertEquals(1, response.accounts().size());
    verify(accountRepository).findByUserIdAndArchivedFalseOrderByNameAsc(testUserId);
  }

  @Test
  @DisplayName("deve listar múltiplas formas de pagamento")
  void testListarMultiplasFormasPagamento() {
    PaymentMethodEntity forma1 = new PaymentMethodEntity(testUserId, "Débito");
    PaymentMethodEntity forma2 = new PaymentMethodEntity(testUserId, "Crédito");

    when(accountRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of());
    when(paymentMethodRepository.findByUserIdAndArchivedFalseOrderByNameAsc(testUserId))
        .thenReturn(List.of(forma1, forma2));

    LookupResponse response = lookupService.listarContas(testUserId);

    assertEquals(2, response.paymentMethods().size());
    assertEquals("Débito", response.paymentMethods().get(0).name());
    assertEquals("Crédito", response.paymentMethods().get(1).name());
  }
}
