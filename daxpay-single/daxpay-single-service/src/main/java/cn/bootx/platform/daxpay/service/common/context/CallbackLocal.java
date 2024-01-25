package cn.bootx.platform.daxpay.service.common.context;

import cn.bootx.platform.daxpay.service.code.PayCallbackStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 回调信息上下文
 * @author xxm
 * @since 2024/1/24
 */
@Data
@Accessors(chain = true)
public class CallbackLocal {

    /** 回调参数内容 */
    private Map<String, String> callbackParam = new HashMap<>();

    /** 本地订单ID */
    private Long orderId;

    /**
     * 第三方支付平台订单号
     * 1. 如付款码支付直接成功时会出现
     */
    private String gatewayOrderNo;

    /** 网关状态 */
    private String gatewayPayStatus;

    /** 金额(元) */
    private String amount;

    /** 完成时间(支付/退款) */
    private LocalDateTime finishTime;

    /** 支付修复ID */
    private Long payRepairOrderId;

    /**
     * 回调处理状态
     * @see PayCallbackStatusEnum
     */
    private PayCallbackStatusEnum callbackStatus = PayCallbackStatusEnum.SUCCESS;

    /** 提示信息 */
    private String msg;
}