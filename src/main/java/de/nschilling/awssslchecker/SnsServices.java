package de.nschilling.awssslchecker;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class SnsServices {

	HelpServices helpServices = new HelpServices();

	@SuppressWarnings("unused")
	protected void publishToSNS(String message) {
		List<String> envVariablesToCheck = new ArrayList<>();
		envVariablesToCheck.add("sns_topic_arn");

		if (helpServices.areEnvVariablesMissing(envVariablesToCheck)) {
			System.out.println("publishToSNS: The environmental variable (" + envVariablesToCheck.toString()
					+ ") is missing. Not able to publish to SNS.");
			throw new RuntimeException("publishToSNS: The environmental variable (" + envVariablesToCheck.toString()
					+ ") is missing. Not able to publish to SNS.");
		}
		String sns_topic_arn = System.getenv("sns_topic_arn");

		AmazonSNS snsClient = AmazonSNSClient.builder().build();
		PublishRequest publishRequest = new PublishRequest();
		publishRequest.setSubject("AWS SSL Check - certificate expired");
		publishRequest.setMessage(message);
		publishRequest.setTopicArn(sns_topic_arn);
		PublishResult publishResult = snsClient.publish(publishRequest);
	}
}
