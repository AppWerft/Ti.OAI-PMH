package de.appwerft.oaipmh;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class OAI_Identify {
	private static final String LCAT = "OAI";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private String ENDPOINT;
	KrollFunction onErrorCallback;
	KrollFunction onLoadCallback;

	public OAI_Identify(String _endpoint, KrollDict options, KrollObject _kroll) {
		final KrollObject kroll = _kroll;
		this.ENDPOINT = _endpoint;
		if (options != null) {
			if (options.containsKeyAndNotNull(TiC.PROPERTY_ONLOAD)) {
				Object cb = options.get(TiC.PROPERTY_ONLOAD);
				if (cb instanceof KrollFunction) {
					onLoadCallback = (KrollFunction) cb;
				}

			} else
				Log.e(LCAT, "missing callback 'onload'");
			if (options.containsKeyAndNotNull(TiC.PROPERTY_ONERROR)) {
				Object cb = options.get(TiC.PROPERTY_ONERROR);
				if (cb instanceof KrollFunction) {
					onErrorCallback = (KrollFunction) cb;
				}
			}
			AsyncHttpClient client = new AsyncHttpClient();
			client.setConnectTimeout(2000);
			client.setMaxRetriesAndTimeout(0, 300);
			String url = ENDPOINT + "?verb=Identify";
			client.get(ctx, url, new AsyncHttpResponseHandler() {
				@Override
				public void onFailure(int status, Header[] header,
						byte[] response, Throwable arg3) {
					if (onErrorCallback != null)
						onErrorCallback.call(kroll, new KrollDict());
				}

				@Override
				public void onSuccess(int status, Header[] header,
						byte[] response) {
					String xml = HTTPHelper.getBody(header, response);
					if (xml.length() < 5 || !xml.contains("<?xml")) {
						if (onErrorCallback != null) {
							KrollDict dict = new KrollDict();
							dict.put("error", "html");
							dict.put("html", xml);
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
							onErrorCallback.call(kroll, dict);
						}

					}

					JSONObject jsonresult = (JSONObject) KrollHelper
							.toKrollDict(json);
					try {
						onLoadCallback.call(kroll, new KrollDict(jsonresult));
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}
			});
		} else
			Log.e(LCAT, "missing options");
	}
}
