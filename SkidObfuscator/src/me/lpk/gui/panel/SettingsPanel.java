package me.lpk.gui.panel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import me.lpk.options.Options;

public abstract class SettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final Map<String, SettingsBox> groups = new HashMap<String, SettingsBox>();
	
	public SettingsPanel(){
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setup();
	}
	
	public SettingsBox createGroup(String group, String... vars){
		SettingsBox box = new SettingsBox(group);
		for (String setting : vars){
			box.addSetting(setting, Options.getDefaultState(setting));
		}
		return box;
	}
	
	public void addGroup(SettingsBox group){
		groups.put(group.title, group);
		add(group);
	}
	
	public abstract void setup();
	
	public SettingsBox getGroup(String name){
		return groups.get(name);
	}
	
	public Map<String, SettingsBox> getGroupMap(){
		return groups;
	}
	
	public Set<String> getGroupNames(){
		return groups.keySet();
	}
}
