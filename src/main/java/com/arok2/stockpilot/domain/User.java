```java
package com.arok2.stockpilot.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false)
    private RiskProfile riskProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_period", nullable = false)
    private InvestmentPeriod investmentPeriod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
        // JPA
    }

    private User(String email, String passwordHash, String nickname,
                  RiskProfile riskProfile, InvestmentPeriod investmentPeriod) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.riskProfile = riskProfile;
        this.investmentPeriod = investmentPeriod;
    }

    public static User create(String email, String passwordHash, String nickname,
                               RiskProfile riskProfile, InvestmentPeriod investmentPeriod) {
        return new User(email, passwordHash, nickname, riskProfile, investmentPeriod);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public RiskProfile getRiskProfile() {
        return riskProfile;
    }

    public InvestmentPeriod getInvestmentPeriod() {
        return investmentPeriod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```
