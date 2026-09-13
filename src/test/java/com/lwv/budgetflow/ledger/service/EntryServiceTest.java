package com.lwv.budgetflow.ledger.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lwv.budgetflow.accounts.repository.AccountRepository;
import com.lwv.budgetflow.accounts.repository.PaymentMethodRepository;
import com.lwv.budgetflow.ledger.dto.EntryRequest;
import com.lwv.budgetflow.ledger.entity.EntryEntity;
import com.lwv.budgetflow.ledger.repository.EntryRepository;
import com.lwv.budgetflow.taxonomy.entity.CategoryEntity;
import com.lwv.budgetflow.taxonomy.entity.SubcategoryEntity;
import com.lwv.budgetflow.taxonomy.repository.CategoryRepository;
import com.lwv.budgetflow.taxonomy.repository.SubcategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntryService")
class EntryServiceTest {

  @Mock
  private EntryRepository entryRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private SubcategoryRepository subcategoryRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private PaymentMethodRepository paymentMethodRepository;

  @Mock
  private EntryScheduleService scheduleService;

  @InjectMocks
  private EntryService entryService;

  private UUID testUserId;
  private UUID testCategoryId;
  private UUID testEntryId;
  private CategoryEntity testCategory;
  private EntryEntity testEntry;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testCategoryId = UUID.randomUUID();
    testEntryId = UUID.randomUUID();

    testCategory = new CategoryEntity(testUserId, "expense", "Alimentação");
    // Simular que a categoria já foi persistida
    testCategoryId = UUID.randomUUID();

