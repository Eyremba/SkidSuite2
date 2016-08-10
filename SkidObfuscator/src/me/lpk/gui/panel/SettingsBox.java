package me.lpk.gui.panel;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import me.lpk.lang.Lang;

public class SettingsBox extends JPanel {
	private static final long serialVersionUID = 1L;
	private final Map<String,JCheckBox> settings = new HashMap<String,JCheckBox>();
	private final JLabel lblTitle;
	public final String title;

	public SettingsBox(String title) {
		this.title = title;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		lblTitle = new JLabel(title);
		setFont(new Font("Tahoma", Font.BOLD, 13));
		add(lblTitle);
	}
	
	public void addSetting(String setting){
		addSetting(setting, false);
	}
	
	public void addSetting(String setting, boolean enabled){
		JCheckBox chk = new JCheckBox(Lang.translations.get(setting), enabled);
		settings.put(setting,chk);
		add(chk);
	}
	
	public Set<String> getSettings(){
		return settings.keySet();
	}
	
	public boolean isSettingActive(String setting){
		return settings.containsKey(setting) && settings.get(setting).isSelected();
	}
}
