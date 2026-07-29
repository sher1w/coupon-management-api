package com.luarc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Coupon code is required")
        @Size(min = 3, max = 50)
        private String code;

        @NotNull(message = "Discount value is required")
        @Positive(message = "Discount value must be positive")
        private BigDecimal discountValue;

        @NotBlank(message = "Discount type is required")
        @Pattern(regexp = "PERCENTAGE|FIXED", message = "Discount type must be PERCENTAGE or FIXED")
        private String discountType;

        @NotNull(message = "Total quantity is required")
        @Positive(message = "Total quantity must be positive")
        private Integer totalQuantity;

        @NotNull(message = "Expiry date is required")
        @FutureOrPresent(message = "Expiry date must be in the future")
        private LocalDateTime expiryDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private Integer totalQuantity;
        private LocalDateTime expiryDate;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String code;
        private BigDecimal discountValue;
        private String discountType;
        private Integer totalQuantity;
        private Integer claimedQuantity;
        private Integer remainingQuantity;
        private LocalDateTime expiryDate;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimRequest {
        @NotBlank(message = "Coupon code is required")
        private String couponCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimResponse {
        private Long claimId;
        private Long couponId;
        private String couponCode;
        private BigDecimal discountValue;
        private String discountType;
        private LocalDateTime claimedAt;
        private Boolean success;
        private String message;
    }
}
