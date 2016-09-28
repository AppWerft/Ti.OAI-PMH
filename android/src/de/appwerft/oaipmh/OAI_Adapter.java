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
	private KrollFunction onErrorCallback;
	private KrollFunction onLoadCallback;
	private static final String LCAT = "OAI 📖";
	Context ctx = TiApplication.getInstance().getApplicationContext();

	public OAI_Adapter(String _endpoint, String _verb, KrollDict _options,
			KrollObject _kroll, Object _onload, Object _onerror) {
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
		client.setConnectTimeout(5000);
		client.addHeader("Accept", "text/xml");

		String url = ENDPOINT;
		Log.d(LCAT, ">>>>>>>>>>>>>>\n" + url);
		client.post(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onFailure(int status, Header[] header, byte[] response,
					Throwable arg3) {
				if (onErrorCallback != null)
					onErrorCallback.call(kroll, new KrollDict());
			}

			@Override
			public void onSuccess(int status, Header[] header, byte[] response) {
				String xml = HTTPHelper.getBody(header, response);
				Log.d(LCAT, "XML length=" + (xml.length()) / 1000 + "kB");
				if (!xml.contains("<?xml ")) {
					if (onErrorCallback != null) {
						onErrorCallback.call(kroll, new KrollDict());
					}
					Log.e(LCAT,
							"response is not valide XML:\n"
									+ xml.substring(0, 512));
					return;
				}
				org.json.jsonjava.JSONObject json = org.json.jsonjava.XML
						.toJSONObject(xml);
				JSONObject jsonresult = (JSONObject) KrollHelper
						.toKrollDict(json);
				// StringEscapeUtils.unescapeHtml
				Log.d(LCAT, "XML converting to JSON succeed");
				Log.d(LCAT, jsonresult.toString());
				if (onLoadCallback != null) {
					Log.d(LCAT, "onLoadCallback != null");
					try {
						Log.d(LCAT, "onLoadCallback.call");
						onLoadCallback.call(kroll, new KrollDict(jsonresult));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else
					Log.e(LCAT, "onLoadCallback missing");

			}
		});

	}
}
