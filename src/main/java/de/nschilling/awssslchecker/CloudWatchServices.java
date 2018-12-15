package de.nschilling.awssslchecker;

import java.util.Date;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class CloudWatchServices {

	protected void putDataToCloudWatch(int daysTillExpiration, String certName) {
		final AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.defaultClient();

		MetricDatum metricDatum1 = new MetricDatum().withMetricName(certName).withTimestamp(new Date())
				.withValue((double) daysTillExpiration).withUnit(StandardUnit.Count);

		PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest();
		putMetricDataRequest.setNamespace("AwsSslChecker");
		putMetricDataRequest.getMetricData().add(metricDatum1);
		cloudWatch.putMetricData(putMetricDataRequest);
	}
}
