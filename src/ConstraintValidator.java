import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

public class ConstraintValidator {

	private SBD sbd;
	private boolean valid = false;

	public ConstraintValidator(SBD sbd, ArrayList<String> names,
			ArrayList<String> types, ArrayList<String> constraints,
			ArrayList<String> values, int toCheck) {
		this.sbd = sbd;
		valid = constraintsOK(names, types, constraints, values, toCheck);
	}

	private boolean constraintsOK(ArrayList<String> names,
			ArrayList<String> types, ArrayList<String> constraints,
			ArrayList<String> values, int toCheck) {
		for (int i = 0; i < toCheck; i++) {
			String type = types.get(i);
			String constraint = constraints.get(i);
			String value = values.get(i);
			String name = names.get(i);
			if (constraint.equals("none")) {
				continue;
			} else {
				if (type.equals("id")) {
					if (constraint.equals("id"))
						if (Integer.parseInt(value) < 0)
							return false;
				} else if (type.equals("String")) {
					if (((String) value).length() > Integer
							.parseInt(constraint)) {
						JOptionPane.showMessageDialog(sbd.getFrame(), name
								+ " should be no longer than " + constraint
								+ ".");
						return false;
					}
				} else if (type.equals("int")) {
					String[] cons = constraint.split("-");
					int min = Integer.parseInt(cons[0]);
					int max = Integer.parseInt(cons[1]);
					int val;
					try {
						val = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						JOptionPane
								.showMessageDialog(sbd.getFrame(), name
										+ " = \"" + value
										+ "\" is not a valid number.");
						return false;
					}
					if (val > max || val < min) {
						JOptionPane.showMessageDialog(sbd.getFrame(), name
								+ " = \"" + value + "\" should be between "
								+ min + " and " + max + ".");
						return false;
					}
				} else if (type.equals("Length")) {
					Pattern pattern = Pattern
							.compile("^([0-5]?[0-9]:)?([0-5]?[0-9]:)?[0-5]?[0-9]$");
					Matcher matcher = pattern.matcher(value);
					if (!matcher.find()) {
						JOptionPane
								.showMessageDialog(
										sbd.getFrame(),
										name
												+ " should be in format [[xy:]xy:]xy.\nx = 0..5, y = 0..9");
						return false;
					}
				} else if (type.equals("uid")) {
					if (((String) value).length() > Integer
							.parseInt(constraint)) {
						JOptionPane.showMessageDialog(sbd.getFrame(), name
								+ " should be no longer than " + constraint
								+ ".");
						return false;
					}
					try {
						PreparedStatement unique = sbd
								.getConnection()
								.prepareStatement(
										"SELECT idTrack FROM Track WHERE uid=?;");
						unique.setString(1, value);
						ResultSet rs = unique.executeQuery();
						if (rs.next()) {
							JOptionPane.showMessageDialog(sbd.getFrame(), name
									+ " should be unique. Value " + value
									+ " is not unique.");
							return false;
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else if (type.startsWith("sequence")) {
					String[] cons = constraint.split("-");
					int min = Integer.parseInt(cons[0]);
					int max = Integer.parseInt(cons[1]);
					int sequence;
					try {
						sequence = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						JOptionPane
								.showMessageDialog(sbd.getFrame(), name
										+ " = \"" + value
										+ "\" is not a valid number.");
						return false;
					}
					if (sequence > max || sequence < min) {
						JOptionPane.showMessageDialog(sbd.getFrame(), name
								+ " = \"" + value + "\" should be between "
								+ min + " and " + max + ".");
						return false;
					}

					try {
						int albumID = -1;
						int trackID = -1;
						ResultSet rs;
						if (type.equals("sequence")) {
							PreparedStatement album = sbd
									.getConnection()
									.prepareStatement(
											"SELECT idAlbum, idTrack FROM Main WHERE idAlbumjoin = ?;");
							int albumjoinID = Integer.parseInt(values
									.get(values.size() - 1));
							album.setInt(1, albumjoinID);
							rs = album.executeQuery();
							rs.next();
							albumID = rs.getInt(1);
							trackID = rs.getInt(2);
						} else if (type.equals("sequenceInsert")) {
							trackID = Integer.parseInt(value);
							albumID = Integer
									.parseInt(values.get(values.size() - 1));
						}

						PreparedStatement unique = sbd
								.getConnection()
								.prepareStatement(
										"SELECT sequence FROM Albumjoin WHERE sequence = ? AND Album_idAlbum = ? AND Track_idTrack != ?;");
						unique.setInt(1, sequence);
						unique.setInt(2, albumID);
						unique.setInt(3, trackID);
						rs = unique.executeQuery();
						if (rs.next()) {
							JOptionPane.showMessageDialog(sbd.getFrame(), name
									+ " should be unique. Value " + value
									+ " is not unique.");
							return false;
						}

						PreparedStatement tracksCount = sbd
								.getConnection()
								.prepareStatement(
										"SELECT tracks FROM Album WHERE idAlbum = ?");
						tracksCount.setInt(1, albumID);
						rs = tracksCount.executeQuery();
						rs.next();
						int maxTrack = rs.getInt(1);
						if (sequence > maxTrack) {
							JOptionPane
									.showMessageDialog(
											sbd.getFrame(),
											name
													+ " "
													+ value
													+ " should be less than tracks count for given album which is "
													+ maxTrack + ".");
							return false;
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return true;
	}

	public boolean isValid() {
		return valid;
	}

}
