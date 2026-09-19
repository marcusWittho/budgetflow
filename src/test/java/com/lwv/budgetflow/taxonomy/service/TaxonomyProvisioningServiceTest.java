package com.lwv.budgetflow.taxonomy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lwv.budgetflow.accounts.entity.AccountEntity;
import com.lwv.budgetflow.accounts.entity.PaymentMethodEntity;
import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import com.lwv.budgetflow.taxonomy.entity.CategoryEntity;
import com.lwv.budgetflow.taxonomy.repository.CategoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxonomyProvisioningService")
class TaxonomyProvisioningServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private PaymentMethodRepository paymentMethodRepository;

  @InjectMocks
  private TaxonomyProvisioningService taxonomyProvisioningService;

  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
  }

  @Test
  @DisplayName("deve provisionar taxonomia para novo usuário")
  void testProvisionarNovoUsuario() {
    when(categoryRepository.existsByUserId(testUserId)).thenReturn(false);

    TaxonomyProvisioningService.Resultado resultado =
        taxonomyProvisioningService.provisionar(testUserId);

    assertNotNull(resultado);
    assertFalse(resultado.jaExistia());
    assertTrue(resultado.categorias() > 0);
    assertTrue(resultado.subcategorias() > 0);
  }

  @Test
  @DisplayName("deve retornar idempotentemente se já provisionado")
  void testProvisionarIdempotente() {
    when(categoryRepository.existsByUserId(testUserId)).thenReturn(true);

    TaxonomyProvisioningService.Resultado resultado =
        taxonomyProvisioningService.provisionar(testUserId);

    assertNotNull(resultado);
    assertTrue(resultado.jaExistia());
    assertEquals(0, resultado.categorias());
    assertEquals(0, resultado.subcategorias());
    assertEquals(0, resultado.contas());
    assertEquals(0, resultado.formas());

    verify(categoryRepository, never()).save(any());
    verify(accountRepository, never()).save(any());
    verify(paymentMethodRepository, never()).save(any());
  }

  @Test
  @DisplayName("deve provisionar apenas uma vez")
  void testProvisionarApenasUmaVez() {
    when(categoryRepository.existsByUserId(testUserId))
        .thenReturn(false)
        .thenReturn(true);

    TaxonomyProvisioningService.Resultado primeira =
        taxonomyProvisioningService.provisionar(testUserId);
    TaxonomyProvisioningService.Resultado segunda =
        taxonomyProvisioningService.provisionar(testUserId);

    assertFalse(primeira.jaExistia());
    assertTrue(segunda.jaExistia());

    verify(categoryRepository, times(2)).existsByUserId(testUserId);
  }
}
