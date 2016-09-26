package de.appwerft.oaipmh;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

public class HTTPHelper {
	public static String getBody(Header[] header, byte[] response) {
		String charset = "UTF-8";
		for (int i = 0; i < header.length; i++) {
			if (header[i].getName() == "Content-Type") {
				String[] parts = header[i].getValue().split("; ");
				if (parts != null) {
					charset = parts[1].replace("charset=", "").toUpperCase();
				}
			}
		}
		String xml = "";
		try {
			xml = new String(response, charset);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return xml;
	}
}
