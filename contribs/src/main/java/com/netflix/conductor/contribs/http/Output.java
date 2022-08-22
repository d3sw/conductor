package com.netflix.conductor.contribs.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.netflix.conductor.contribs.http.GenericHttpTask.CUSTOM_FAILURE_REASON_PARAMETER_NAME;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Output {

	private ResponseFailureReason responseFailureReason;

	private Validate validate;

	@JsonProperty(CUSTOM_FAILURE_REASON_PARAMETER_NAME)
	public ResponseFailureReason getResponseFailureReason() {
		return responseFailureReason;
	}

	public void setResponseFailureReason(ResponseFailureReason responseFailureReason) {
		this.responseFailureReason = responseFailureReason;
	}

	public Validate getValidate() {
		return validate;
	}

	public void setValidate(Validate validate) {
		this.validate = validate;
	}
}