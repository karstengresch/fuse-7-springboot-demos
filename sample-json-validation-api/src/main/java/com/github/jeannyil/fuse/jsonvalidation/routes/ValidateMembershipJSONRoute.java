package com.github.jeannyil.fuse.jsonvalidation.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 *  Route that validates a sample JSON instance against the Membership JSON schema.
 *  Expects the sample JSON as a Camel message body.
 *
 */
@Component
public class ValidateMembershipJSONRoute extends RouteBuilder {
	
	private static String logName = ValidateMembershipJSONRoute.class.getName();

	@Override
	public void configure() throws Exception {
		
		/**
		 * Catch unexpected exceptions
		 */
		onException(Exception.class)
			.handled(true)
			.maximumRedeliveries(0)
			.log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught exception: ${exception.stacktrace}").id("log-validateMembershipJSON-unexpected")
			.to("direct:common-500").id("to-validateMembershipJSON-500")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-validateMembershipJSON-unexpected-response")
		;
		
		/**
		 * Catch the org.apache.camel.component.jsonvalidator.JsonValidationException exception
		 */
		onException(org.apache.camel.component.jsonvalidator.JsonValidationException.class)
			.handled(true)
			.maximumRedeliveries(0)
			.log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught exception after JSON Schema Validation: ${exception.stacktrace}").id("log-validateMembershipJSON-exception")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, simple("" + HttpStatus.BAD_REQUEST.value()))
			.setProperty(Exchange.HTTP_RESPONSE_TEXT, simple("" + HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.setBody()
				.method("validationResultHelper", "generateKOValidationResult(${exception.message})")
				.id("set-KO-validationResult")
			.marshal().json(JsonLibrary.Jackson, true).id("marshal-KO-validationResult-to-json")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - validateMembership response: headers:[${headers}] - body:[${body}]").id("log-validateMembershipJSON-KO-response")
		;
		
		/**
		 * Validates a sample JSON data against the Membership JSON schema.
		 * Expects the sample JSON as a Camel message body.
		 */
		from("direct:validateMembershipJSON")
			.routeId("validate-membership-json-route")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - Before JSON Schema Validation - Camel Exchange message: in.headers[${headers}] - in.body[${body}]")
			.to("json-validator:json-schema/membership-schema.json?synchronous=true")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - JSON Schema validation is successful")
			.setBody()
				.method("validationResultHelper", "generateOKValidationResult()")
				.id("set-OK-validationResult")
			.marshal().json(JsonLibrary.Jackson, true).id("marshal-OK-validationResult-to-json")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - validateMembership response: headers:[${headers}] - body:[${body}]").id("log-validateMembershipJSON-response")
		;
		
	}

}
