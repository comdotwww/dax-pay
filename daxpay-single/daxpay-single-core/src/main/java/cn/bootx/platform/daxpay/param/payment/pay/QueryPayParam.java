package cn.bootx.platform.daxpay.param.payment.pay;

import cn.bootx.platform.daxpay.param.PaymentCommonParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付单查询参数
 * @author xxm
 * @since 2024/1/16
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(title = "支付单查询参数")
public class QueryPayParam extends PaymentCommonParam {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商户订单号")
    private String outTradeNo;
}
