package com.fantingame.game.gamecenter.partener.yxcq.apple;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.alibaba.fastjson.JSONObject;
import com.fantingame.game.gamecenter.partener.BaseSdk;
import com.fantingame.game.gamecenter.util.UrlRequestUtils;
import com.google.gson.JsonObject;

/**
 * 英雄传奇-正版苹果
 * @author Administrator
 *
 */
public class YXCQAppleSdk extends BaseSdk {

	private static final Logger logger = Logger.getLogger(YXCQAppleSdk.class);

	private static YXCQAppleSdk ins;

	private static Properties prop;

	private String partnerId;

	private String host;

	private String testHost;

	private String testVersion;

	private ExecutorService executorService;

	private final static String PROTOCOL_HEAD = "https://";

	public static YXCQAppleSdk instance() {
		synchronized (YXCQAppleSdk.class) {
			if (ins == null) {
				ins = new YXCQAppleSdk();
			}
		}

		return ins;
	}

	private YXCQAppleSdk() {
		loadSdkProperties();
	}

	public void reload() {
		loadSdkProperties();
	}

	private void loadSdkProperties() {
		try {
			prop = PropertiesLoaderUtils.loadProperties(new ClassPathResource("yxcqsdk.properties"));
			partnerId = prop.getProperty("AppleSdk.partnerId");
			host = prop.getProperty("AppleSdk.host");
			testHost = prop.getProperty("AppleSdk.testhost");
			testVersion = prop.getProperty("AppleSdk.testVersion");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isTestVersion(String version) {

		if (StringUtils.isEmpty(testVersion) || StringUtils.isEmpty(version)) {
			return false;
		}

		String[] versions = testVersion.split(",");
		for (String ver : versions) {
			if (StringUtils.equalsIgnoreCase(ver, version)) {
				return true;
			}
		}
		return false;
	}

	public boolean checkPayCallbackSign(ApplePaymentObj cb) {
		try {

			String url = null;
			if (isTestVersion(cb.getVersion())) {
				url = PROTOCOL_HEAD + testHost;
			} else {
				url = PROTOCOL_HEAD + host;
			}
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("receipt-data", cb.getReceipt());
			String jsonStr = UrlRequestUtils.executeHttpsByJson(url, jsonObject);
			logger.info("checkPayCallbackSign:" + jsonStr);
			JSONObject jObject = JSONObject.parseObject(jsonStr);
			if (jObject != null && "0".equals(jObject.getString("status"))) {
				JSONObject receipt = jObject.getJSONObject("receipt");
				if ("com.yxcq.fanting.apps".equals(receipt.getString("bid"))) {
					cb.setAppleOrderId(receipt.getString("transaction_id"));
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			logger.error("apple verify error!", e);
		}
		return false;
	}

	public String getPartnerId() {
		return partnerId;
	}

	public String getTestHost() {
		return testHost;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	static class DefaultThreadFactory implements ThreadFactory {
		static final AtomicInteger poolNumber = new AtomicInteger(1);
		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;

		DefaultThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "appleverifyReceipt-" + poolNumber.getAndIncrement() + "-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

	private static class TrustAnyTrustManager implements X509TrustManager

	{

		public void checkClientTrusted(X509Certificate[] chain, String

		authType) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String

		authType) throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
		}
	}

	private static class TrustAnyHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

}
