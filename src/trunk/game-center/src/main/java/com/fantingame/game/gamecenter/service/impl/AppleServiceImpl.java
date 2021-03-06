package com.fantingame.game.gamecenter.service.impl;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fantingame.game.gamecenter.bo.UserToken;
import com.fantingame.game.gamecenter.constant.OrderStatus;
import com.fantingame.game.gamecenter.constant.PartenerEnum;
import com.fantingame.game.gamecenter.constant.WebApiCode;
import com.fantingame.game.gamecenter.dao.ServerListDao;
import com.fantingame.game.gamecenter.exception.ServiceException;
import com.fantingame.game.gamecenter.model.GameServer;
import com.fantingame.game.gamecenter.model.PaymentOrder;
import com.fantingame.game.gamecenter.partener.PaymentObj;
import com.fantingame.game.gamecenter.partener.apple.ApplePaymentObj;
import com.fantingame.game.gamecenter.partener.apple.AppleSdk;
import com.fantingame.game.gamecenter.partener.fantin.AuthAPI;
import com.fantingame.game.gamecenter.partener.fantin.service.AuthBean;
import com.fantingame.game.gamecenter.partener.fantin.service.EucAPIException;
import com.fantingame.game.gamecenter.partener.fantin.service.EucApiResult;
import com.fantingame.game.gamecenter.partener.fantin.service.JUser;
import com.fantingame.game.gamecenter.partener.fantin.util.TradeInfo;
import com.fantingame.game.gamecenter.sdk.GameApiSdk;
import com.fantingame.game.gamecenter.service.BasePartnerService;
import com.fantingame.game.gamecenter.util.json.Json;

public class AppleServiceImpl extends BasePartnerService {
	private static final Logger logger = Logger.getLogger(AppleServiceImpl.class);
	@Autowired
	private ServerListDao serverListDao;
	@Override
	public UserToken login(String token, String partnerId, String serverId, long timestamp, String sign, Map<String, String> params) {
		checkSign(token, partnerId, serverId, timestamp, sign);

		try {
			EucApiResult<AuthBean> result = AuthAPI.validate(token, null, null);
			JUser juser = null;
			if (result == null || result.getResult() == null) {
				if (juser == null) {
					throw new ServiceException(WebApiCode.LOGIN_VALID_FAILD, "登录验证失败");
				}
			}
			juser = result.getResult().getUser();
			GameServer gameServer = serverListDao.getServerByServerIdAndPartenerId(serverId, partnerId);
			return GameApiSdk.getInstance().loadUserToken(juser.getId().toString(), partnerId, serverId, juser.getQn(),gameServer, params);
		} catch (EucAPIException e) {
			throw new ServiceException(WebApiCode.LOGIN_VALID_FAILD, "登录验证失败");
		}
	}

	@Override
	public boolean doPayment(String orderId, String partnerUserId, boolean success, String partnerOrderId, BigDecimal finishAmount, Map<String, String> reqInfo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean doPayment(PaymentObj paymentObj) {
		if (paymentObj == null) {
			logger.error("paymentObj为空");
			return false;
		}
		ApplePaymentObj cb = (ApplePaymentObj) paymentObj;
		logger.info("game id:" + cb.getGameOrderId());
		PaymentOrder order = paymentOrderDao.get(cb.getGameOrderId());
		if (order == null) {
			logger.error("订单为空：" + Json.toJson(cb));
			return false;
		}
		BigDecimal finishAmount = order.getAmount();
		int n = 0;
		while (!AppleSdk.instance().checkPayCallbackSign(cb)) {
			if (n >= 5) {
				logger.error("apple验证失败" + Json.toJson(paymentObj));
				this.paymentOrderDao.updateStatus(cb.getGameOrderId(), OrderStatus.STATUS_ERROR, cb.getAppleOrderId(), finishAmount, cb.getReceipt());
				return false;
			}
			n++;
		}
		if (!this.paymentOrderDao.countOrderByExtInfo(cb.getAppleOrderId())) {
			logger.error("订单已经完成" + Json.toJson(cb));
			return true;
		}
		logger.info("应用订单：" + Json.toJson(order));
		if (order.getStatus() == OrderStatus.STATUS_FINISH) {
			logger.error("订单已经完成" + Json.toJson(cb));
			return true;
		}
		if (order.getAmount().compareTo(finishAmount) != 0) {
			logger.error("订单金额不符：" + Json.toJson(cb));
			return false;
		}
		int gold = (int) (order.getAmount().doubleValue() * 10);
		// 更新订单状态
		if (this.paymentOrderDao.updateStatus(order.getOrderId(), OrderStatus.STATUS_FINISH, cb.getAppleOrderId(), finishAmount, cb.getReceipt())) {
			GameServer gameServer = serverListDao.getServerByServerIdAndPartenerId(order.getServerId(), order.getPartnerId());
			// 请求游戏服，发放游戏货币
			if (!GameApiSdk.getInstance().doPayment(order.getPartnerId(), order.getServerId(), order.getPartnerUserId(), "", order.getOrderId(), finishAmount, gold, "", "",gameServer)) {
				// 如果失败，要把订单置为未支付
				this.paymentOrderDao.updateStatus(order.getOrderId(), OrderStatus.STATUS_NOT_PAY, cb.getAppleOrderId(), finishAmount, cb.getReceipt());
				logger.error("发货失败：" + Json.toJson(cb));
				return false;
			} else {
				logger.info("支付成功：" + Json.toJson(cb));
				return true;
			}
		}
		this.paymentOrderDao.updateStatus(order.getOrderId(), OrderStatus.STATUS_ERROR, cb.getAppleOrderId(), finishAmount, cb.getReceipt());
		logger.error("充值失败：" + Json.toJson(cb));
		return false;
	}

	@Override
	public String createOrder(String partnerId, String serverId, String partnerUserId, BigDecimal amount, String tradeName) {
		TradeInfo info = createOrderInfo(partnerId, serverId, partnerUserId, amount, tradeName);
		GameServer gameServer = serverListDao.getServerByServerIdAndPartenerId(serverId, partnerId);
		int systemGoldSetId = GameApiSdk.getInstance().getSystemGoldSetId(serverId, Double.toString(amount.intValue()),gameServer);
		info.setExectInfo(Integer.toString(systemGoldSetId));
		// 以分为单位
		// info.setReqFee(Integer.toString(new BigDecimal(100).multiply(new
		// BigDecimal(info.getReqFee())).intValue()));
		return Json.toJson(info);
	}

	@Override
	public PartenerEnum getPatenerEnum() {
		return PartenerEnum.AppStore;
	}

}
