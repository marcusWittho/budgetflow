package com.lwv.budgetflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "plan_id")
  @Builder.Default
  private PlanType planId = PlanType.FREE;

  @Column(name = "subscription_id")
  private UUID subscriptionId;

  @Column(name = "seats_available")
  @Builder.Default
  private Integer seatsAvailable = 5;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "features_enabled")
  @Builder.Default
  private List<String> featuresEnabled = List.of();

  @Builder.Default
  private OrganizationStatus status = OrganizationStatus.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
