package com.lwv.budgetflow.security.userdetails;

import com.lwv.budgetflow.domain.entity.UserEntity;
import com.lwv.budgetflow.domain.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String email;
  private final String password;
  private final String fullName;
  private final UserStatus status;

  public UserPrincipal(UserEntity user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.password = user.getPasswordHash();
    this.fullName = user.getFullName();
    this.status = user.getStatus();
  }

  private UserPrincipal(UUID id, String email) {
    this.id = id;
    this.email = email;
    this.password = null;
    this.fullName = null;
    this.status = UserStatus.ACTIVE;
  }

  /** Builds a principal straight from validated JWT claims, without a DB round trip. */
  public static UserPrincipal fromToken(UUID id, String email) {
    return new UserPrincipal(id, email);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.SUSPENDED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return status == UserStatus.ACTIVE;
  }
}