    testEntry = new EntryEntity(testUserId, LocalDate.now(), testCategory, "Supermercado",
        new BigDecimal("150.00"), EntryEntity.PENDING);
    // Simular que a entrada já foi persistida
    testEntryId = UUID.randomUUID();
  }

  @Test
  @DisplayName("deve criar entrada única com sucesso")
  void testCriarEntradaUnica() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Compra no supermercado",
        new BigDecimal("150.00"),
        EntryEntity.PENDING,
        null,
        null, // repeticao nula = entrada única
        null,
        null,
        null
    );

    lenient().when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
        .thenReturn(Optional.of(testCategory));
    lenient().when(entryRepository.save(any(EntryEntity.class))).thenReturn(testEntry);

    List<EntryEntity> result = entryService.criar(testUserId, request);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testEntry.getDescription(), result.get(0).getDescription());
    verify(entryRepository).save(any(EntryEntity.class));
    verify(scheduleService, never()).criarParcelado(any(), anyInt(), anyInt());
    verify(scheduleService, never()).criarRecorrente(any(), anyInt());
  }

  @Test
  @DisplayName("deve criar entrada parcelada")
  void testCriarEntradaParcelada() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Compra parcelada",
        new BigDecimal("100.00"),
        EntryEntity.PENDING,
        null,
        "installment",
        12,
        1,
        null
    );

    List<EntryEntity> expectedInstallments = List.of(
        testEntry,
        testEntry,
        testEntry
    );

    lenient().when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
        .thenReturn(Optional.of(testCategory));
    lenient().when(entryRepository.save(any(EntryEntity.class))).thenReturn(testEntry);
    when(scheduleService.criarParcelado(any(), eq(1), eq(12)))
        .thenReturn(expectedInstallments);

    List<EntryEntity> result = entryService.criar(testUserId, request);

    assertNotNull(result);
    assertEquals(3, result.size());
    verify(scheduleService).criarParcelado(any(EntryEntity.class), eq(1), eq(12));
  }

  @Test
  @DisplayName("deve criar entrada recorrente")
  void testCriarEntradaRecorrente() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Assinatura",
        new BigDecimal("50.00"),
        EntryEntity.PENDING,
        null,
        "recurring",
        null,
        null,
        6
    );

    List<EntryEntity> expectedRecurrences = List.of(testEntry, testEntry, testEntry);

    lenient().when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
        .thenReturn(Optional.of(testCategory));
    lenient().when(entryRepository.save(any(EntryEntity.class))).thenReturn(testEntry);
    when(scheduleService.criarRecorrente(any(), eq(6))).thenReturn(expectedRecurrences);

    List<EntryEntity> result = entryService.criar(testUserId, request);

    assertNotNull(result);
    assertEquals(3, result.size());
    verify(scheduleService).criarRecorrente(any(EntryEntity.class), eq(6));
  }

  @Test
  @DisplayName("deve falhar ao criar com data nula")
  void testCriarFalhaSemData() {
    EntryRequest request = new EntryRequest(
        null,
        testCategoryId,
        null,
        null,
        null,
        "Descrição",
        new BigDecimal("100.00"),
        EntryEntity.PENDING,
        null,
        null,
        null,
        null,
        null
    );

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve falhar ao criar com categoria nula")
  void testCriarFalhaSemCategoria() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        null,
        null,
        null,
        null,
        "Descrição",
        new BigDecimal("100.00"),
        EntryEntity.PENDING,
        null,
        null,
        null,
        null,
        null
    );

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve falhar ao criar com descrição vazia")
  void testCriarFalhaSemDescricao() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "  ",
        new BigDecimal("100.00"),
        EntryEntity.PENDING,
        null,
        null,
        null,
        null,
        null
    );

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve falhar ao criar com valor inválido")
  void testCriarFalhaComValorZero() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Descrição",
        BigDecimal.ZERO,
        EntryEntity.PENDING,
        null,
        null,
        null,
        null,
        null
    );

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve falhar com status inválido")
  void testCriarFalhaComStatusInvalido() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Descrição",
        new BigDecimal("100.00"),
        "invalid_status",
        null,
        null,
        null,
        null,
        null
    );

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve falhar se parcela obrigatória não informada")
  void testCriarParceladaSemTotalParcelas() {
    EntryRequest request = new EntryRequest(
        LocalDate.now(),
        testCategoryId,
        null,
        null,
        null,
        "Descrição",
        new BigDecimal("100.00"),
        EntryEntity.PENDING,
        null,
        "installment",
        null, // totalParcelas nulo = erro
        null,
        null
    );

    lenient().when(categoryRepository.findByIdAndUserId(testCategoryId, testUserId))
        .thenReturn(Optional.of(testCategory));
    lenient().when(entryRepository.save(any(EntryEntity.class))).thenReturn(testEntry);

    assertThrows(IllegalArgumentException.class, () ->
        entryService.criar(testUserId, request)
    );
  }

  @Test
  @DisplayName("deve listar entradas por mês")
  void testListarMes() {
    YearMonth mes = YearMonth.now();
    List<EntryEntity> entries = List.of(testEntry);

    when(entryRepository.findPeriodo(eq(testUserId), any(), any()))
        .thenReturn(entries);

    List<EntryEntity> result = entryService.listarMes(testUserId, mes);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(entryRepository).findPeriodo(
        eq(testUserId),
        eq(mes.atDay(1)),
        eq(mes.atEndOfMonth())
    );
  }

  @Test
  @DisplayName("deve listar entradas por período")
  void testListarPeriodo() {
    LocalDate de = LocalDate.now().minusDays(30);
    LocalDate ate = LocalDate.now();
    List<EntryEntity> entries = List.of(testEntry);

    when(entryRepository.findPeriodo(testUserId, de, ate)).thenReturn(entries);

    List<EntryEntity> result = entryService.listarPeriodo(testUserId, de, ate);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(entryRepository).findPeriodo(testUserId, de, ate);
  }

  @Test
  @DisplayName("deve falhar ao listar com data inicial posterior final")
  void testListarPeriodoDataInvalida() {
    LocalDate de = LocalDate.now();
    LocalDate ate = LocalDate.now().minusDays(30);

    assertThrows(IllegalArgumentException.class, () ->
        entryService.listarPeriodo(testUserId, de, ate)
    );
  }

  @Test
  @DisplayName("deve marcar entrada como paga")
  void testMarcarPago() {
    when(entryRepository.findByIdAndUserId(testEntryId, testUserId))
        .thenReturn(Optional.of(testEntry));

    EntryEntity result = entryService.marcarPago(testUserId, testEntryId);

    assertNotNull(result);
    verify(entryRepository).findByIdAndUserId(testEntryId, testUserId);
  }

  @Test
  @DisplayName("deve falhar ao marcar entrada inexistente como paga")
  void testMarcarPagoNaoEncontrada() {
    when(entryRepository.findByIdAndUserId(testEntryId, testUserId))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        entryService.marcarPago(testUserId, testEntryId)
    );
  }

  @Test
  @DisplayName("deve apagar entrada")
  void testApagarEntrada() {
    when(entryRepository.findByIdAndUserId(testEntryId, testUserId))
        .thenReturn(Optional.of(testEntry));

    entryService.apagar(testUserId, testEntryId);

    verify(entryRepository).delete(testEntry);
  }

  @Test
  @DisplayName("deve falhar ao apagar entrada inexistente")
  void testApagarEntradaNaoEncontrada() {
    when(entryRepository.findByIdAndUserId(testEntryId, testUserId))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        entryService.apagar(testUserId, testEntryId)
    );
  }
}
