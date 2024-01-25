package cn.bootx.platform.daxpay.service.func;

import cn.bootx.platform.daxpay.code.PaySyncStatusEnum;
import cn.bootx.platform.daxpay.service.core.order.refund.entity.PayRefundOrder;
import cn.bootx.platform.daxpay.service.core.payment.sync.result.PayGatewaySyncResult;
import lombok.Getter;

/**
 * 支付退款订单同步策略
 * @author xxm
 * @since 2024/1/25
 */
@Getter
public abstract class AbsRefundSyncStrategy implements PayStrategy{

    private PayRefundOrder refundOrder;

    /**
     * 初始化参数
     */
    public void initParam(PayRefundOrder refundOrder){
        this.refundOrder = refundOrder;
    }

    /**
     * 异步支付单与支付网关进行状态比对后的结果
     * @see PaySyncStatusEnum
     */
    public abstract PayGatewaySyncResult doSyncStatus();
}