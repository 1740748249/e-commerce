package com.ecommerce.notification.domain.dto;

import com.ecommerce.api.enums.NotificationType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateNotificationDTO {

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @NotNull(message = "通知类型不能为空")
    private NotificationType type;

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200, message = "通知标题最长200字符")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Size(max = 5000, message = "通知内容最长5000字符")
    private String content;

    private Long orderId;
}
