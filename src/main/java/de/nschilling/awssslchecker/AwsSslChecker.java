package de.nschilling.awssslchecker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

public class AwsSslChecker {

	CloudWatchServices cwServices = new CloudWatchServices();
	SnsServices snsServices = new SnsServices();
	HelpServices helpServices = new HelpServices();

	protected void mainFunction() {
		List<String> envVariablesToCheck = new ArrayList<>();
		envVariablesToCheck.add("days_till_expiration");
		envVariablesToCheck.add("ssl_endpoint");
		envVariablesToCheck.add("useSNS");

		if (helpServices.areEnvVariablesMissing(envVariablesToCheck)) {
			System.out.println("mainFunction: One of the environmental variables (" + envVariablesToCheck.toString()
					+ ") is missing or both are missing. Exiting here.");
			throw new RuntimeException("mainFunction: One of the environmental variables ("
					+ envVariablesToCheck.toString() + ") is missing or both are missing. Exiting here.");
		}

		String days_till_expiration = System.getenv("days_till_expiration");
		String ssl_endpoint = System.getenv("ssl_endpoint");
		String useSNS = System.getenv("useSNS");

		System.out.println("mainFunction: start checking the configured http URL " + ssl_endpoint
				+ " and check if certificates expire in the next " + days_till_expiration + " days");
		HashMap<String, Integer> certificateData = new HashMap<String, Integer>();
		certificateData = hTTPsUrlCheck(ssl_endpoint);

		for (Map.Entry<String, Integer> entry : certificateData.entrySet()) {
			cwServices.putDataToCloudWatch(entry.getValue(), entry.getKey());
		}

		if (judgeCertificates(certificateData, Integer.valueOf(days_till_expiration))) {
			if (useSNS.equals("true")) {
				snsServices.publishToSNS(
						"The SSL certificate for the HTTPs endpoint " + ssl_endpoint + " will expire in less than "
								+ days_till_expiration + " days. Please replace the certificate!");
			}
		} else {
			System.out.println("mainFunction: No certificate is expired. No message was sent to SNS.");
		}
	}

	protected boolean judgeCertificates(HashMap<String, Integer> certificateData, int configDaysTillExpiration) {
		System.out.println("judgeCertificates: Start");

		boolean expiredCertAvailable = false;

		if (certificateData.size() == 0) {
			System.out.println("judgeCertificates: There was no data!");
		}

		for (Entry<String, Integer> certificateDataSet : certificateData.entrySet()) {
			int days_to_expiration = certificateDataSet.getValue();
			String alias = certificateDataSet.getKey();
			if (days_to_expiration < configDaysTillExpiration) {
				expiredCertAvailable = true;
				System.out.println("judgeCertificates: Certificate " + alias + " will expire in less than "
						+ configDaysTillExpiration + " days.");
			} else {
				System.out.println("judgeCertificates: Certificate " + alias + " is okay.");
			}
		}

		System.out.println("judgeCertificates: End with " + expiredCertAvailable);
		return expiredCertAvailable;
	}

	protected HashMap<String, Integer> hTTPsUrlCheck(String url) {
		System.out.println("hTTPsUrlCheck: Start with https endpoint: " + url);
		Subsegment xraySubSegment = AWSXRay.beginSubsegment("Get HTTPs Certs");
		Date dateNow = new Date();
		HashMap<String, Integer> certificateData = new HashMap<String, Integer>();
		try {
			URL destinationURL = new URL(url);
			HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
			conn.connect();
			Certificate[] certs = conn.getServerCertificates();
			System.out.println("hTTPsUrlCheck: Number of certificates found in URL: " + certs.length);
			for (Certificate cert : certs) {
				if (cert instanceof X509Certificate) {
					String subjectDn = ((X509Certificate) cert).getSubjectDN().getName();
					Date expire_Date = ((X509Certificate) cert).getNotAfter();
					long days_to_expiration = expire_Date.getTime() - dateNow.getTime();
					days_to_expiration = TimeUnit.DAYS.convert(days_to_expiration, TimeUnit.MILLISECONDS);
					System.out.println("hTTPsUrlCheck: The certificate " + subjectDn + " expires in "
							+ days_to_expiration + " days.");
					certificateData.put(subjectDn, (int) (long) days_to_expiration);
				} else {
					System.out.println("hTTPsUrlCheck: Unknow certificate type, could not get data");
				}
			}
		} catch (MalformedURLException e) {
			System.out.println(
					"hTTPsUrlCheck: There was an error while checking the certificate: MalformedURLException " + e);
			certificateData.clear();
			xraySubSegment.addException(e);
		} catch (IOException e) {
			System.out.println("hTTPsUrlCheck: There was an error while checking the certificate: IOException " + e);
			certificateData.clear();
			xraySubSegment.addException(e);
		} finally {
			AWSXRay.endSubsegment();
		}

		System.out.println("hTTPsUrlCheck: End");
		return certificateData;
	}
}
