import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * @author luke Wykrywa zmiany w tabeli i aktualizuje bazę danych.
 */
public class MyTableModelListener implements TableModelListener {

	SBD sbd;
	SQLTable table;

	public MyTableModelListener(SBD sbd, SQLTable table) {
		this.sbd = sbd;
		this.table = table;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getType() == TableModelEvent.UPDATE) {
			if (table.getColumnNames() != null) {
				int row = e.getFirstRow();
				int col = e.getColumn();
				Object item = table.getValueAt(row, col);
				String value;
				if (item != null)
					value = item.toString();
				else
					value = "";
				String backup = table.getLastEditedCellBackupValue();
				if (value != backup) {
					ArrayList<String> values = new ArrayList<String>();
					values.add(value);
					values.add(Integer.toString(table.getIDs().get(0).get(row)));
					if (!table.executeUpdate(values, col)) {
						table.setValueAt(backup, row, col);
					}
				}
			}
		}
	}
}
