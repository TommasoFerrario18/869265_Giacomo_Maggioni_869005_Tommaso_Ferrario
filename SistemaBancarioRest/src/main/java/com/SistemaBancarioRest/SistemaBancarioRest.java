package com.SistemaBancarioRest;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String homepage() {
		// Leggo da file
		return "";
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public String getAccount() {
		DataHandler db = new DataHandler();
		db.connect();
		try {
			List<HashMap<String, String>> results = db.query("SELECT ID, Nome, Cognome FROM Account");
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
			String id = creaID();
			id = (id.charAt(0) != '-') ? id : (String) id.substring(1, id.length());
			String query = "INSERT INTO Account(ID, Nome, Cognome) VALUES('" + id + "', '" + body.get("name") + "', '"
					+ body.get("surname") + "')";
			try {
				if (db.update(query) != 0) {
					return new ResponseEntity<String>(id, HttpStatus.CREATED);
				} else
					return new ResponseEntity<String>("Failed", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
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
			String query = "DELETE FROM Account WHERE ID = '" + accountID + "'";
			try {
				if (db.update(query) != 0)
					return new ResponseEntity<String>("Ok", HttpStatus.OK);
				else
					return new ResponseEntity<String>("Non cancellato no eccezione", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed eccezione", HttpStatus.OK);
			}
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.GET)
	public String getAccountInfo(@PathVariable String accountId) throws SQLException {
		if (accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "SELECT * FROM Account WHERE ID = '" + accountId + "'";
			String queryT = "SELECT * FROM Transazione WHERE (mittente = '" + accountId + "' OR destinatario = '"
					+ accountId + "') ORDER BY dataOra ASC";
			DataHandler db = new DataHandler();
			db.connect();
			List<HashMap<String, String>> res;
			try {
				res = db.query(query);
				res.get(0).put("Transazioni", new Gson().toJson(db.query(queryT)));
				if (res != null)
					return new Gson().toJson(res);
			} catch (SQLException e) {
				db.closeConnection();
			}
		}
		return "";
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.POST)
	public void accountPost(@PathVariable String accountId) {

	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PUT)
	public void accountPut(@PathVariable String accountId) {

	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PATCH)
	public void accountPatch(@PathVariable String accountId) {

	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.HEAD)
	public void accountHead(@PathVariable String accountId) {

	}

	private Double getSaldo(String accountID) throws SQLException {
		if (accountID != null && accountID.length() == 20) {
			String query = "SELECT Saldo FROM Account WHERE ID = '" + accountID + "'";
			DataHandler db = new DataHandler();
			db.connect();
			List<HashMap<String, String>> res;
			try {
				res = db.query(query);
				if (res != null)
					return Double.parseDouble(res.get(0).get("Saldo"));
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				db.closeConnection();
			}
		}
		return 0.0;
	}

	@RequestMapping(value = "/api/transfer", method = RequestMethod.POST)
	public ResponseEntity<String> transferPost(@RequestBody String paramtransazione) {
		Map<String, String> body = bodyParser(paramtransazione);
		if (body != null && body.containsKey("from") && body.containsKey("to") && body.containsKey("amount")) {
			try {
				double saldoPrima = getSaldo(body.get("from"));
				double saldoDopo = saldoPrima - Double.parseDouble(body.get("amount"));
				if (saldoDopo >= 0) {
					String insert = "INSERT INTO Transazione (ID, amount, dataOra, mittente, destinatario) VALUES ("
							+ UUID.randomUUID() + ", " + body.get("amount") + ", datetime('now', 'localtime'), "
							+ body.get("from") + ", " + body.get("to") + ")";
					String updateSaldo = "UPDATE Account SET Saldo = " + saldoDopo + " WHERE ID = '" + body.get("from")
							+ "'";
					// Da fare in una transazione
					DataHandler db = new DataHandler();
					db.connect();

					if (db.update(insert) != 0)
						if (db.update(updateSaldo) != 0)
							return new ResponseEntity<String>("Ok", HttpStatus.OK);
						else
							return new ResponseEntity<String>("Non Eseguita", HttpStatus.OK);
					else
						return new ResponseEntity<String>("Non Eseguita", HttpStatus.OK);
				} else {
					throw new InvalidBalanceException();
				}
			} catch (SQLException e) {

			}
		}
		return new ResponseEntity<String>("Transazione non valida", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/divert", method = RequestMethod.POST)
	public void divertPost() {

	}
}
