package com.arok2.stockpilot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
        // JPA 기본 생성자
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
