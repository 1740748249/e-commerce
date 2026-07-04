package com.ecommerce.notification.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.notification.enums.IsReadStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_notification")
@ApiModel(value = "ENotification对象", description = "通知表")
public class ENotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "接收通知的店铺ID")
    private Long shopId;

    @ApiModelProperty(value = "类型: 0=新订单, 1=系统通知, 2=促销活动")
    private NotificationType type;

    @ApiModelProperty(value = "通知标题")
    private String title;

    @ApiModelProperty(value = "通知内容")
    private String content;

    @ApiModelProperty(value = "关联订单ID")
    private Long orderId;

    @ApiModelProperty(value = "是否已读: 0=未读, 1=已读")
    private IsReadStatus isRead;

    private LocalDateTime createTime;
}
