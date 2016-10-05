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

public class OAIRequester {
	private String ENDPOINT;
	private KrollFunction onErrorCallback;
	private KrollFunction onLoadCallback;
	private static final String LCAT = "OAI 📖";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private long startTime;
	private String verb;
	private int retries;
	private int connectTimeout;
	private KrollObject kroll;
	private boolean stopped = false;
	AsyncHttpClient client;

	public OAIRequester(String _endpoint, String _verb, KrollDict _options,
			KrollObject _kroll, Object _onload, Object _onerror) {
		this(_endpoint, 0, 20000, _verb, _options, _kroll, _onload, _onerror);
	}

	public OAIRequester(final String _endpoint, final int _retries,
			int _connectTimeout, String _verb, KrollDict _options,
			KrollObject _kroll, Object _onload, Object _onerror) {
		this.kroll = _kroll;
		this.ENDPOINT = _endpoint;
		this.verb = _verb;
		this.retries = _retries;
		this.connectTimeout = _connectTimeout;
		if (_onload instanceof KrollFunction) {
			this.onLoadCallback = (KrollFunction) _onload;
		}
		if (_onerror instanceof KrollFunction) {
			this.onErrorCallback = (KrollFunction) _onerror;
		}
		doRequest(_options);
	}

	public void abort() {
		client.cancelAllRequests(true);
		this.onLoadCallback = null;
		this.onErrorCallback = null;
	}

	private void doRequest(final KrollDict _options) {
		if (stopped)
			return;
		client = new AsyncHttpClient();
		RequestParams requestParams = new RequestParams();
		requestParams.put("verb", verb);
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
						if (oai.has(verb)) {
							JSONObject record = oai.getJSONObject(verb);
							if (record.has("resumptionToken")) {
								String resumptionToken = record.getJSONObject(
										"resumptionToken").getString("content");

								_options.put("resumptionToken", resumptionToken);
								Log.d(LCAT, "resumptionToken" + resumptionToken
										+ " found");
								doRequest(_options);
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
