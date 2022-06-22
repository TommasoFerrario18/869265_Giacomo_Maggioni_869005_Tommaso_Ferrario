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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

	private List<HashMap<String, String>> eseguiQuery(String query) {
		if (query != null && !query.equalsIgnoreCase(""))
			db.connect();
		try {
			return db.query(query);
		} catch (SQLException e) {
			return null;
		} finally {
			try {
				db.closeConnection();
			} catch (SQLException e) {
				System.out.println("Chiusura database fallita");
			}
		}
	}

	private int eseguiUpdate(String query) {
		if (query != null && !query.equalsIgnoreCase("")) {
			db.connect();
			try {
				return db.update(query);
			} catch (SQLException e) {
				return 0;
			} finally {
				try {
					db.closeConnection();
				} catch (SQLException e) {
					System.out.println("Database non chiuso");
				}
			}
		} else
			return 0;
	}

	private String controlloStringa(String s) {
		if (s != null && !s.equalsIgnoreCase("")) {
			return s.trim();
		}
		return "";
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String homepage() throws FileNotFoundException, IOException {
		return HTMLtoString("src/main/resources/WebUI/index.html");
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public String getAccount() {
		List<HashMap<String, String>> results = eseguiQuery("SELECT ID, Nome, Cognome FROM Account");
		if (results != null)
			return (String) new Gson().toJson(results);
		else
			return "";
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public ResponseEntity<String> creaAccount(@RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("name") && body.containsKey("surname")) {
			String id = creaID();
			id = (id.charAt(0) != '-') ? id : (String) id.substring(1, id.length());
			String query = "INSERT INTO Account(ID, Nome, Cognome, Saldo) VALUES('" + id + "', '" + body.get("name")
					+ "', '" + body.get("surname") + "', 0.0)";
			if (eseguiUpdate(query) != 0) {
				return new ResponseEntity<String>(id, HttpStatus.CREATED);
			} else
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
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
			String query = "DELETE FROM Account WHERE ID = '" + accountID + "'";
			if (eseguiUpdate(query) != 0)
				return new ResponseEntity<String>("Ok", HttpStatus.OK);
			else
				return new ResponseEntity<String>("Non cancellato no eccezione", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.GET)
	public HttpEntity<String> getAccountInfo(@PathVariable String accountId) {
		if (accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "SELECT Nome, Cognome, Saldo FROM Account WHERE ID = '" + accountId + "'";
			String queryT = "SELECT * FROM Transazione WHERE (mittente = '" + accountId + "' OR destinatario = '"
					+ accountId + "') ORDER BY dataOra DESC";
			List<HashMap<String, String>> res = eseguiQuery(query);
			StringBuilder sb = new StringBuilder();
			sb.append(new Gson().toJson(res)).insert(sb.indexOf("}"),
					",\"Transazioni\":" + new Gson().toJson(eseguiQuery(queryT)));
			if (sb != null) {
				HttpHeaders header = new HttpHeaders();
				header.add("X-Sistema-Bancario", res.get(0).get("Nome") + ";" + res.get(0).get("Cognome"));
				return new HttpEntity<String>(sb.toString(), header);
			}
		}
		throw new InvalidAccountException();
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.POST)
	public ResponseEntity<String> accountPost(@PathVariable String accountId, @RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("amount") && accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "";
			double saldo = getSaldo(accountId);
			if (Double.parseDouble(body.get("amount")) > 0) {
				query = "UPDATE Account SET Saldo = " + (saldo + Double.parseDouble(body.get("amount")))
						+ " WHERE ID = '" + accountId + "'";
			} else {
				if ((saldo - Double.parseDouble(body.get("amount"))) > 0)
					query = "UPDATE Account SET Saldo = " + (saldo + Double.parseDouble(body.get("amount")))
							+ " WHERE ID = '" + accountId + "'";
				else
					throw new InvalidBalanceException();
			}
			String uuid = UUID.randomUUID().toString();
			String transazione = "INSERT INTO Transazione(ID, amount, dataOra, mittente, destinatario) VALUES ('" + uuid
					+ "', " + Double.parseDouble(body.get("amount")) + ", datetime('now', 'localtime'), '" + accountId
					+ "', '" + accountId + "')";
			if (eseguiUpdate(query) > 0 && eseguiUpdate(transazione) > 0)
				return new ResponseEntity<String>("Saldo: " + getSaldo(accountId) + " Transazione: " + uuid,
						HttpStatus.OK);
			else
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PUT)
	public ResponseEntity<String> accountPut(@PathVariable String accountId, @RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (accountId != null && !accountId.equalsIgnoreCase("") && body.containsKey("name")
				&& body.containsKey("surname")) {
			String query = "UPDATE Account SET Nome = '" + body.get("name") + "', Cognome = '" + body.get("surname")
					+ "' WHERE ID = '" + accountId + "'";
			if (eseguiUpdate(query) > 0)
				return new ResponseEntity<String>("OK", HttpStatus.OK);
			else
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.PATCH)
	public ResponseEntity<String> accountPatch(@PathVariable String accountId, @RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (accountId != null && !accountId.equalsIgnoreCase("")
				&& (body.containsKey("name") || body.containsKey("surname"))
				&& ((body.get("name") != null && !body.get("name").equalsIgnoreCase(""))
						|| (body.get("surname") != null && !body.get("surname").equalsIgnoreCase("")))) {
			String query = (body.containsKey("name"))
					? "UPDATE Account SET Nome = '" + body.get("name") + "' WHERE ID = '" + accountId + "'"
					: "UPDATE Account SET Cognome = '" + body.get("surname") + "' WHERE ID = '" + accountId + "'";
			if (eseguiUpdate(query) > 0)
				return new ResponseEntity<String>("OK", HttpStatus.OK);
			else
				return new ResponseEntity<String>("Failed", HttpStatus.OK);
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.HEAD)
	public HttpEntity<String> accountHead(@PathVariable String accountId) {
		if (accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "SELECT Nome, Cognome FROM Account WHERE ID = '" + accountId + "'";
			List<HashMap<String, String>> res = eseguiQuery(query);
			if (res != null) {
				HttpHeaders header = new HttpHeaders();
				header.add("X-Sistema-Bancario", res.get(0).get("Nome") + ";" + res.get(0).get("Cognome"));
				return new HttpEntity<String>(header);
			}
		}
		throw new InvalidAccountException();
	}

	private Double getSaldo(String accountID) {
		if (accountID != null && accountID.length() == 20) {
			String query = "SELECT Saldo FROM Account WHERE ID = '" + accountID + "'";
			List<HashMap<String, String>> res = eseguiQuery(query);
			if (res != null)
				return Double.parseDouble((String) res.get(0).get("Saldo"));
		}
		return 0.0;
	}

	@RequestMapping(value = "/api/transfer", method = RequestMethod.POST)
	public ResponseEntity<String> transferPost(@RequestBody String paramtransazione) {
		Map<String, String> body = bodyParser(paramtransazione);
		System.out.println(body.toString());
		if (body != null && body.containsKey("from") && body.containsKey("to") && body.containsKey("amount")) {
			if (Double.parseDouble(body.get("amount")) > 0) {
				double saldoPrima = getSaldo(body.get("from"));
				double saldoDopo = saldoPrima - Double.parseDouble(body.get("amount"));
				if (saldoDopo >= 0) {
					String insert = "INSERT INTO Transazione (ID, amount, dataOra, mittente, destinatario) VALUES ('"
							+ UUID.randomUUID() + "', '" + body.get("amount") + "', datetime('now', 'localtime'), '"
							+ body.get("from") + "', '" + body.get("to") + "')";
					String updateSaldoM = "UPDATE Account SET Saldo = " + saldoDopo + " WHERE ID = '" + body.get("from")
							+ "'";
					String updateSaldoD = "UPDATE Account SET Saldo = "
							+ (getSaldo(body.get("to")) + Double.parseDouble(body.get("amount"))) + " WHERE ID = '"
							+ body.get("to") + "'";
					System.out.println(insert);
					db.startTransaction();
					if (eseguiUpdate(insert) != 0 && eseguiUpdate(updateSaldoM) != 0
							&& eseguiUpdate(updateSaldoD) != 0) {
						db.commit();
						return new ResponseEntity<String>("Ok", HttpStatus.OK);
					} else {
						db.rollback();
						return new ResponseEntity<String>("Non Eseguita", HttpStatus.OK);
					}
				} else
					throw new InvalidBalanceException();
			}
		}
		return new ResponseEntity<String>("Transazione non valida", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/divert", method = RequestMethod.POST)
	public ResponseEntity<String> divertPost(@RequestBody String id) {
		Map<String, String> body = bodyParser(id);
		if (body != null && body.containsKey("id")) {
			String query = "SELECT amount, mittente, destinatario FROM Transazione WHERE ID = '" + body.get("id") + "'";
			List<HashMap<String, String>> res = eseguiQuery(query);
			if ((getSaldo(res.get(0).get("destinatario")) - Double.parseDouble(res.get(0).get("amount"))) >= 0) {
				String insert = "INSERT INTO Transazione (ID, amount, dataOra, mittente, destinatario) VALUES ('"
						+ UUID.randomUUID() + "', " + res.get(0).get("amount") + ", datetime('now', 'localtime'), '"
						+ res.get(0).get("destinatario") + "', '" + res.get(0).get("mittente") + "')";

				String updateSaldoD = "UPDATE Account SET Saldo = "
						+ (getSaldo(res.get(0).get("destinatario")) - Double.parseDouble(res.get(0).get("amount")))
						+ " WHERE ID = '" + res.get(0).get("destinatario") + "'";
				String updateSaldoM = "UPDATE Account SET Saldo = "
						+ (getSaldo(res.get(0).get("mittente")) + Double.parseDouble(res.get(0).get("amount")))
						+ " WHERE ID = '" + res.get(0).get("mittente") + "'";
				db.startTransaction();
				if (eseguiUpdate(insert) != 0 && eseguiUpdate(updateSaldoD) != 0 && eseguiUpdate(updateSaldoM) != 0) {
					db.commit();
					return new ResponseEntity<String>("Ok", HttpStatus.OK);
				} else {
					db.rollback();
					return new ResponseEntity<String>("Non Eseguita", HttpStatus.OK);
				}

			} else
				throw new InvalidBalanceException();
		}
		return new ResponseEntity<String>("Manca ID transazione", HttpStatus.BAD_REQUEST);
	}
}