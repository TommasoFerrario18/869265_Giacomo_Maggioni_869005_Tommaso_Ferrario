package com.SistemaBancarioRest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

	private DataHandler db = new DataHandler();

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

	private String HTMLtoString(String fileURL) throws FileNotFoundException, IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileURL)))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String homepage() throws FileNotFoundException, IOException {
		return HTMLtoString("src/main/resources/WebUI/index.html");
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public String getAccount() throws SQLException {
		db.connect();
		try {
			List<HashMap<String, String>> results = db.query("SELECT ID, Nome, Cognome FROM Account");
			if (results != null)
				return (String) new Gson().toJson(results);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			db.closeConnection();
		}
		return "";

	}

	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public ResponseEntity<String> creaAccount(@RequestBody String parametriAccount) throws SQLException {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("name") && body.containsKey("surname")) {
			db.connect();
			String id = creaID();
			id = (id.charAt(0) != '-') ? id : (String) id.substring(1, id.length());
			String query = "INSERT INTO Account(ID, Nome, Cognome, Saldo) VALUES('" + id + "', '" + body.get("name")
					+ "', '" + body.get("surname") + "', 0.0)";
			try {
				if (db.update(query) != 0) {
					return new ResponseEntity<String>(id, HttpStatus.CREATED);
				} else
					return new ResponseEntity<String>("Failed", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
			} finally {
				db.closeConnection();
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
	public ResponseEntity<String> eliminaAccount(@RequestParam String accountID) throws SQLException {
		if (accountID != null && (!accountID.equalsIgnoreCase(""))) {
			db.connect();
			String query = "DELETE FROM Account WHERE ID = '" + accountID + "'";
			try {
				if (db.update(query) != 0)
					return new ResponseEntity<String>("Ok", HttpStatus.OK);
				else
					return new ResponseEntity<String>("Non cancellato no eccezione", HttpStatus.OK);
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed eccezione", HttpStatus.OK);
			} finally {
				db.closeConnection();
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
			db.connect();
			List<HashMap<String, String>> res;
			try {
				res = db.query(query);
				res.get(0).put("Transazioni", new Gson().toJson(db.query(queryT)));
				if (res != null)
					return new Gson().toJson(res);
			} catch (SQLException e) {
				db.closeConnection();
			} finally {
				db.closeConnection();
			}
		}
		return "";
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.POST)
	public ResponseEntity<String> accountPost(@PathVariable String accountId, @RequestBody String parametriAccount)
			throws SQLException {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("amount") && accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "";
			double saldo = getSaldo(accountId);
			if (Double.parseDouble(body.get("amount")) > 0) {
				query = "UPDATE Account SET Saldo = " + (saldo + Double.parseDouble(body.get("amount")))
						+ " WHERE ID = '" + accountId + "'";
			} else {
				if ((saldo - Double.parseDouble(body.get("amount"))) > 0) {
					query = "UPDATE Account SET Saldo = " + (saldo - Double.parseDouble(body.get("amount")))
							+ " WHERE ID = '" + accountId + "'";
				} else {
					throw new InvalidBalanceException();
				}
			}

			db.connect();
			if (db.update(query) > 0) {
				db.closeConnection();
				return new ResponseEntity<String>(getSaldo(accountId) + "", HttpStatus.OK);
			} else {
				db.closeConnection();
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
			}

		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PUT)
	public ResponseEntity<String> accountPut(@PathVariable String accountId, @RequestBody String parametriAccount)
			throws SQLException {
		Map<String, String> body = bodyParser(parametriAccount);
		if (accountId != null && !accountId.equalsIgnoreCase("") && body.containsKey("name")
				&& body.containsKey("surname")) {
			String query = "UPDATE Account SET Nome = '" + body.get("name") + "', Cognome = '" + body.get("surname")
					+ "' WHERE ID = '" + accountId + "'";
			db.connect();
			try {
				if (db.update(query) > 0) {
					return new ResponseEntity<String>("OK", HttpStatus.OK);
				} else {
					return new ResponseEntity<String>("Failed", HttpStatus.OK);
				}
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
			} finally {
				db.closeConnection();
			}
		}
		return new ResponseEntity<String>("Failed", HttpStatus.OK);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PATCH)
	public ResponseEntity<String> accountPatch(@PathVariable String accountId, @RequestBody String parametriAccount)
			throws SQLException {
		Map<String, String> body = bodyParser(parametriAccount);
		if (accountId != null && !accountId.equalsIgnoreCase("")
				&& (body.containsKey("name") || body.containsKey("surname"))
				&& ((body.get("name") != null && !body.get("name").equalsIgnoreCase(""))
						|| (body.get("surname") != null && !body.get("surname").equalsIgnoreCase("")))) {
			String query = "";
			if (body.containsKey("name")) {
				query = "UPDATE Account SET Nome = '" + body.get("name") + "' WHERE ID = '" + accountId + "'";
			} else {
				query = "UPDATE Account SET Cognome = '" + body.get("surname") + "' WHERE ID = '" + accountId + "'";
			}

			db.connect();
			try {
				if (db.update(query) > 0) {
					return new ResponseEntity<String>("OK", HttpStatus.OK);
				} else {
					return new ResponseEntity<String>("Failed", HttpStatus.OK);
				}
			} catch (SQLException e) {
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
			} finally {
				db.closeConnection();
			}
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
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
				System.out.println(res.get(0).toString());
				System.out.println(res.get(0).keySet().toString());
				System.out.println(res.get(0).containsKey("Saldo"));
				System.out.println(res.get(0).get("Saldo"));
				if (res != null)
					return Double.parseDouble((String) res.get(0).get("Saldo"));
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				db.closeConnection();
			}
		}
		return 0.0;
	}

	@RequestMapping(value = "/api/transfer", method = RequestMethod.POST)
	public ResponseEntity<String> transferPost(@RequestBody String paramtransazione) throws SQLException {
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

			} finally {
				db.closeConnection();
			}
		}
		return new ResponseEntity<String>("Transazione non valida", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/divert", method = RequestMethod.POST)
	public void divertPost() {

	}
}
