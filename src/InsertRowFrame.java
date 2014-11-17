import java.awt.Component;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

public class InsertRowFrame extends JFrame {

	private static final long serialVersionUID = 6409590746128079995L;
	private ArrayList<Component> components;
	private ArrayList<JComboBox<String>> fields;
	private ArrayList<String> names;
	private Container pane;
	private SBD sbd;

	public InsertRowFrame(int x, int y, int width, int height, SBD _sbd) {
		this.setBounds(x, y, width, height);
		this.sbd = _sbd;

		String[] data = { "Artist", "Album", "Track", "Track into Album" };
		final JList<String> list = new JList<String>(data);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroller = new JScrollPane(list);
		ListSelectionListener lsl = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					String selectedValue = list.getSelectedValue();
					names = new ArrayList<>();
					if (selectedValue != null) {
						if (pane.getComponentCount() > 1) {
							for (Component c : components) {
								pane.remove(c);
							}
						}
						ArrayList<Hints> hints = new ArrayList<Hints>();
						if (selectedValue == "Artist") {
							names.addAll(sbd.artistTable.getColumnNames());
							hints = sbd.artistTable.getHints();
						} else if (selectedValue == "Album") {
							names.addAll(sbd.albumTable.getColumnNames());
							hints = sbd.albumTable.getHints();
						} else if (selectedValue == "Track") {
							names.addAll(sbd.trackTable.getColumnNames());
							hints = sbd.trackTable.getHints();
						} else if (selectedValue == "Track into Album") {
							names.add("Track number");
							PreparedStatement select = null;
							try {
								select = sbd.getConnection().prepareStatement(
										"SELECT sequence FROM Albumjoin;");
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
							hints.add(new Hints(select));
						}
						components = new ArrayList<Component>();
						fields = new ArrayList<JComboBox<String>>();
						int rowCount = names.size();
						for (int row = 0; row < rowCount; row++) {
							String name = names.get(row);
							JSplitPane splitPane = new JSplitPane(
									JSplitPane.HORIZONTAL_SPLIT);
							splitPane.setMinimumSize(new Dimension(20, 19));
							splitPane.setMaximumSize(new Dimension(240, 19));
							JLabel label = new JLabel(name);
							Vector<String> autocomplete = hints.get(row)
									.getHints();
							JComboBox<String> field = new JComboBox<String>(
									autocomplete);
							field.setName(name);
							field.setEditable(true);
							field.setSelectedIndex(-1);
							AutoCompleteDecorator.decorate(field);
							splitPane.add(label);
							splitPane.add(field);
							fields.add(field);
							pane.add(splitPane);
							components.add(splitPane);
						}
						JButton btn = new JButton("Insert row");
						ActionListener actionListener = new ActionListener() {
							public void actionPerformed(ActionEvent actionEvent) {
								String selectedValue = list.getSelectedValue();
								if (selectedValue != null) {
									SQLTable table = null;
									ArrayList<String> values = new ArrayList<String>();
									ArrayList<String> types = null;
									for (JComboBox<String> comboBox : fields) {
										String value;
										Object item = comboBox
												.getSelectedItem();
										if (item != null) {
											value = (String) item;
										} else {
											value = "";
										}
										values.add(value);
									}
									int selected = -1;
									if (selectedValue == "Artist") {
										table = sbd.artistTable;
										types = new ArrayList<String>(
												table.getTypes());
										selected = -2;
									} else if (selectedValue == "Album") {
										table = sbd.albumTable;
										types = new ArrayList<String>(
												table.getTypes());
										selected = getSelectedRowRealID(
												sbd.artistTable, 0);
										values.add(Integer.toString(selected));
									} else if (selectedValue == "Track") {
										table = sbd.trackTable;
										types = new ArrayList<String>(
												table.getTypes());
										selected = getSelectedRowRealID(
												sbd.albumTable, 0);
										values.add(Integer.toString(selected));
									} else if (selectedValue == "Track into Album") {
										table = sbd.mainTable;
										ArrayList<String> names = new ArrayList<String>(
												Arrays.asList("Track number",
														"idTrack"));
										types = new ArrayList<String>(
												Arrays.asList("sequenceInsert",
														"id"));
										ArrayList<String> constraints = new ArrayList<String>(
												Arrays.asList("1-1024", "id"));
										int track = getSelectedRowRealID(
												sbd.trackTable, 0);
										int album = getSelectedRowRealID(
												sbd.albumTable, 0);
										values.add(Integer.toString(track));
										values.add(Integer.toString(album));

										ConstraintValidator constraintValidator = new ConstraintValidator(
												sbd, names, types, constraints,
												values, values.size() - 1);
										if (constraintValidator.isValid()) {
											PreparedStatement insert = null;
											try {
												insert = sbd
														.getConnection()
														.prepareStatement(
																"INSERT INTO Albumjoin(sequence, Album_idAlbum, Track_idTrack)  VALUES(?, ?, ?);");
												int sequence = Integer
														.parseInt(values.get(0));
												insert.setInt(1, sequence);
												insert.setInt(2, album);
												insert.setInt(3, track);
												int changes = 0;
												if (album != -1 && track != -1)
													changes = insert
															.executeUpdate();
												if (changes > 0) {
													setVisible(false);
													sbd.updateAllTables();
												}
											} catch (SQLException e) {
												e.printStackTrace();
											}
											return;
										}
									}
									if (selected != -1)
										if (table.executeInsert(values, types))
											setVisible(false);
								}
							}
						};
						btn.addActionListener(actionListener);
						pane.add(btn);
						components.add(btn);
						pane.revalidate();
						validate();
					}
				}
			}
		};
		list.addListSelectionListener(lsl);

		pane = this.getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.add(listScroller);
		setVisible(true);
	}

	public int getSelectedRowRealID(SQLTable table, int column) {
		int rows[] = table.getSelectedRows();
		if (rows.length != 1) {
			JOptionPane.showMessageDialog(sbd.getFrame(), "Please select one "
					+ table.getName() + " record!");
			return -1;
		}
		return table.getIDs().get(column).get(rows[0]);
	}
}
