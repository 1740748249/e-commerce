package com.ecommerce.order.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartSyncMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long cartItemId;
    /** Redis 版本号，消费端比对后决定是否跳过 */
    private long version;
}
