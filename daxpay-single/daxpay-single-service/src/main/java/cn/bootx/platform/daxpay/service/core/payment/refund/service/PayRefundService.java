package cn.bootx.platform.daxpay.service.core.payment.refund.service;

import cn.bootx.platform.common.core.exception.RepetitiveOperationException;
import cn.bootx.platform.common.core.util.ValidationUtil;
import cn.bootx.platform.daxpay.code.PayStatusEnum;
import cn.bootx.platform.daxpay.service.core.payment.refund.factory.PayRefundStrategyFactory;
import cn.bootx.platform.daxpay.service.core.record.pay.entity.PayOrder;
import cn.bootx.platform.daxpay.service.core.record.pay.service.PayOrderService;
import cn.bootx.platform.daxpay.exception.pay.PayUnsupportedMethodException;
import cn.bootx.platform.daxpay.service.func.AbsPayRefundStrategy;
import cn.bootx.platform.daxpay.param.pay.RefundChannelParam;
import cn.bootx.platform.daxpay.param.pay.RefundParam;
import cn.bootx.platform.daxpay.param.pay.SimpleRefundParam;
import cn.bootx.platform.daxpay.result.pay.RefundResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支付退款服务
 * @author xxm
 * @since 2023/12/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayRefundService {

    private final PayRefundAssistService payRefundAssistService;;

    private final PayOrderService payOrderService;

    private final LockTemplate lockTemplate;

    /**
     * 支付退款
     */
    @Transactional(rollbackFor = Exception.class )
    public RefundResult refund(RefundParam param){
        return this.refund(param,false);
    }

    /**
     * 简单退款
     */
    @Transactional(rollbackFor = Exception.class )
    public RefundResult simpleRefund(SimpleRefundParam param){
        ValidationUtil.validateParam(param);
        // 构建退款参数
        RefundParam refundParam = new RefundParam();
        BeanUtil.copyProperties(param,refundParam);
        RefundChannelParam channelParam = new RefundChannelParam()
                .setAmount(param.getAmount())
                .setChannelExtra(param.getChannelExtra());
        refundParam.setRefundChannels(Collections.singletonList(channelParam));
        return this.refund(refundParam,true);
    }

    /**
     * 支付退款方法
     * @param param 退款参数
     * @param simple 是否简单退款
     */
    private RefundResult refund(RefundParam param, boolean simple){
        // 检查获取支付订单, 同时设置退款参数中对应的支付通道参数
        PayOrder payOrder = payRefundAssistService.getPayOrderAndCheckByRefundParam(param, simple);
        // 参数校验
        ValidationUtil.validateParam(param);

        // 加锁
        LockInfo lock = lockTemplate.lock("payment:refund:" + payOrder.getId());
        if (Objects.isNull(lock)){
            throw new RepetitiveOperationException("退款处理中，请勿重复操作");
        }

        try {
            // 退款上下文初始化
            payRefundAssistService.initRefundContext(param);
            // 是否全部退款
            if (param.isRefundAll()){
                // 全部退款根据支付订单的退款信息构造退款参数
                List<RefundChannelParam> channelParams = payOrder.getRefundableInfos()
                        .stream()
                        .map(o -> new RefundChannelParam().setChannel(o.getChannel())
                                .setAmount(o.getAmount()))
                        .collect(Collectors.toList());
                param.setRefundChannels(channelParams);
            }
            return this.refundByChannel(param,payOrder);
        } finally {
            lockTemplate.releaseLock(lock);
        }
    }

    /**
     * 分支付通道进行退款
     */
    public RefundResult refundByChannel(RefundParam refundParam, PayOrder payOrder){
        List<RefundChannelParam> refundChannels = refundParam.getRefundChannels();
        // 1.获取退款参数方式，通过工厂生成对应的策略组
        List<AbsPayRefundStrategy> payRefundStrategies = PayRefundStrategyFactory.createAsyncLast(refundChannels);
        if (CollectionUtil.isEmpty(payRefundStrategies)) {
            throw new PayUnsupportedMethodException();
        }

        // 2.初始化退款策略的参数
        payRefundStrategies.forEach(refundStrategy -> refundStrategy.initRefundParam(payOrder, refundParam));

        try {
            // 3.退款前准备
            payRefundStrategies.forEach(AbsPayRefundStrategy::doBeforeRefundHandler);

            // 4.执行退款策略
            payRefundStrategies.forEach(AbsPayRefundStrategy::doRefundHandler);
        }
        catch (Exception e) {
            // 记录退款失败的记录
            payRefundAssistService.saveOrder(refundParam,payOrder);
            throw e;
        }

        // 5.退款成功后处理
        this.successHandler(refundParam, payOrder);

        // 返回结果
        return new RefundResult();
    }

    /**
     * 支付订单处理
     */
    private void successHandler(RefundParam refundParam, PayOrder payOrder) {
        Integer amount = refundParam.getRefundChannels().stream()
                .map(RefundChannelParam::getAmount)
                .reduce(0, Integer::sum);
        // 剩余可退款余额
        int refundableBalance = payOrder.getRefundableBalance() - amount;

        // 退款完成
        if (refundableBalance == 0) {
            payOrder.setStatus(PayStatusEnum.REFUNDED.getCode());
        }
        else {
            payOrder.setStatus(PayStatusEnum.PARTIAL_REFUND.getCode());
        }
        payOrder.setRefundableBalance(refundableBalance);
        payOrderService.updateById(payOrder);
        payRefundAssistService.saveOrder(refundParam,payOrder);
    }
}