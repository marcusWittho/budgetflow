package com.lwv.budgetflow.ledger.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.lwv.budgetflow.ledger.entity.EntryEntity;
import com.lwv.budgetflow.ledger.entity.EntryGroupEntity;
import com.lwv.budgetflow.ledger.repository.EntryGroupRepository;
import com.lwv.budgetflow.ledger.repository.EntryRepository;
import com.lwv.budgetflow.taxonomy.entity.CategoryEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntryScheduleService")
class EntryScheduleServiceTest {

  @Mock
  private EntryRepository entryRepository;

  @Mock
  private EntryGroupRepository groupRepository;

  @InjectMocks
  private EntryScheduleService scheduleService;

  private UUID testUserId;
  private CategoryEntity testCategory;
  private EntryEntity testEntry;
  private UUID testGroupId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testGroupId = UUID.randomUUID();

    testCategory = new CategoryEntity(testUserId, "expense", "Compras");

    testEntry = new EntryEntity(
        testUserId,
        LocalDate.now(),
        testCategory,
        "Compra teste",
        new BigDecimal("100.00"),
        EntryEntity.PENDING
    );
    testGroupId = UUID.randomUUID();
  }

  @Test
  @DisplayName("deve criar parcelamento com sucesso")
  void testCriarParceladoComSucesso() {
    int totalParcelas = 3;
    int parcelaInicial = 1;

    List<EntryEntity> savedEntries = List.of(testEntry, testEntry, testEntry);
    lenient().when(groupRepository.save(any(EntryGroupEntity.class)))
        .thenReturn(EntryGroupEntity.installment(testUserId, "P001", 3, "Teste"));
    when(entryRepository.saveAll(any())).thenReturn(savedEntries);

    List<EntryEntity> result = scheduleService.criarParcelado(testEntry, parcelaInicial, totalParcelas);

    assertNotNull(result);
    assertEquals(3, result.size());
    verify(groupRepository).save(any(EntryGroupEntity.class));
    verify(entryRepository).saveAll(any());
  }

  @Test
  @DisplayName("deve falhar com total de parcelas menor que 1")
  void testCriarParceladoComTotalInvalido() {
    assertThrows(IllegalArgumentException.class, () ->
        scheduleService.criarParcelado(testEntry, 1, 0)
    );
  }

  @Test
  @DisplayName("deve falhar com parcela inicial menor que 1")
  void testCriarParceladoComParcelaInicialMenorQue1() {
    assertThrows(IllegalArgumentException.class, () ->
        scheduleService.criarParcelado(testEntry, 0, 12)
    );
  }

  @Test
  @DisplayName("deve falhar com parcela inicial maior que total")
  void testCriarParceladoComParcelaInicialMaiorQueTotal() {
    assertThrows(IllegalArgumentException.class, () ->
        scheduleService.criarParcelado(testEntry, 13, 12)
    );
  }

  @Test
  @DisplayName("deve criar parcelamento começando de parcela diferente de 1")
  void testCriarParceladoComParcelaInicialCustomizada() {
    int totalParcelas = 12;
    int parcelaInicial = 3;
    int esperadas = totalParcelas - parcelaInicial + 1; // 10

    List<EntryEntity> savedEntries = new java.util.ArrayList<>();
    for (int i = 0; i < esperadas; i++) {
      savedEntries.add(testEntry);
    }

    lenient().when(groupRepository.save(any(EntryGroupEntity.class)))
        .thenReturn(EntryGroupEntity.installment(testUserId, "P001", totalParcelas, "Teste"));
    when(entryRepository.saveAll(any())).thenReturn(savedEntries);

    List<EntryEntity> result = scheduleService.criarParcelado(testEntry, parcelaInicial, totalParcelas);

    assertEquals(esperadas, result.size());
  }

  @Test
  @DisplayName("deve criar recorrência com sucesso")
  void testCriarRecorrenteComSucesso() {
    int meses = 6;
    List<EntryEntity> savedEntries = List.of(
        testEntry, testEntry, testEntry,
        testEntry, testEntry, testEntry
    );

    lenient().when(groupRepository.save(any(EntryGroupEntity.class)))
        .thenReturn(EntryGroupEntity.installment(testUserId, "P001", 3, "Teste"));
    when(entryRepository.saveAll(any())).thenReturn(savedEntries);

    List<EntryEntity> result = scheduleService.criarRecorrente(testEntry, meses);

    assertNotNull(result);
    assertEquals(6, result.size());
    verify(groupRepository).save(any(EntryGroupEntity.class));
    verify(entryRepository).saveAll(any());
  }

  @Test
  @DisplayName("deve falhar com meses menor que 1")
  void testCriarRecorrenteComMesesInvalido() {
    assertThrows(IllegalArgumentException.class, () ->
        scheduleService.criarRecorrente(testEntry, 0)
    );

    assertThrows(IllegalArgumentException.class, () ->
        scheduleService.criarRecorrente(testEntry, -1)
    );
  }

  @Test
  @DisplayName("deve criar recorrência com 1 mês")
  void testCriarRecorrenteComUmMes() {
    List<EntryEntity> savedEntries = List.of(testEntry);

    lenient().when(groupRepository.save(any(EntryGroupEntity.class)))
        .thenReturn(EntryGroupEntity.installment(testUserId, "P001", 3, "Teste"));
    when(entryRepository.saveAll(any())).thenReturn(savedEntries);

    List<EntryEntity> result = scheduleService.criarRecorrente(testEntry, 1);

    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("deve estender recorrência com sucesso")
  void testEstenderRecorrenteComSucesso() {
    EntryGroupEntity grupo = EntryGroupEntity.recurring(testUserId, "R001", "Recorrente");

    EntryEntity ultimaEntrada = new EntryEntity(
        testUserId,
        LocalDate.now().minusMonths(1),
        testCategory,
        "Recorrente",
        new BigDecimal("50.00"),
        EntryEntity.PENDING
    );

    when(entryRepository.findTopByGroupOrderByEntryDateDesc(grupo))
        .thenReturn(Optional.of(ultimaEntrada));
    when(entryRepository.saveAll(any())).thenReturn(List.of(testEntry));

    int geradas = scheduleService.estenderRecorrente(grupo, 6, LocalDate.now());

    assertTrue(geradas > 0);
    verify(entryRepository).saveAll(any());
  }

  @Test
  @DisplayName("deve retornar 0 ao estender grupo inativo")
  void testEstenderRecorrenteGrupoInativo() {
    EntryGroupEntity grupoInativo = EntryGroupEntity.recurring(testUserId, "R001", "Inativo");
    grupoInativo.deactivate();

    int geradas = scheduleService.estenderRecorrente(grupoInativo, 6, LocalDate.now());

    assertEquals(0, geradas);
    verify(entryRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("deve retornar 0 ao estender grupo de parcelamento")
  void testEstenderGrupoNaoRecorrente() {
    EntryGroupEntity grupoParcelado = EntryGroupEntity.installment(testUserId, "P001", 12, "Parcelado");

    int geradas = scheduleService.estenderRecorrente(grupoParcelado, 6, LocalDate.now());

    assertEquals(0, geradas);
  }

  @Test
  @DisplayName("deve retornar 0 se grupo não tiver entradas")
  void testEstenderRecorrenteSemEntradas() {
    EntryGroupEntity grupo = EntryGroupEntity.recurring(testUserId, "R001", "Recorrente");

    when(entryRepository.findTopByGroupOrderByEntryDateDesc(grupo))
        .thenReturn(Optional.empty());

    int geradas = scheduleService.estenderRecorrente(grupo, 6, LocalDate.now());

    assertEquals(0, geradas);
    verify(entryRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("deve cancelar futuros com sucesso")
  void testCancelarFuturosComSucesso() {
    EntryGroupEntity grupo = EntryGroupEntity.recurring(testUserId, "R001", "Recorrente");

    List<EntryEntity> futuras = List.of(testEntry, testEntry, testEntry);

    when(entryRepository.findByGroupAndStatusAndEntryDateAfter(
        eq(grupo), eq(EntryEntity.PENDING), any())).thenReturn(futuras);

    int deletadas = scheduleService.cancelarFuturos(grupo, LocalDate.now());

    assertEquals(3, deletadas);
    verify(entryRepository).deleteAll(futuras);
    verify(groupRepository).save(any(EntryGroupEntity.class));
  }

  @Test
  @DisplayName("deve retornar 0 ao cancelar sem entradas futuras")
  void testCancelarFuturosSemEntradas() {
    EntryGroupEntity grupo = EntryGroupEntity.recurring(testUserId, "R001", "Recorrente");

    lenient().when(entryRepository.findByGroupAndStatusAndEntryDateAfter(
        eq(grupo), eq(EntryEntity.PENDING), any())).thenReturn(List.of());

    int deletadas = scheduleService.cancelarFuturos(grupo, LocalDate.now());

    assertEquals(0, deletadas);
    verify(entryRepository).deleteAll(List.of());
    verify(groupRepository).save(any());
  }
}
