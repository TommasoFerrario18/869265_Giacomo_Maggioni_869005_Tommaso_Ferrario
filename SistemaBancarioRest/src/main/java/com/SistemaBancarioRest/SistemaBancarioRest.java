package com.SistemaBancarioRest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SistemaBancarioRest {

	
	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public void ritornaLista() {
		
	}
	
	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public void creaAccount() {
		
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
