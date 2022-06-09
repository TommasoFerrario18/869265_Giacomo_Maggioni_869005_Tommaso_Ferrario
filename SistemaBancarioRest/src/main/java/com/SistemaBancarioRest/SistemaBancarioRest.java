package com.SistemaBancarioRest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SistemaBancarioRest {

	private Map<String, String> bodyParser(String reqBody){
		Map<String, String> parseBody = new HashMap<String, String>();
		
		String[] values = reqBody.split("&");
		
		for (int i = 0; i < values.length; ++i) {
			String[] coppia = values[i].split("=");
			if (coppia.length != 2)
				continue;
			else
				parseBody.put(coppia[0], coppia[1]);
		}
		return parseBody;
	}
	
	
	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public void ritornaLista() {
		
	}
	
	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public void creaAccount(@RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		
		
	}
	
	@RequestMapping(value = "/api/account", method = RequestMethod.DELETE)
	public void eliminaAccount() {
		
	}
	
	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.GET)
	public void accountGet(@PathVariable String accountID) {
		
	}
	
	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.POST)
	public void accountPost(@PathVariable String accountID) {
		
	}
	
	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PUT)
	public void accountPut(@PathVariable String accountID) {
		
	}
	
	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PATCH)
	public void accountPatch(@PathVariable String accountID) {
		
	}
	
	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.HEAD)
	public void accountHead(@PathVariable String accountID) {
		
	}
	
	@RequestMapping(value = "/api/transfer", method = RequestMethod.POST)
	public void transferPost() {
		
	}
	
	@RequestMapping(value = "/api/divert", method = RequestMethod.POST)
	public void divertPost() {
		
	}
}
