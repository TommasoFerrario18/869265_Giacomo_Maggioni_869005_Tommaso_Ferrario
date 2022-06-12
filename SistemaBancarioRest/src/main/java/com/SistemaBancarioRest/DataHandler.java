package com.SistemaBancarioRest;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataHandler {

	private Connection c;

	public boolean connect() {
		try {
			c = DriverManager.getConnection("jdbc:sqlite:" + "src/main/resources/DatabaseProgetto.db");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void closeConnection() throws SQLException {
		c.close();
	}

	// Return una lista di hashmap rappresentante la risposta della query
	public List query(String statement) throws SQLException {
		ResultSet result = c.createStatement().executeQuery(statement);
		return resultSetToArrayList(result);
	}

	public int update(String statement) throws SQLException {
		return c.createStatement().executeUpdate(statement);
	}

	// Main per prova / esempio connessione
	public static void main(String[] args) {
		DataHandler db = new DataHandler();
		if (db.connect()) {
			try {
				System.out.println(db.query("select * from account"));
				db.closeConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	// TODO modificare secondo ciò che ci serve, ora è una lista di hashmap
	private List<HashMap<String, String>> resultSetToArrayList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>(50);
		while (rs.next()) {
			HashMap<String, String> row = new HashMap<String, String>(columns);
			for (int i = 1; i <= columns; ++i) {
				row.put(md.getColumnName(i), (String) rs.getObject(i));
			}
			list.add(row);
		}
		return list;
	}

}
