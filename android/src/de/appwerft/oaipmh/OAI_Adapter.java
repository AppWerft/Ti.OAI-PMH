package de.appwerft.oaipmh;

import org.apache.commons.lang.StringEscapeUtils;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class OAI_Adapter {
	private String ENDPOINT;
	private KrollFunction onErrorCallback;
	private KrollFunction onLoadCallback;
	private static final String LCAT = "OAI 📖";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private long startTime;

	public OAI_Adapter(String _endpoint, String _verb, KrollDict _options,
			KrollObject _kroll, Object _onload, Object _onerror) {
		this(_endpoint, 0, 20000, _verb, _options, _kroll, _onload, _onerror);
	}

	public OAI_Adapter(final String _endpoint, final int retries,
			final int connectTimeout, final String _verb,
			final KrollDict _options, final KrollObject _kroll,
			final Object _onload, final Object _onerror) {
		final KrollObject kroll = _kroll;
		this.ENDPOINT = _endpoint;
		if (_onload instanceof KrollFunction) {
			onLoadCallback = (KrollFunction) _onload;
		}
		if (_onerror instanceof KrollFunction) {
			onErrorCallback = (KrollFunction) _onerror;
		}
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams requestParams = new RequestParams();
		requestParams.put("verb", _verb);
		if (_options != null) {
			for (String key : _options.keySet()) {
				requestParams.put(key, _options.get(key));
				Log.d(LCAT,
						key + " " + _options.get(key)
								+ requestParams.toString());
			}
		} else
			Log.w(LCAT, "_options are empty");
		client.setConnectTimeout(connectTimeout);
		Log.d(LCAT, "===================\nURL");
		Log.d(LCAT, ENDPOINT + "?" + requestParams.toString());
		client.setMaxRetriesAndTimeout(retries, connectTimeout);
		client.addHeader("Accept", "text/xml");
		startTime = System.currentTimeMillis();
		client.post(ENDPOINT, requestParams, new AsyncHttpResponseHandler() {
			@Override
			public void onFailure(int status, Header[] header, byte[] response,
					Throwable arg3) {
				if (onErrorCallback != null) {
					KrollDict dict = new KrollDict();
					if (System.currentTimeMillis() - startTime < 1000) {
						dict.put("error", "offline");
						dict.put("message", "Host not reachable");
					} else {
						dict.put("error", "timeout");
						dict.put("time", ""
								+ (System.currentTimeMillis() - startTime));
						dict.put("message", "Server don't answer in 30 sec. ");
					}
					onErrorCallback.call(kroll, dict);
				}
			}

			@Override
			public void onSuccess(int status, Header[] header, byte[] response) {
				String xml = HTTPHelper.getBody(header, response);
				if (xml.length() < 5 || !xml.contains("<?xml")) {
					if (onErrorCallback != null) {
						KrollDict dict = new KrollDict();
						dict.put("error", "html");
						dict.put("html", xml);
						dict.put("message", "Server answer is HTML.");
						onErrorCallback.call(kroll, dict);
					}
					return;
				}
				org.json.jsonjava.JSONObject json = new org.json.jsonjava.JSONObject();
				try {
					String escapedXml = xml;// StringEscapeUtils.unescapeHtml(xml);
					json = org.json.jsonjava.XML.toJSONObject(escapedXml);
				} catch (org.json.jsonjava.JSONException ex) {
					if (onErrorCallback != null) {
						KrollDict dict = new KrollDict();
						dict.put("error", "cannot parse xml");
						dict.put("message", "Issues during XML parsing");
						onErrorCallback.call(kroll, dict);
					}
				}
				JSONObject jsonresult = (JSONObject) KrollHelper
						.toKrollDict(json);
				try {
					if (onLoadCallback != null)
						onLoadCallback.call(kroll, new KrollDict(jsonresult));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (jsonresult.has("OAI-PMH")) {
					try {
						JSONObject oai = jsonresult.getJSONObject("OAI-PMH");
						if (oai.has(_verb)) {
							Log.d(LCAT, _verb + "_found");
							JSONObject record = oai.getJSONObject(_verb);
							if (record.has("resumptionToken")) {
								String resumptionToken = record.getJSONObject(
										"resumptionToken").getString("content");

								_options.put("resumptionToken", resumptionToken);
								Log.d(LCAT, "resumptionToken" + resumptionToken
										+ " found");
								new OAI_Adapter(_endpoint, _verb, _options,
										_kroll, _onload, _onerror);
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}

		});

	}
}
