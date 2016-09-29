package de.appwerft.oaipmh;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.lang3.StringEscapeUtils;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class OAI_Adapter {
	private String ENDPOINT;
	private int timeout = 10000;
	private int retries = 0;
	private KrollFunction onErrorCallback;
	private KrollFunction onLoadCallback;
	private static final String LCAT = "OAI 📖";
	Context ctx = TiApplication.getInstance().getApplicationContext();

	public OAI_Adapter(String _endpoint, String _verb, KrollDict _options,
			KrollObject _kroll, Object _onload, Object _onerror) {
		this(_endpoint, 0, 20000, _verb, _options, _kroll, _onload, _onerror);
	}

	public OAI_Adapter(String _endpoint, int retries, int connectTimeout,
			String _verb, KrollDict _options, KrollObject _kroll,
			Object _onload, Object _onerror) {
		final KrollObject kroll = _kroll;
		this.ENDPOINT = _endpoint;
		if (_onload instanceof KrollFunction) {
			onLoadCallback = (KrollFunction) _onload;
		}
		if (_onerror instanceof KrollFunction) {
			onErrorCallback = (KrollFunction) _onerror;
		}
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("verb", _verb);
		if (_options != null)
			for (String key : _options.keySet()) {
				params.put(key, _options.get(key));
			}
		client.setConnectTimeout(connectTimeout);
		client.setMaxRetriesAndTimeout(retries, connectTimeout);
		client.addHeader("Accept", "text/xml");
		String url = ENDPOINT;
		client.post(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onFailure(int status, Header[] header, byte[] response,
					Throwable arg3) {
				if (onErrorCallback != null) {
					KrollDict dict = new KrollDict();
					dict.put("error", "timeout");
					dict.put("message", "Timeout of server request");
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
					json = org.json.jsonjava.XML.toJSONObject(xml);
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
				if (onLoadCallback == null) {
					Log.e(LCAT, "onLoadCallback is null");
					return;
				}
				if (kroll == null) {
					Log.e(LCAT, "kroll is null");
					return;
				}
				if (jsonresult == null) {
					Log.e(LCAT, "jsonresult is null");
					return;
				}
				try {
					onLoadCallback.call(kroll, new KrollDict(jsonresult));
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}

		});

	}
}
