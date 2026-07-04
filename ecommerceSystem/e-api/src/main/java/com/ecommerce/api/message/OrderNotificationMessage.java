package com.ecommerce.api.message;

import com.ecommerce.api.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private Long orderNo;

    /** 商家店铺ID */
    private Long shopId;

    /** 通知类型 */
    private NotificationType type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;
}
