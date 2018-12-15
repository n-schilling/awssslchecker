package de.nschilling.awssslchecker;

import java.util.List;
import java.util.Map;

public class HelpServices {

	protected boolean areEnvVariablesMissing(List<String> variablesToCheck) {
		Map<String, String> envVariables = System.getenv();

		for (String variableToCheck : variablesToCheck) {
			if (!envVariables.containsKey(variableToCheck)) {
				System.out.println("areEnvVariablesMissing: Could not find the variable " + variableToCheck);
				return true;
			}
		}
		return false;
	}
}
