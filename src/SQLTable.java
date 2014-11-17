import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;

public class SQLTable extends JTable {

	private static final long serialVersionUID = 4049920073437857008L;
	private Vector<String> columnNames;
	ArrayList<String> constraints = new ArrayList<String>();
	boolean editable = true;
	ArrayList<Hints> hints = null;
	ArrayList<ArrayList<Integer>> IDs = new ArrayList<ArrayList<Integer>>();
	private ArrayList<PreparedStatement> inserts;
	String lastEditedCellBackupValue;
	private SBD sbd;
	private PreparedStatement select;
	ArrayList<String> types = new ArrayList<String>();
	private ArrayList<PreparedStatement> updates = null;
	private ArrayList<Integer> variableCount;

	public SQLTable(boolean editable) {
		this.editable = editable;
	}

	public SQLTable(int numRows, int numColumns) {
		super(numRows, numColumns);
	}

	public SQLTable(Object[][] rowData, Object[] columnNames) {
		super(rowData, columnNames);
	}

	public SQLTable(SBD sbd) {
		this.sbd = sbd;
	}

	public SQLTable(TableModel dm) {
		super(dm);
	}

	public SQLTable(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
	}

	public SQLTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
		super(dm, cm, sm);
	}

	@SuppressWarnings("rawtypes")
	public SQLTable(Vector rowData, Vector columnNames) {
		super(rowData, columnNames);
	}

	public void close() throws SQLException {
		select.close();
	}

	public boolean executeInsert(ArrayList<String> values,
			ArrayList<String> types) {
		ConstraintValidator constraintValidator = new ConstraintValidator(sbd,
				new ArrayList<String>(getColumnNames()), types,
				getConstraints(), values, columnNames.size());
		if (constraintValidator.isValid()) {
			try {
				int insCount = inserts.size();
				int accumulatedVarCount = 0;
				for (int ins = 0; ins < insCount; ins++) {
					PreparedStatement insert = inserts.get(ins);
					int varCount = variableCount.get(ins);
					for (int var = 0; var < varCount; var++) {
						String type = types.get(var + accumulatedVarCount);
						String value = values.get(var + accumulatedVarCount);
						if (type.equals("int") || type.equals("id")) {
							insert.setInt(var + 1, Integer.parseInt(value));
						} else if (type.equals("String")) {
							insert.setBytes(var + 1, value.getBytes());
						} else if (type.equals("Length")) {
							insert.setString(var + 1, value);
						} else if (type.equals("uid")) {
							insert.setString(var + 1, value);
						} else if (type.equals("sequence")) {
							insert.setInt(var + 1, Integer.parseInt(value));
						}
					}
					accumulatedVarCount += varCount;
					int changes = insert.executeUpdate();
					if (changes > 0) {
						sbd.updateAllTables();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}

	public void executeSelect() {
		sbd.cancelEditingTables(null);
		DefaultTableModel model = (DefaultTableModel) this.getModel();
		model.setRowCount(0);
		try {
			ResultSet rs;
			rs = select.executeQuery();
			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			IDs = new ArrayList<ArrayList<Integer>>();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			for (int column = 0; column < columnCount; column++)
				IDs.add(new ArrayList<Integer>());

			while (rs.next()) {
				Vector<Object> row = new Vector<Object>();
				for (int column = 1; column <= columnCount; column++) {
					if (rsmd.getColumnName(column).startsWith("id")) {
						IDs.get(column - 1).add(rs.getInt(column));
					} else {
						String colType = rsmd.getColumnTypeName(column);
						if (colType.equals("VARBINARY")) {
							byte[] b = rs.getBytes(column);
							if (b != null) {
								row.add(new String(b, "UTF8"));
							}
						} else if (colType.equals("INT UNSIGNED")) {
							row.add(rs.getInt(column));
						} else if (colType.equals("VARCHAR")) {
							row.add(rs.getString(column));
						} else {
							row.add(rs.getObject(column));
						}
					}
				}
				rows.addElement(row);
			}
			rs.close();
			int rowCount = rows.size();
			for (int row = 0; row < rowCount; row++) {
				model.addRow(rows.get(row));
			}
			setUpHints();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public boolean executeUpdate(ArrayList<String> values, int col) {
		ArrayList<String> names = new ArrayList<String>();
		names.add(this.columnNames.get(col));
		names.add("id");
		ArrayList<String> types = new ArrayList<String>();
		types.add(this.types.get(col));
		types.add("id");
		ArrayList<String> constraints = new ArrayList<String>();
		constraints.add(this.constraints.get(col));
		constraints.add("none");

		ConstraintValidator constraintValidator = new ConstraintValidator(sbd,
				names, types, constraints, values, values.size());
		if (constraintValidator.isValid()) {
			try {
				int columnCount = values.size();
				PreparedStatement update = updates.get(col);
				for (int column = 0; column < columnCount; column++) {
					String type = types.get(column);
					String value = (String) values.get(column);
					if (type.equals("int")) {
						update.setInt(column + 1, Integer.parseInt(value));
					} else if (type.equals("String")) {
						update.setBytes(column + 1, value.getBytes());
					} else if (type.equals("id")) {
						update.setInt(column + 1, Integer.parseInt(value));
					} else if (type.equals("Length")) {
						update.setString(column + 1, value);
					} else if (type.equals("uid")) {
						update.setString(column + 1, value);
					} else if (type.equals("sequence")) {
						update.setInt(column + 1, Integer.parseInt(value));
					}
				}
				update.executeUpdate();
				sbd.updateAllTables();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}

	public Vector<String> getColumnNames() {
		return columnNames;
	}

	public ArrayList<String> getConstraints() {
		return constraints;
	}

	public ArrayList<Hints> getHints() {
		return hints;
	}

	public ArrayList<ArrayList<Integer>> getIDs() {
		return IDs;
	}

	public String getLastEditedCellBackupValue() {
		return lastEditedCellBackupValue;
	}

	public ArrayList<String> getTypes() {
		return types;
	}

	public boolean isCellEditable(int rowIndex, int vColIndex) {
		return editable;
	}

	public void setColumnNames(Vector<String> columnNames) {
		DefaultTableModel model = (DefaultTableModel) this.getModel();
		if (model.getColumnCount() == 0) {
			int colCount = columnNames.size();
			for (int col = 0; col < colCount; col++) {
				model.addColumn(columnNames.get(col));
			}
		}
		this.columnNames = columnNames;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public void setConstraints(ArrayList<String> constraints) {
		this.constraints = constraints;
	}

	public void setHints(ArrayList<Hints> hints) {
		this.hints = hints;
		setUpHints();
	}

	public void setInserts(ArrayList<PreparedStatement> inserts) {
		this.inserts = inserts;
	}

	public void setLastEditedCellBackupValue(String lastEditedCellBackupValue) {
		this.lastEditedCellBackupValue = lastEditedCellBackupValue;
	}

	public void setListSelectionListener(
			MyListSelectionListener myListSelectionListener) {
		this.getSelectionModel().addListSelectionListener(
				myListSelectionListener);
		this.getColumnModel().getSelectionModel()
				.addListSelectionListener(myListSelectionListener);
	}

	public void setSelect(PreparedStatement select) {
		this.select = select;
	}

	public void setTableModelListener(MyTableModelListener myTableModelListener) {
		this.getModel().addTableModelListener(myTableModelListener);
	}

	public void setTypes(ArrayList<String> types) {
		this.types = types;
	}

	public void setUpdates(ArrayList<PreparedStatement> updates) {
		this.updates = updates;
	}

	public void setUpHints() {
		if (hints != null) {
			int columnCount = this.getColumnCount();
			for (int column = 0; column < columnCount; column++) {
				Vector<String> autocomplete = hints.get(column).getHints();
				JComboBox<String> comboBox = new JComboBox<String>(autocomplete);
				comboBox.setEditable(true);
				comboBox.setSelectedIndex(-1);
				TableColumn tableColumn = this.getColumnModel().getColumn(
						column);
				tableColumn.setCellEditor(new ComboBoxCellEditor(comboBox));
			}
		}
	}

	public void setVariableCount(ArrayList<Integer> variableCount) {
		this.variableCount = variableCount;
	}
}
