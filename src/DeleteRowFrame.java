import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class DeleteRowFrame extends JFrame {

	private static final long serialVersionUID = -8207581713494714274L;
	SBD sbd;

	public DeleteRowFrame(int x, int y, int width, int height, SBD _sbd) {
		setBounds(x, y, width, height);
		this.sbd = _sbd;

		String[] data = { "Artist", "Album", "Track", "Track from Album" };
		final JList<String> list = new JList<String>(data);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		JScrollPane listScroller = new JScrollPane(list);
		JButton btn = new JButton("Delete selected rows");

		ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				sbd.cancelEditingTables(null);
				PreparedStatement delete = null;
				int rows[] = null;
				Connection connection = sbd.getConnection();
				String selectedValue = list.getSelectedValue();
				ArrayList<Integer> IDs = new ArrayList<Integer>();
				if (selectedValue != null) {
					try {
						if (selectedValue == "Track") {
							delete = connection
									.prepareStatement("delete from Track where idTrack = ?;");
							rows = sbd.trackTable.getSelectedRows();
							IDs = sbd.trackTable.getIDs().get(0);
						} else if (selectedValue == "Artist") {
							delete = connection
									.prepareStatement("delete from Artist where idArtist = ?;");
							rows = sbd.artistTable.getSelectedRows();
							IDs = sbd.artistTable.getIDs().get(0);
						} else if (selectedValue == "Album") {
							delete = connection
									.prepareStatement("delete from Album where idAlbum = ?;");
							rows = sbd.albumTable.getSelectedRows();
							IDs = sbd.albumTable.getIDs().get(0);
						} else if (selectedValue == "Track from Album") {
							delete = connection
									.prepareStatement("delete from Albumjoin where idAlbumjoin = ?;");
							rows = sbd.mainTable.getSelectedRows();
							PreparedStatement select = connection
									.prepareStatement("SELECT idAlbumjoin FROM Main;");
							ResultSet rs = select.executeQuery();
							while (rs.next())
								IDs.add(rs.getInt(1));
						}
						int changes = 0;
						if (rows.length > 0 && IDs.size() > 0) {
							Object[] options = { "Yes", "No" };
							int answer = JOptionPane.showOptionDialog(
									sbd.getFrame(),
									"Delete record and associated?", "Delete",
									JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE, null,
									options, options[1]);
							if (answer == JOptionPane.YES_OPTION) {
								for (int row : rows) {
									int id = IDs.get(row);
									delete.setInt(1, id);
									changes += delete.executeUpdate();
								}
							}
						}
						delete.close();
						if (changes > 0) {
							sbd.updateAllTables();
							setVisible(false);
						}
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
		};
		btn.addActionListener(actionListener);

		Container pane = this.getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.add(listScroller);
		pane.add(btn);
		setVisible(true);
	}
}
