package me.lpk.gui.panel;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import me.lpk.lang.Lang;

public class SettingsBox extends JPanel {
	private static final long serialVersionUID = 1L;
	private final Map<String,JCheckBox> settings = new HashMap<String,JCheckBox>();
	public final String title;
	private final JPanel internal = new JPanel();

	public SettingsBox(String title) {
		this.title = title;
		setFont(new Font("Tahoma", Font.BOLD, 13));
		setBorder(BorderFactory.createTitledBorder(title));
		setAlignmentX(LEFT_ALIGNMENT);
		setAlignmentY(LEFT_ALIGNMENT);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		internal.setLayout(new BoxLayout(internal, BoxLayout.Y_AXIS));
		add(internal);
		
	}
	
	public void addSetting(String setting){
		addSetting(setting, false);
	}
	
	public void addSetting(String setting, boolean enabled){
		JCheckBox chk = new JCheckBox(Lang.translations.get(setting), enabled);
		settings.put(setting,chk);
		internal.add(chk);
	}
	
	public Set<String> getSettings(){
		return settings.keySet();
	}
	
	public boolean isSettingActive(String setting){
		return settings.containsKey(setting) && settings.get(setting).isSelected();
	}
}
