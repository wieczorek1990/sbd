import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.ButtonGroup;
import javax.swing.CellEditor;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JScrollPane;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class SBD {

	private static SBD sbd;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new SBD();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public SQLTable albumTable;
	public SQLTable artistTable;
	private boolean connected = false;
	private Connection connection;
	private JFrame frame;
	public SQLTable mainTable;
	private String mode = "update";
	private JTextField searchTextField;
	private ArrayList<ArrayList<PreparedStatement>> selects = new ArrayList<ArrayList<PreparedStatement>>();

	public SQLTable trackTable;

	public SBD() throws SQLException {
		sbd = this;
		establishConnection();
		if (connected) {
			initializeFrame();
			initializeTables();
			setSelects(0, false, 0);
			updateAllTables();
			initializeHints();
		} else {
			connectionFailure();
		}
	}

	public void clearSelection(SQLTable table) {
		if (mainTable != table)
			mainTable.clearSelection();
		if (artistTable != table)
			artistTable.clearSelection();
		if (albumTable != table)
			albumTable.clearSelection();
		if (trackTable != table)
			trackTable.clearSelection();
	}

	private void closing() {
		try {
			if (connected) {
				mainTable.close();
				artistTable.close();
				albumTable.close();
				trackTable.close();
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	private void connectionFailure() {
		JOptionPane.showMessageDialog(getFrame(),
				"Couldn't connect to server!\nClosing...");
		closing();
	}

	private void establishConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Properties properties = new Properties();
			properties.put("user", "annonymous");
			properties.put("password", "basictag");
			String host = "localhost";
			String port = "3306";
			connection = DriverManager.getConnection("jdbc:mysql://" + host
					+ ":" + port
					+ "/basictag?useUnicode=true&characterEncoding=utf8",
					properties);
			if (connection.isValid(0)) {
				PreparedStatement pst = connection
						.prepareStatement("SET NAMES 'utf8' COLLATE 'utf8_general_ci';");
				pst.execute();
				setConnected(true);
			} else {
				connectionFailure();
			}
		} catch (SQLException e) {
			connectionFailure();
		} catch (ClassNotFoundException e) {
			connectionFailure();
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public Component getFrame() {
		return frame;
	}

	public String getMode() {
		return mode;
	}

	public void home() {
		setSelects(0, false, 0);
		cancelEditingTables(null);
		clearSelection(null);
		updateAllTables();
	}

	private void initializeFrame() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1024, 768);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				closing();
			}
		});
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 100, 100, 100 };
		gridBagLayout.rowHeights = new int[] { 19, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 1.0 };
		frame.getContentPane().setLayout(gridBagLayout);

		JLabel lblSearch = new JLabel("Search:");
		GridBagConstraints gbc_lblSearch = new GridBagConstraints();
		gbc_lblSearch.anchor = GridBagConstraints.EAST;
		gbc_lblSearch.insets = new Insets(0, 0, 5, 5);
		gbc_lblSearch.gridx = 0;
		gbc_lblSearch.gridy = 0;
		frame.getContentPane().add(lblSearch, gbc_lblSearch);

		searchTextField = new JTextField();
		searchTextField
				.setToolTipText("<html>Searches only through Tracks that are bound to Albums.<br>Usage: field_name0='value' .. field_nameN='value'<br>Field names are case insensitive.<br>Values are case sensitive and match the begining of exact value.</html>");
		searchTextField.addActionListener(new SearchActionListener(sbd,
				searchTextField));

		GridBagConstraints gbc_searchTextField = new GridBagConstraints();
		gbc_searchTextField.insets = new Insets(0, 0, 5, 5);
		gbc_searchTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_searchTextField.gridx = 1;
		gbc_searchTextField.gridy = 0;
		frame.getContentPane().add(searchTextField, gbc_searchTextField);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 1;
		gbc_scrollPane_2.gridwidth = 3;
		frame.getContentPane().add(scrollPane_2, gbc_scrollPane_2);

		mainTable = new SQLTable(sbd);
		mainTable.setName("mainTable");
		scrollPane_2.setViewportView(mainTable);

		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 2;
		gbc_scrollPane_3.gridy = 2;
		frame.getContentPane().add(scrollPane_3, gbc_scrollPane_3);

		trackTable = new SQLTable(sbd);
		trackTable.setName("trackTable");
		scrollPane_3.setViewportView(trackTable);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		frame.getContentPane().add(scrollPane_1, gbc_scrollPane_1);

		artistTable = new SQLTable(sbd);
		artistTable.setName("artistTable");
		scrollPane_1.setViewportView(artistTable);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 2;
		frame.getContentPane().add(scrollPane, gbc_scrollPane);

		albumTable = new SQLTable(sbd);
		albumTable.setName("albumTable");
		scrollPane.setViewportView(albumTable);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu mnNewMenu = new JMenu("Database");
		menuBar.add(mnNewMenu);

		JMenuItem mntmHome = new JMenuItem("Home");
		mntmHome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				home();
			}
		});
		mnNewMenu.add(mntmHome);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closing();
			}
		});
		mnNewMenu.add(mntmExit);

		JMenu mnNewMenu_1 = new JMenu("Row");
		menuBar.add(mnNewMenu_1);

		JMenuItem mntmAdd = new JMenuItem("Insert");
		mntmAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new InsertRowFrame(frame.getWidth() / 2, frame.getHeight() / 2,
						300, 300, sbd);
			}
		});
		mnNewMenu_1.add(mntmAdd);

		JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new DeleteRowFrame(frame.getWidth() / 2, frame.getHeight() / 2,
						300, 200, sbd);
			}
		});
		mnNewMenu_1.add(mntmDelete);

		JMenu mnNewMenu_2 = new JMenu("Function");
		menuBar.add(mnNewMenu_2);

		JMenuItem mntmF = new JMenuItem("Summary");
		mntmF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					PreparedStatement pst = connection
							.prepareStatement("SELECT summary();");
					ResultSet rs = pst.executeQuery();
					rs.next();
					String message = rs.getString(1);
					JOptionPane.showMessageDialog(getFrame(), message);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		mnNewMenu_2.add(mntmF);

		JMenu mnNewMenu_3 = new JMenu("Mode");
		menuBar.add(mnNewMenu_3);

		ButtonGroup group = new ButtonGroup();

		JMenuItem mntmUpdate = new JRadioButtonMenuItem("Update");
		mntmUpdate.setSelected(true);
		mntmUpdate.addActionListener(new ModeActionListener(sbd, "update"));
		mnNewMenu_3.add(mntmUpdate);
		group.add(mntmUpdate);

		JMenuItem mntmSelect = new JRadioButtonMenuItem("Select");
		mntmSelect.addActionListener(new ModeActionListener(sbd, "select"));
		mnNewMenu_3.add(mntmSelect);
		group.add(mntmSelect);

		mainTable.getTableHeader().setReorderingAllowed(false);
		artistTable.getTableHeader().setReorderingAllowed(false);
		albumTable.getTableHeader().setReorderingAllowed(false);
		trackTable.getTableHeader().setReorderingAllowed(false);

		mainTable
				.setTableModelListener(new MyTableModelListener(sbd, mainTable));
		artistTable.setTableModelListener(new MyTableModelListener(sbd,
				artistTable));
		albumTable.setTableModelListener(new MyTableModelListener(sbd,
				albumTable));
		trackTable.setTableModelListener(new MyTableModelListener(sbd,
				trackTable));

		mainTable.setListSelectionListener(new MyListSelectionListener(sbd,
				mainTable));
		artistTable.setListSelectionListener(new MyListSelectionListener(sbd,
				artistTable));
		albumTable.setListSelectionListener(new MyListSelectionListener(sbd,
				albumTable));
		trackTable.setListSelectionListener(new MyListSelectionListener(sbd,
				trackTable));

		frame.setVisible(true);
	}

	private void initializeHints() throws SQLException {
		ArrayList<Hints> hints = new ArrayList<Hints>();
		String sql = "";
		PreparedStatement select = null;

		// Main
		hints = new ArrayList<Hints>();
		sql = "SELECT title FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT title FROM Album;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT name FROM Artist;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT toTime(length) FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT sequence FROM Albumjoin;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT uid FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		mainTable.setHints(hints);

		// Artist
		hints = new ArrayList<Hints>();
		sql = "SELECT name FROM Artist;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT country FROM Artist;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT comment FROM Artist;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		artistTable.setHints(hints);

		// Album
		hints = new ArrayList<Hints>();
		sql = "SELECT title FROM Album;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT tracks FROM Album;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT releaseYear FROM Album;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		albumTable.setHints(hints);

		// Track
		hints = new ArrayList<Hints>();
		sql = "SELECT title FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT toTime(length) FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		sql = "SELECT uid FROM Track;";
		select = connection.prepareStatement(sql);
		hints.add(new Hints(select));
		trackTable.setHints(hints);
	}

	private void initializeTables() {
		Vector<String> mainColumnNames = new Vector<String>(Arrays.asList(
				"Title", "Album", "Artist", "Length", "Track number",
				"Unique id"));
		Vector<String> artistColumnNames = new Vector<String>(Arrays.asList(
				"Artist", "Country", "Comment"));
		Vector<String> albumColumnNames = new Vector<String>(Arrays.asList(
				"Album", "Tracks", "Release year"));
		Vector<String> trackColumnNames = new Vector<String>(Arrays.asList(
				"Title", "Length", "Unique id"));
		mainTable.setColumnNames(mainColumnNames);
		artistTable.setColumnNames(artistColumnNames);
		albumTable.setColumnNames(albumColumnNames);
		trackTable.setColumnNames(trackColumnNames);

		ArrayList<String> mainTypes = new ArrayList<String>(
				Arrays.asList("String", "String", "String", "Length",
						"sequence", "uid", "id"));
		ArrayList<String> artistTypes = new ArrayList<String>(Arrays.asList(
				"String", "String", "String", "id"));
		ArrayList<String> albumTypes = new ArrayList<String>(Arrays.asList(
				"String", "int", "int", "id"));
		ArrayList<String> trackTypes = new ArrayList<String>(Arrays.asList(
				"String", "Length", "uid", "id"));
		mainTable.setTypes(mainTypes);
		artistTable.setTypes(artistTypes);
		albumTable.setTypes(albumTypes);
		trackTable.setTypes(trackTypes);

		ArrayList<String> mainConstraints = new ArrayList<String>(
				Arrays.asList("64", "64", "64", "length", "1-1024", "18", "id"));
		ArrayList<String> artistConstraints = new ArrayList<String>(
				Arrays.asList("64", "64", "64", "id"));
		ArrayList<String> albumConstraints = new ArrayList<String>(
				Arrays.asList("64", "1-1024", "0-2011", "id"));
		ArrayList<String> trackConstraints = new ArrayList<String>(
				Arrays.asList("64", "length", "18", "id"));
		mainTable.setConstraints(mainConstraints);
		artistTable.setConstraints(artistConstraints);
		albumTable.setConstraints(albumConstraints);
		trackTable.setConstraints(trackConstraints);

		try {
			PreparedStatement select = null;
			ArrayList<PreparedStatement> selectGroup = null;

			// Selects //
			// 0 - Normal
			selectGroup = new ArrayList<PreparedStatement>();
			select = connection
					.prepareStatement("SELECT idAlbumjoin, title, album, artist, toTime(length), sequence, uid FROM Main;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idArtist, name, country, comment FROM Artist;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idAlbum, title, tracks, releaseYear FROM Album;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idTrack, title, toTime(length), uid FROM Track;");
			selectGroup.add(select);
			selects.add(selectGroup);
			// 1 - Artist
			selectGroup = new ArrayList<PreparedStatement>();
			select = connection
					.prepareStatement("SELECT idAlbumjoin, title, album, artist, toTime(length), sequence, uid FROM Main WHERE idArtist = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idArtist, name, country, comment FROM Artist WHERE idArtist = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idAlbum, title, tracks, releaseYear FROM Album WHERE Artist_idArtist = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idTrack, title, toTime(length), uid FROM Main WHERE idArtist = ?;");
			selectGroup.add(select);
			selects.add(selectGroup);
			// 2 - Album
			selectGroup = new ArrayList<PreparedStatement>();
			select = connection
					.prepareStatement("SELECT idAlbumjoin, title, album, artist, toTime(length), sequence, uid FROM Main WHERE idAlbum = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idArtist, name, country, comment FROM Artist ar JOIN Album al ON ar.idArtist = al.Artist_idArtist WHERE idAlbum = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idAlbum, title, tracks, releaseYear FROM Album WHERE idAlbum = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idTrack, title, toTime(length), uid FROM Main WHERE idAlbum = ?;");
			selectGroup.add(select);
			selects.add(selectGroup);
			// 3 - Track, Main
			selectGroup = new ArrayList<PreparedStatement>();
			select = connection
					.prepareStatement("SELECT idAlbumjoin, title, album, artist, toTime(length), sequence, uid FROM Main WHERE idTrack = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idArtist, artist, country, comment FROM Main WHERE idTrack = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idAlbum, album, tracks, releaseYear FROM Main WHERE idTrack = ?;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT idTrack, title, toTime(length), uid FROM Track WHERE idTrack = ?;");
			selectGroup.add(select);
			selects.add(selectGroup);
			// 4 - Search
			selectGroup = new ArrayList<PreparedStatement>();
			select = connection
					.prepareStatement("SELECT DISTINCT idAlbumjoin, title, album, artist, toTime(length), sequence, uid FROM Search "
							+ "WHERE (title LIKE CONCAT(?, '%') OR title IS NULL) "
							+ "AND (toTime(length) LIKE CONCAT(?, '%') OR toTime(length) IS NULL) "
							+ "AND (uid LIKE CONCAT(?, '%') OR uid IS NULL)"
							+ "AND (album LIKE CONCAT(?, '%') OR album IS NULL) "
							+ "AND (tracks LIKE CONCAT(?, '%') OR tracks IS NULL)"
							+ "AND (releaseYear LIKE CONCAT(?, '%') OR releaseYear IS NULL)"
							+ "AND (artist LIKE CONCAT(?, '%') OR artist IS NULL)"
							+ "AND (country LIKE CONCAT(?, '%') OR country IS NULL)"
							+ "AND (comment LIKE CONCAT(?, '%') OR comment IS NULL)"
							+ "AND (sequence LIKE CONCAT(?, '%') OR sequence IS NULL) "
							+ "AND idAlbumjoin IS NOT NULL "
							+ "AND idTrack IS NOT NULL "
							+ "AND idAlbum IS NOT NULL "
							+ "AND idArtist IS NOT NULL;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT DISTINCT idArtist, artist, country, comment FROM Search "
							+ "WHERE (title LIKE CONCAT(?, '%') OR title IS NULL) "
							+ "AND (toTime(length) LIKE CONCAT(?, '%') OR toTime(length) IS NULL) "
							+ "AND (uid LIKE CONCAT(?, '%') OR uid IS NULL)"
							+ "AND (album LIKE CONCAT(?, '%') OR album IS NULL) "
							+ "AND (tracks LIKE CONCAT(?, '%') OR tracks IS NULL)"
							+ "AND (releaseYear LIKE CONCAT(?, '%') OR releaseYear IS NULL)"
							+ "AND (artist LIKE CONCAT(?, '%') OR artist IS NULL)"
							+ "AND (country LIKE CONCAT(?, '%') OR country IS NULL)"
							+ "AND (comment LIKE CONCAT(?, '%') OR comment IS NULL)"
							+ "AND (sequence LIKE CONCAT(?, '%') OR sequence IS NULL) "
							+ "AND idAlbumjoin IS NOT NULL "
							+ "AND idTrack IS NOT NULL "
							+ "AND idAlbum IS NOT NULL "
							+ "AND idArtist IS NOT NULL;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT DISTINCT idAlbum, album, tracks, releaseYear FROM Search "
							+ "WHERE (title LIKE CONCAT(?, '%') OR title IS NULL) "
							+ "AND (toTime(length) LIKE CONCAT(?, '%') OR toTime(length) IS NULL) "
							+ "AND (uid LIKE CONCAT(?, '%') OR uid IS NULL)"
							+ "AND (album LIKE CONCAT(?, '%') OR album IS NULL) "
							+ "AND (tracks LIKE CONCAT(?, '%') OR tracks IS NULL)"
							+ "AND (releaseYear LIKE CONCAT(?, '%') OR releaseYear IS NULL)"
							+ "AND (artist LIKE CONCAT(?, '%') OR artist IS NULL)"
							+ "AND (country LIKE CONCAT(?, '%') OR country IS NULL)"
							+ "AND (comment LIKE CONCAT(?, '%') OR comment IS NULL)"
							+ "AND (sequence LIKE CONCAT(?, '%') OR sequence IS NULL) "
							+ "AND idAlbumjoin IS NOT NULL "
							+ "AND idTrack IS NOT NULL "
							+ "AND idAlbum IS NOT NULL "
							+ "AND idArtist IS NOT NULL;");
			selectGroup.add(select);
			select = connection
					.prepareStatement("SELECT DISTINCT idTrack, title, toTime(length), uid FROM Search "
							+ "WHERE (title LIKE CONCAT(?, '%') OR title IS NULL) "
							+ "AND (toTime(length) LIKE CONCAT(?, '%') OR toTime(length) IS NULL) "
							+ "AND (uid LIKE CONCAT(?, '%') OR uid IS NULL)"
							+ "AND (album LIKE CONCAT(?, '%') OR album IS NULL) "
							+ "AND (tracks LIKE CONCAT(?, '%') OR tracks IS NULL)"
							+ "AND (releaseYear LIKE CONCAT(?, '%') OR releaseYear IS NULL)"
							+ "AND (artist LIKE CONCAT(?, '%') OR artist IS NULL)"
							+ "AND (country LIKE CONCAT(?, '%') OR country IS NULL)"
							+ "AND (comment LIKE CONCAT(?, '%') OR comment IS NULL)"
							+ "AND (sequence LIKE CONCAT(?, '%') OR sequence IS NULL) "
							+ "AND idAlbumjoin IS NOT NULL "
							+ "AND idTrack IS NOT NULL "
							+ "AND idAlbum IS NOT NULL "
							+ "AND idArtist IS NOT NULL;");
			selectGroup.add(select);
			selects.add(selectGroup);

			// Updates //
			ArrayList<PreparedStatement> updates;
			PreparedStatement update;
			// Main
			updates = new ArrayList<PreparedStatement>();
			update = connection
					.prepareStatement("UPDATE Main SET title = ? WHERE idAlbumjoin = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Main SET album = ? WHERE idAlbumjoin = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Main SET artist = ? WHERE idAlbumjoin = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Main SET length = toLength(?) WHERE idAlbumjoin = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Main SET sequence = ? WHERE idAlbumjoin = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Main SET uid = ? WHERE idAlbumjoin = ?;");
			updates.add(update);
			mainTable.setUpdates(updates);
			// Artist
			updates = new ArrayList<PreparedStatement>();
			update = connection
					.prepareStatement("UPDATE Artist SET name = ? WHERE idArtist = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Artist SET country = ? WHERE idArtist = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Artist SET comment = ? WHERE idArtist = ?;");
			updates.add(update);
			artistTable.setUpdates(updates);
			// Album
			updates = new ArrayList<PreparedStatement>();
			update = connection
					.prepareStatement("UPDATE Album SET title = ? WHERE idAlbum = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Album SET tracks = ? WHERE idAlbum = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Album SET releaseYear = ?	WHERE idAlbum = ?;");
			updates.add(update);
			albumTable.setUpdates(updates);
			// Track
			updates = new ArrayList<PreparedStatement>();
			update = connection
					.prepareStatement("UPDATE Track SET title = ? WHERE idTrack = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Track SET length = toLength(?) WHERE idTrack = ?;");
			updates.add(update);
			update = connection
					.prepareStatement("UPDATE Track SET uid = ? WHERE idTrack = ?;");
			updates.add(update);
			trackTable.setUpdates(updates);

			// Inserts //
			ArrayList<PreparedStatement> inserts;
			ArrayList<Integer> variableCount;
			PreparedStatement insert = null;
			// Artist
			inserts = new ArrayList<PreparedStatement>();
			variableCount = new ArrayList<Integer>();
			insert = connection
					.prepareStatement("insert into Artist (name, country, comment) values (?, ?, ?);");
			inserts.add(insert);
			variableCount.add(3);
			artistTable.setInserts(inserts);
			artistTable.setVariableCount(variableCount);
			// Album
			inserts = new ArrayList<PreparedStatement>();
			variableCount = new ArrayList<Integer>();
			insert = connection
					.prepareStatement("insert into Album (title, tracks, releaseYear, Artist_idArtist) values (?, ?, ?, ?);");
			inserts.add(insert);
			variableCount.add(4);
			albumTable.setInserts(inserts);
			albumTable.setVariableCount(variableCount);
			// Track & Albumjoin
			inserts = new ArrayList<PreparedStatement>();
			variableCount = new ArrayList<Integer>();
			insert = connection
					.prepareStatement("insert into Track (title, length, uid) values (?, toLength(?), ?);");
			inserts.add(insert);
			variableCount.add(3);
			trackTable.setInserts(inserts);
			trackTable.setVariableCount(variableCount);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	private void setSelects(ArrayList<PreparedStatement> selects, int which,
			String value) {
		for (PreparedStatement select : selects)
			try {
				select.setBytes(which, value.getBytes());
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	private void setSelects(int index, boolean useId, int id) {
		if (useId)
			for (PreparedStatement select : selects.get(index))
				try {
					select.setInt(1, id);
				} catch (SQLException e) {
					e.printStackTrace();
				}
		mainTable.setSelect(selects.get(index).get(0));
		artistTable.setSelect(selects.get(index).get(1));
		albumTable.setSelect(selects.get(index).get(2));
		trackTable.setSelect(selects.get(index).get(3));
		sbd.updateAllTables();
	}

	public void setupSelects(ArrayList<String> names, ArrayList<String> values) {
		try {
			ArrayList<SQLTable> tables = new ArrayList<SQLTable>(Arrays.asList(
					mainTable, artistTable, albumTable, trackTable));
			ArrayList<PreparedStatement> searchSelects = selects.get(4);

			int selectCount = searchSelects.size();
			for (int select = 0; select < selectCount; select++) {
				PreparedStatement query = searchSelects.get(select);
				int varNum = 10;
				for (int var = 0; var < varNum; var++) {
					query.setString(var + 1, "");
				}
			}

			for (SQLTable table : tables) { // tabela
				ArrayList<String> fieldNames = new ArrayList<String>(
						table.getColumnNames());
				for (String fieldName : fieldNames) { // nazwa
					int conditionCount = values.size();
					for (int condition = 0; condition < conditionCount; condition++) { // warunek
						if (names.get(condition).equalsIgnoreCase(fieldName)) {
							if (fieldName.equalsIgnoreCase("Title")) {
								setSelects(searchSelects, 1,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Length")) {
								setSelects(searchSelects, 2,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Unique id")) {
								setSelects(searchSelects, 3,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Album")) {
								setSelects(searchSelects, 4,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Tracks")) {
								setSelects(searchSelects, 5,
										values.get(condition));
							} else if (fieldName
									.equalsIgnoreCase("Release year")) {
								setSelects(searchSelects, 6,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Artist")) {
								setSelects(searchSelects, 7,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Country")) {
								setSelects(searchSelects, 8,
										values.get(condition));
							} else if (fieldName.equalsIgnoreCase("Comment")) {
								setSelects(searchSelects, 9,
										values.get(condition));
							} else if (fieldName
									.equalsIgnoreCase("Track number")) {
								setSelects(searchSelects, 10,
										values.get(condition));
							}
						}
					} // warunek
				} // nazwa
			} // tabela
			setSelects(4, false, 0);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void cancelEditing(SQLTable table) {
		if (table.isEditing()) {
			CellEditor cellEditor = table.getCellEditor();
			if (cellEditor != null) {
				cellEditor.cancelCellEditing();
			}
		}
	}

	public void cancelEditingTables(SQLTable table) {
		ArrayList<SQLTable> tables = new ArrayList<SQLTable>(Arrays.asList(
				mainTable, artistTable, albumTable, trackTable));
		for (SQLTable tab : tables)
			if (tab != table)
				cancelEditing(tab);
	}

	public void switchView(SQLTable table, boolean useId, int row) {
		int id = 0;
		if (useId)
			id = table.getIDs().get(0).get(row);
		if (table == mainTable) {
			setSelects(3, useId, id);
		} else if (table == artistTable) {
			setSelects(1, useId, id);
		} else if (table == albumTable) {
			setSelects(2, useId, id);
		} else if (table == trackTable) {
			setSelects(3, useId, id);
		}
	}

	public void updateAllTables() {
		mainTable.executeSelect();
		artistTable.executeSelect();
		albumTable.executeSelect();
		trackTable.executeSelect();
	}

	public void setTablesEditable(boolean editable) {
		mainTable.setEditable(editable);
		artistTable.setEditable(editable);
		albumTable.setEditable(editable);
		trackTable.setEditable(editable);
	}
}