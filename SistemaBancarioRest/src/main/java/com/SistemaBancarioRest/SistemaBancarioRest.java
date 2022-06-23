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

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ResponseEntity<String> homepage() throws FileNotFoundException, IOException {
		return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8")
				.body(HTMLtoString("src/main/resources/WebUI/index.html"));
	}

	@RequestMapping(value = "/transfer", method = RequestMethod.GET)
	public ResponseEntity<String> transfer() throws FileNotFoundException, IOException {
		return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8")
				.body(HTMLtoString("src/main/resources/WebUI/transfer.html"));
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.GET)
	public ResponseEntity<String> getAccount() {
		List<HashMap<String, String>> results = eseguiQuery("SELECT ID, Nome, Cognome FROM Account");
		if (results != null)
			return ResponseEntity.ok().header("Content-Type", "application/json")
					.body((String) new Gson().toJson(results));
		else
			return ResponseEntity.ok().header("Content-Type", "application/json").body("[]");
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.POST)
	public ResponseEntity<String> creaAccount(@RequestBody String parametriAccount) {
		Map<String, String> body = bodyParser(parametriAccount);
		if (body != null && body.containsKey("name") && body.containsKey("surname")) {
			String id = creaID();
			id = (id.charAt(0) != '-') ? id : (String) id.substring(1, id.length());
			String query = "INSERT INTO Account(ID, Nome, Cognome, Saldo) VALUES('" + id + "', '" + body.get("name")
					+ "', '" + body.get("surname") + "', 0.0)";
			if (eseguiUpdate(query) != 0)
				return ResponseEntity.ok().header("Content-Type", "application/json")
						.body("[{\"accountId\": \"" + id + "\"}]");
			else
				return ResponseEntity.ok().body("Failed");
		}
		return ResponseEntity.badRequest().body("Failed");
	}

	@RequestMapping(value = "/api/account", method = RequestMethod.DELETE)
	public ResponseEntity<String> eliminaAccount(@RequestParam String accountID) {
		if (accountID != null && (!accountID.equalsIgnoreCase(""))) {
			String query = "DELETE FROM Account WHERE ID = '" + accountID + "'";
			if (eseguiUpdate(query) != 0)
				return new ResponseEntity<String>("Cancellato", HttpStatus.OK);
			else
				return new ResponseEntity<String>("Non Cancellato", HttpStatus.NOT_MODIFIED);
		}
		return new ResponseEntity<String>("Non Cancellato", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.GET)
	public ResponseEntity<String> getAccountInfo(@PathVariable String accountId) {
		if (accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "SELECT Nome, Cognome, Saldo FROM Account WHERE ID = '" + accountId + "'";
			String queryT = "SELECT * FROM Transazione WHERE (mittente = '" + accountId + "' OR destinatario = '"
					+ accountId + "') ORDER BY dataOra DESC";
			List<HashMap<String, String>> res = eseguiQuery(query);
			StringBuilder sb = new StringBuilder();
			sb.append(new Gson().toJson(res)).insert(sb.indexOf("}"),
					",\"Transazioni\":" + new Gson().toJson(eseguiQuery(queryT)));
			if (sb != null) {
				HttpHeaders headers = new HttpHeaders();
				headers.add("X-Sistema-Bancario", res.get(0).get("Nome") + ";" + res.get(0).get("Cognome"));
				headers.add("Content-Type", "application/json");
				return ResponseEntity.ok().headers(headers).body(sb.toString());
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
				query = creaUpdate((saldo + Double.parseDouble(body.get("amount"))), accountId);
			} else {
				if ((saldo - Double.parseDouble(body.get("amount"))) > 0)
					query = creaUpdate((saldo + Double.parseDouble(body.get("amount"))), accountId);
				else
					throw new InvalidBalanceException();
			}
			String uuid = UUID.randomUUID().toString();
			String transazione = creaInsert(uuid, Double.parseDouble(body.get("amount")), accountId, accountId);
			db.startTransaction();
			if (eseguiUpdate(query) > 0 && eseguiUpdate(transazione) > 0) {
				db.commit();
				return ResponseEntity.ok().header("Content-Type", "application/json")
						.body("[{\"Saldo\": " + getSaldo(accountId) + ", \"Transazione\": \"" + uuid + "\"}]");
			} else {
				db.rollback();
				return new ResponseEntity<String>("Failed", HttpStatus.NOT_MODIFIED);
			}
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
				return new ResponseEntity<String>("Failed", HttpStatus.NOT_MODIFIED);
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
				return new ResponseEntity<String>("Failed", HttpStatus.NOT_MODIFIED);
		}
		return new ResponseEntity<String>("Failed", HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/api/account/{accountId}", method = RequestMethod.HEAD)
	public ResponseEntity<String> accountHead(@PathVariable String accountId) {
		if (accountId != null && !accountId.equalsIgnoreCase("")) {
			String query = "SELECT Nome, Cognome FROM Account WHERE ID = '" + accountId + "'";
			List<HashMap<String, String>> res = eseguiQuery(query);
			if (res != null) {
				HttpHeaders header = new HttpHeaders();
				header.add("X-Sistema-Bancario", res.get(0).get("Nome") + ";" + res.get(0).get("Cognome"));
				return ResponseEntity.ok().headers(header).body(null);
			}
		}
		throw new InvalidAccountException();
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
					String insert = creaInsert(UUID.randomUUID().toString(), Double.parseDouble(body.get("amount")),
							body.get("to"), body.get("from"));
					String updateSaldoM = creaUpdate(saldoDopo, body.get("from"));
					String updateSaldoD = creaUpdate(
							(getSaldo(body.get("to")) + Double.parseDouble(body.get("amount"))), body.get("to"));
					System.out.println(insert);
					db.startTransaction();
					if (eseguiUpdate(insert) != 0 && eseguiUpdate(updateSaldoM) != 0
							&& eseguiUpdate(updateSaldoD) != 0) {
						db.commit();
						return new ResponseEntity<String>("OK", HttpStatus.OK);
					} else {
						db.rollback();
						return new ResponseEntity<String>("Non Eseguita", HttpStatus.NOT_MODIFIED);
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
				String insert = creaInsert(UUID.randomUUID().toString(), Double.parseDouble(res.get(0).get("amount")),
						res.get(0).get("mittente"), res.get(0).get("destinatario"));
				String updateSaldoD = creaUpdate(
						(getSaldo(res.get(0).get("destinatario")) - Double.parseDouble(res.get(0).get("amount"))),
						res.get(0).get("destinatario"));
				String updateSaldoM = creaUpdate(
						(getSaldo(res.get(0).get("mittente")) + Double.parseDouble(res.get(0).get("amount"))),
						res.get(0).get("mittente"));
				db.startTransaction();
				if (eseguiUpdate(insert) != 0 && eseguiUpdate(updateSaldoD) != 0 && eseguiUpdate(updateSaldoM) != 0) {
					db.commit();
					return new ResponseEntity<String>("OK", HttpStatus.OK);
				} else {
					db.rollback();
					return new ResponseEntity<String>("Non Eseguita", HttpStatus.NOT_MODIFIED);
				}

			} else
				throw new InvalidBalanceException();
		}
		return new ResponseEntity<String>("Manca ID transazione", HttpStatus.BAD_REQUEST);
	}

	/* Metodi utili */
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
			s = s.replaceAll("[^\\w]+", " ");
			s = s.replaceAll("[^A-Za-z0-9]+", "");
			return s.trim();
		}
		return "";
	}

	private String creaID() {
		byte[] id = new byte[10];
		new Random().nextBytes(id);
		return String.format("%x", new BigInteger(id));
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

	private String creaInsert(String uuid, double amount, String destinatario, String mittente) {
		return "INSERT INTO Transazione (ID, amount, dataOra, mittente, destinatario) VALUES ('" + uuid + "', " + amount
				+ ", datetime('now', 'localtime'), '" + mittente + "', '" + destinatario + "')";
	}

	private String creaUpdate(double amount, String id) {
		return "UPDATE Account SET Saldo = " + amount + " WHERE ID = '" + id + "'";
	}
}