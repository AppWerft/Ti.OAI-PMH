/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package de.appwerft.oaipmh;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

// This proxy can be created by calling Oaipmh.createExample({message: "hello world"})
@Kroll.proxy(creatableInModule = OaipmhModule.class)
public class ProviderProxy extends KrollProxy {
	// Standard Debugging variables
	private static final String LCAT = "OAI";
	Context ctx = TiApplication.getInstance().getApplicationContext();
	private String ENDPOINT;
	KrollFunction onErrorCallback;
	KrollFunction onLoadCallback;

	// Constructor
	public ProviderProxy() {
		super();
	}

	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
		if (options.containsKeyAndNotNull(TiC.PROPERTY_URL)) {
			final URI uri;
			try {
				uri = new URI(options.getString(TiC.PROPERTY_URL));
				this.ENDPOINT = uri.toString();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

	}

	// Methods
	@Kroll.method
	public void identify(KrollDict options) {
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
			client.get(ctx, url, new XMLResponseHandler());
		} else
			Log.e(LCAT, "missing options");
	}

	private final class XMLResponseHandler extends AsyncHttpResponseHandler {
		@Override
		public void onFailure(int status, Header[] header, byte[] response,
				Throwable arg3) {
			if (onErrorCallback != null)
				onErrorCallback.call(getKrollObject(), new KrollDict());
		}

		@Override
		public void onSuccess(int status, Header[] header, byte[] response) {
			String charset = "UTF-8";
			for (int i = 0; i < header.length; i++) {
				if (header[i].getName() == "Content-Type") {
					String[] parts = header[i].getValue().split("; ");
					if (parts != null) {
						charset = parts[1].replace("charset=", "")
								.toUpperCase();
					}
				}
			}
			String xml = "";
			try {
				xml = new String(response, charset);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			org.json.jsonjava.JSONObject json = org.json.jsonjava.XML
					.toJSONObject(xml);
			JSONObject jsonresult = (JSONObject) de.appwerft.oaipmh.JSON
					.toJSON(json);
			Log.d(LCAT, jsonresult.toString());

			try {
				onLoadCallback
						.call(getKrollObject(), new KrollDict(jsonresult));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}