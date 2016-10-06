package de.appwerft.oaipmh;

import org.apache.commons.lang.StringEscapeUtils;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiConvert;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class OAIRequester {
	private String ENDPOINT;
	private KrollFunction onErrorCallback;
	private KrollFunction onLoadCallback;
	private static final String LCAT = "OAIReq 📖";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private long startTime;
	private long mainstartTime = System.currentTimeMillis();
	private String verb;
	private int retries;
	private int connectTimeout;
	private KrollObject kroll;
	public boolean aborted = false;
	AsyncHttpClient client = new AsyncHttpClient();
	private int page = 0;
	private int requestId = -1;
	final int MSG_DO_NEXTREQUEST = 1;

	/* C O N S T R U C T O R */
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
		startAsyncRequest(_options);
	}

	private void startAsyncRequest(final KrollDict _options) {
		if (this.aborted == true) {
			return;
		}
		RequestParams requestParams = new RequestParams();
		requestParams.put("verb", verb);
		if (_options != null) {
			for (String key : _options.keySet()) {
				requestParams.put(key, _options.get(key));
			}
		} else
			Log.w(LCAT, "_options are empty");
		client.setConnectTimeout(connectTimeout);
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
				if (OAIRequester.this.aborted == true) {
					if (client != null)
						client.cancelAllRequests(true);
					return;
				} else {
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
							onLoadCallback.call(kroll,
									new KrollDict(jsonresult));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					try {
						String resumptionToken = jsonresult
								.getJSONObject("OAI-PMH").getJSONObject(verb)
								.getJSONObject("resumptionToken")
								.getString("content");
						_options.put("resumptionToken", resumptionToken);
						page++;
						// send a message to main thread to
						// startAsyncRequest
						startAsyncRequest(_options);
						/*
						 * TiMessenger.sendBlockingMainMessage(new Handler(
						 * TiMessenger.getMainMessenger().getLooper(), new
						 * Handler.Callback() { public boolean
						 * handleMessage(Message msg) { switch (msg.what) { case
						 * MSG_DO_NEXTREQUEST: { startAsyncRequest(_options);
						 * AsyncResult result = (AsyncResult) msg.obj;
						 * result.setResult(null); return true; } } return
						 * false; } }).obtainMessage(MSG_DO_NEXTREQUEST,
						 * _options));
						 */
					} catch (JSONException e) {
						// e.printStackTrace();
					}

				}
			}
		});
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public void abort() {
		this.aborted = true;
		this.client.cancelAllRequests(true);
		this.onLoadCallback = null;
		this.onErrorCallback = null;
	}

	@Override
	public String toString() {
		return "{requestId=" + requestId + ", verb=" + this.verb + ", page="
				+ page + ", duration="
				+ (System.currentTimeMillis() - mainstartTime) + ", aborted="
				+ aborted + "}";
	}
}
