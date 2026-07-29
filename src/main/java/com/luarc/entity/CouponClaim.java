package com.luarc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_claims", indexes = {
    @Index(name = "idx_claim_user_id", columnList = "user_id"),
    @Index(name = "idx_claim_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_claim_user_coupon", columnList = "user_id,coupon_id"),
    @Index(name = "idx_claim_created_at", columnList = "claimed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.claimedAt == null) {
            this.claimedAt = LocalDateTime.now();
        }
    }
}
