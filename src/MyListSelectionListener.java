import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author luke Ustawia ostatnią zmienioną wartość
 */
public class MyListSelectionListener implements ListSelectionListener {

	SBD sbd;
	SQLTable table;

	public MyListSelectionListener(SBD sbd, SQLTable table) {
		this.sbd = sbd;
		this.table = table;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()
				&& e.getSource() == table.getSelectionModel()
				&& table.getRowSelectionAllowed()) {
			int rowIndexStart = table.getSelectedRow();
			int rowIndexEnd = table.getSelectionModel().getMaxSelectionIndex();
			int colIndexStart = table.getSelectedColumn();
			int colIndexEnd = table.getColumnModel().getSelectionModel()
					.getMaxSelectionIndex();
			if (rowIndexStart != -1 && rowIndexEnd != -1 && colIndexStart != -1
					&& colIndexEnd != -1) {
				for (int r = rowIndexStart; r <= rowIndexEnd; r++) {
					for (int c = colIndexStart; c <= colIndexEnd; c++) {
						if (table.isCellSelected(r, c)) {
							Object item = table.getValueAt(r, c);
							String value;
							if (item != null)
								value = item.toString();
							else
								value = "";
							table.setLastEditedCellBackupValue(value);
						}
					}
				}
				if (sbd.getMode() == "select") {
					sbd.switchView(table, true, e.getFirstIndex());
				}
			}
		}
	}
}
