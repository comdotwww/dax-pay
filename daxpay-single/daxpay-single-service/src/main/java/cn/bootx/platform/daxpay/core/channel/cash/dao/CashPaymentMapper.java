package cn.bootx.platform.daxpay.core.channel.cash.dao;

import cn.bootx.platform.daxpay.core.channel.cash.entity.CashPayOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 现金支付
 *
 * @author xxm
 * @since 2021/6/23
 */
@Mapper
public interface CashPaymentMapper extends BaseMapper<CashPayOrder> {

}
