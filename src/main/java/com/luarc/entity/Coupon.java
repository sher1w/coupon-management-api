package com.luarc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(
                        name = "idx_coupon_code",
                        columnList = "code",
                        unique = true
                ),
                @Index(
                        name = "idx_coupon_status",
                        columnList = "is_active"
                ),
                @Index(
                        name = "idx_coupon_expiry",
                        columnList = "expiry_date"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    private String code;


    @Column(nullable = false)
    private BigDecimal discountValue;


    @Column(nullable = false)
    private String discountType;
    // PERCENTAGE or FIXED


    @Column(nullable = false)
    private Integer totalQuantity;


    @Column(nullable = false)
    private Integer claimedQuantity = 0;


    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;


    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Prevents lost updates when multiple users claim coupons simultaneously
    @Version
    private Long version;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * Check whether coupon can still be claimed
     */
    public boolean isAvailableForClaim() {

        return Boolean.TRUE.equals(isActive)
                && claimedQuantity < totalQuantity
                && LocalDateTime.now().isBefore(expiryDate);
    }


    /**
     * Remaining coupons available
     */
    public Integer getRemainingQuantity() {

        return totalQuantity - claimedQuantity;
    }


    /**
     * Increase claimed quantity safely
     */
    public void incrementClaimedQuantity() {

        if (getRemainingQuantity() > 0) {
            this.claimedQuantity++;
        }
    }
}