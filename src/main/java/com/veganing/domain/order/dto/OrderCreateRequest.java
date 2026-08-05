package com.veganing.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "수령인 이름을 입력해주세요")
    private String recipientName;

    @NotBlank(message = "연락처를 입력해주세요")
    private String recipientPhone;

    @NotBlank(message = "배송지 주소를 입력해주세요")
    private String address;

    private String addressDetail;

    private String memo;
}