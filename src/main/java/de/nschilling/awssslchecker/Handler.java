package de.nschilling.awssslchecker;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class Handler implements RequestStreamHandler {

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
		System.out.println("handleRequest: Start");
		AwsSslChecker awsSslChecker = new AwsSslChecker();
		awsSslChecker.mainFunction();
		System.out.println("handleRequest: End");
	}
}
