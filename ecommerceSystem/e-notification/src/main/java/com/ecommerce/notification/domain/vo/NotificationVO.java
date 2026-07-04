package com.ecommerce.notification.domain.vo;

import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.notification.enums.IsReadStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "通知消息")
public class NotificationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("通知ID")
    private Long id;

    @ApiModelProperty("接收通知的店铺ID")
    private Long shopId;

    @ApiModelProperty("类型: 0=新订单, 1=系统通知, 2=促销活动")
    private NotificationType type;

    @ApiModelProperty("通知标题")
    private String title;

    @ApiModelProperty("通知内容")
    private String content;

    @ApiModelProperty("关联订单ID")
    private Long orderId;

    @ApiModelProperty("是否已读: 0=未读, 1=已读")
    private IsReadStatus isRead;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
