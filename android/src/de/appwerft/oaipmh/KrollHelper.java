package de.appwerft.oaipmh;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KrollHelper {
	public static Object toKrollDict(Object value) {
		try {
			if (value instanceof org.json.jsonjava.JSONObject) {
				org.json.jsonjava.JSONObject foo = (org.json.jsonjava.JSONObject) value;
				JSONObject bar = new JSONObject();
				for (String key : foo.keySet()) {
					bar.put(key, toKrollDict(foo.get(key)));
				}
				return (Object) bar;
			} else if (value instanceof org.json.jsonjava.JSONArray) {
				org.json.jsonjava.JSONArray foo = (org.json.jsonjava.JSONArray) value;
				JSONArray bar = new JSONArray();
				for (int i = 0; i < foo.length(); i++) {
					bar.put(toKrollDict(foo.get(i)));
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
		}
		return value;
	}
}
