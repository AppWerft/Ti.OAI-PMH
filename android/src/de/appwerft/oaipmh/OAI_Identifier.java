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

public class OAI_Identifier {
	private static final String LCAT = "OAI";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private String ENDPOINT;
	KrollFunction onErrorCallback;
	KrollFunction onLoadCallback;

	public OAI_Identifier(String _endpoint, KrollDict options,
			KrollObject _kroll) {
		final KrollObject kroll = _kroll;
		this.ENDPOINT = _endpoint;
		if (options != null) {
			Log.d(LCAT, "start identify");
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
			client.setConnectTimeout(3000);
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
					org.json.jsonjava.JSONObject json = org.json.jsonjava.XML
							.toJSONObject(xml);
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
