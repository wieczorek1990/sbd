import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class Hints {
	Vector<String> hints;
	PreparedStatement select;

	public Hints(PreparedStatement select) {
		setSelect(select);
	}

	public Hints(Vector<String> hints) {
		this.hints = hints;
	}

	public Vector<String> getHints() {
		hints = new Vector<String>();
		try {
			if (select != null) {
				ResultSet rs = select.executeQuery();
				while (rs.next()) {
					byte[] b = rs.getBytes(1);
					if (b != null)
						hints.add(new String(b, "UTF8"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return hints;
	}

	private void setSelect(PreparedStatement select) {
		try {
			if (select.getMetaData().getColumnCount() == 1) {
				this.select = select;
			} else {
				this.select = null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
