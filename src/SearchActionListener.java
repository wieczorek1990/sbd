import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextField;

public class SearchActionListener implements ActionListener {

	private SBD sbd;
	private JTextField search;

	public SearchActionListener(SBD sbd, JTextField search) {
		this.sbd = sbd;
		this.search = search;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 * Specjalne literki z wyrażeń regularnych javy: "<([{\^-=$!|]})?*+.>"
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();
		String text = (String) search.getText();
		String special = "~`\\!@#\\$%\\^&\\*\\(\\)\\-_\\+\\=\\{\\[\\}\\];:\",\\.\\<\\>\\?/\\\\\\|";
		String polish = "ęĘóÓąĄśŚłŁżŻźŹćĆńŃ";
		Pattern pattern = Pattern.compile("[\\w\\s]+\\='[\\w\\s\\d" + special
				+ polish + "]*'");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String group = matcher.group().trim();
			// System.out.println("group " + group);
			String parts[] = group.split("=", 2);
			names.add(parts[0]);
			values.add(cutInside(parts[1]));
		}
		if (values.size() > 0)
			sbd.setupSelects(names, values);
		else
			sbd.home();
		// for (String str : names)
		// System.out.println(str);
		// for (String str : values)
		// System.out.println(str);
	}

	private String cutInside(String str) {
		int len = str.length();
		return str.substring(1, len - 1);
	}
}
