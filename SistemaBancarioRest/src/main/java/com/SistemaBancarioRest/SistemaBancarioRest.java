package com.SistemaBancarioRest;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

@RestController
public class SistemaBancarioRest {

	private Map<String, String> bodyParser(String reqBody) {
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
	public String getAccount() {
		DataHandler db = new DataHandler();

		db.connect();
		try {
			List<HashMap<String, String>> results = db.query("SELECT * FROM Account");
			if (results != null)
				return (String) new Gson().toJson(results);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

	}

	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public ResponseEntity<String> creaAccount(@RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("name") && body.containsKey("surname")) {
			DataHandler db = new DataHandler();
			db.connect();
			String query = "INSERT INTO Account(ID, Nome, Cognome) VALUES('" + creaID() + "', '" + body.get("name")
					+ "', '" + body.get("surname") + "')";
			try {
				if (db.update(query) != 0)
					return new ResponseEntity<String>("OK", HttpStatus.CREATED);
				else
					return new ResponseEntity<String>("Non inserito no eccezione", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Non inserito eccezione", HttpStatus.OK);
			}
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	private String creaID() {
		byte[] id = new byte[10];
		new Random().nextBytes(id);
		return String.format("%x", new BigInteger(id));
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.DELETE)
	public ResponseEntity<String> eliminaAccount(@RequestParam String accountID) {
		if (accountID != null && (!accountID.equalsIgnoreCase(""))) {
			DataHandler db = new DataHandler();
			db.connect();
			String query = "DELETE FROM Account WHERE ID = '" + accountID + "' LIMIT 1";
			try {
				if (db.update(query) != 0)
					return new ResponseEntity<String>("Ok", HttpStatus.OK);
				else
					return new ResponseEntity<String>("Non cancellato no eccezione", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed eccezione", HttpStatus.OK);
			}
		}
		System.out.println(accountID);
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
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
