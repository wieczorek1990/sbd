import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author luke Zmienia tryb na ustalony.
 */
public class ModeActionListener implements ActionListener {

	private String mode;
	SBD sbd;

	public ModeActionListener(SBD sbd, String mode) {
		this.sbd = sbd;
		this.mode = mode;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		sbd.cancelEditingTables(null);
		sbd.clearSelection(null);
		if (mode.equals("update"))
			sbd.setTablesEditable(true);
		else if (mode.equals("select"))
			sbd.setTablesEditable(false);
		sbd.setMode(mode);
	}

}
