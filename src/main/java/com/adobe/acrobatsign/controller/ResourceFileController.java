package com.adobe.acrobatsign.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.adobe.acrobatsign.util.Constants;

import io.swagger.v3.oas.annotations.Hidden;

@Controller
public class ResourceFileController {

	/**
	 * Send contract method.
	 *
	 * @return the string
	 */
	@Hidden
	@GetMapping(Constants.SEND_FOR_SIGNATURE_ENDPOINT)
	public String getAgreementPage() {
		return Constants.SEND_FORM_HTML;
	}

	@GetMapping(Constants.GET_HELP)
	public String help() {
		return Constants.GET_HELPX;
	}

	/**
	 * open main page.
	 *
	 * @return the string
	 */
	@Hidden
	@GetMapping(Constants.MAIN_PAGE_ENDPOINT)
	public String sendContractMethod() {
		return Constants.INDEX_HTML;
	}

	@GetMapping(Constants.SEND_PAGE_ENDPOINT)
	public String sendPageMethod() {
		return Constants.SEND_FORM_HTML;
	}
}
