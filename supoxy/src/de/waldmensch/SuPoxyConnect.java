package de.waldmensch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.ParseException;

public class SuPoxyConnect extends Thread {

	/** Sunny Portal address */
	private static final String HOST = "https://www.sunnyportal.com";
	/** Login path, used for posting login data */
	private static final String LOGIN = HOST + "/Templates/Start.aspx";
	/** Path to LiveData JSON */
	private static final String LIVEDATA_JSON = HOST + "/homemanager";

	public static Boolean stop_Thread = false;

	/*
	 * JSON example delivered from Sunny Portal { "__type":"LiveDataUI",
	 * "Timestamp":{"__type":"DateTime","DateTime":"2018-05-21T17:21:38","Kind":
	 * "Unspecified"}, "PV": 57, "FeedIn": 0, "GridConsumption": 996,
	 * "DirectConsumption": null, "SelfConsumption": 57, "SelfSupply": 57,
	 * "TotalConsumption": 1053, "DirectConsumptionQuote": null,
	 * "SelfConsumptionQuote": 100, "AutarkyQuote": 5, "BatteryIn": null,
	 * "BatteryOut": null, "BatteryChargeStatus": null, "OperationHealth": null,
	 * "BatteryStateOfHealth": null, "InfoMessages": [], "WarningMessages": [],
	 * "ErrorMessages": [], "Info": {} }
	 */

	public SuPoxyConnect(String str) {
		super(str);
	}

	public void run() {
		try {

			WebConnect();

		} catch (IllegalStateException | IOException | InterruptedException | NoSuchAlgorithmException
				| KeyStoreException | KeyManagementException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		SuPoxyUtils.log("WebConnect Thread ended " + getName());
	}

	public static void WebConnect() throws IOException, IllegalStateException, InterruptedException,
			NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		SSLContextBuilder builder = new SSLContextBuilder();

		builder.loadTrustMaterial(null, new TrustSelfSignedStrategy() {
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				return true;
			}
		});

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		BasicCookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore)
				.setSSLSocketFactory(sslsf).setRedirectStrategy(new LaxRedirectStrategy())
				.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36")
				.build();

		SuPoxyUtils.log("SuPoxy try to log in");

		login(httpclient);
		SuPoxyUtils.log("SuPoxy login done");

		// enter the endless loop
		while (!stop_Thread) {

			try {
				SuPoxyUtils.log("Getting live Data..");
				getLiveData(httpclient);
				SuPoxyUtils.log("..done");

			} catch (ParseException e) {

				// if we have a parse error it could be that we got the login page instead of
				// JSON
				SuPoxyUtils.log("JSON parse error - try re-login...");
				login(httpclient);
				SuPoxyUtils.log("JSON parse error - re-login done");

			}

			Thread.sleep(SuPoxySettings.requestinterval * 1000);

		}
	}

	private static void getLiveData(CloseableHttpClient httpclient)
			throws FileNotFoundException, IllegalStateException, ParseException {
		HttpGet get = new HttpGet(LIVEDATA_JSON);
		CloseableHttpResponse response;

		try {
			response = httpclient.execute(get);
			HttpEntity entity = response.getEntity();

			SuPoxyDataObject data;
			data = new SuPoxyDataObject(SuPoxyUtils.fromStream(entity.getContent()));

			// handle Portal errors
			if (data.getErrorMessages().length > 0) {

				// Session expired - re-login
				if (data.getErrorMessages()[0].contains("Your session has expired. Please login again")) {
					login(httpclient);
				}

			}

			// if the cache is full we delete the first (oldest) entry before adding a new
			// one
			while (SuPoxyServer.SunnyList.size() > SuPoxySettings.cachesize) {
				SuPoxyServer.SunnyList.remove(0);
			}
			SuPoxyServer.SunnyList.add(data);

		} catch (ClientProtocolException eIO) {
			SuPoxyUtils.log("getLiveData ClientProtocolException");
		} catch (IOException eIO) {
			SuPoxyUtils.log("getLiveData IO Error " + eIO.getMessage());
		}

	}

	private static void login(CloseableHttpClient httpclient) {
		HttpPost httpost = new HttpPost(LOGIN);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$Logincontrol1$txtUserName",
				SuPoxySettings.sunnyuser));
		nvps.add(new BasicNameValuePair("ctl00$ContentPlaceHolder1$Logincontrol1$txtPassword",
				SuPoxySettings.sunnypassword));
		nvps.add(new BasicNameValuePair("__EVENTTARGET", "ctl00$ContentPlaceHolder1$Logincontrol1$LoginBtn"));
		httpost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
		try {

			CloseableHttpResponse response = httpclient.execute(httpost);
			HttpEntity entity = response.getEntity();
			EntityUtils.consume(entity);
			SuPoxyUtils.log("login done");

		} catch (ClientProtocolException eIO) {
			SuPoxyUtils.log("login ClientProtocolException");
		} catch (IOException eIO) {
			SuPoxyUtils.log("login IO Error");
		}
	}

}
