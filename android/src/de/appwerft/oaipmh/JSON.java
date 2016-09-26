package de.appwerft.oaipmh;

import org.appcelerator.kroll.common.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSON {
	final static String LCAT = "importJSON  🌀️";

	public static Object toJSON(Object value) {
		try {
			if (value instanceof org.json.jsonjava.JSONObject) {
				Log.d(LCAT, "was Object >>>>>>>>>>>>>>>");
				org.json.jsonjava.JSONObject foo = (org.json.jsonjava.JSONObject) value;
				JSONObject bar = new JSONObject();
				for (String key : foo.keySet()) {
					Log.d(LCAT, "key=" + key + "  " + foo.get(key).toString());
					bar.put(key, toJSON(foo.get(key)));
				}
				return (Object) bar;
			} else if (value instanceof org.json.jsonjava.JSONArray) {
				org.json.jsonjava.JSONArray foo = (org.json.jsonjava.JSONArray) value;
				JSONArray bar = new JSONArray();
				for (int i = 0; i < foo.length(); i++) {
					bar.put(toJSON(foo.get(i)));
				}
				JSONObject res = new JSONObject();
				res.put("list", bar);
				return (Object) res;
			} else if (value == org.json.jsonjava.JSONObject.NULL) {
				return null;
			} else if (value instanceof String) {
				return value;
			}
		} catch (JSONException e) {
			Log.e(LCAT, e.getMessage());
		}
		return value;
	}

}
