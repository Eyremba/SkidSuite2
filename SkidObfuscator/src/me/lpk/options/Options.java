package me.lpk.options;

import java.util.HashMap;
import java.util.Map;

import me.lpk.gui.NewGui;
import me.lpk.gui.panel.SettingsBox;
import me.lpk.gui.panel.SettingsPanel;
import me.lpk.lang.Lang;

public class Options {
	public static Map<String, Boolean> defaultEnabledStates = new HashMap<String, Boolean>();
	
	static {
		defaultEnabledStates.put(Lang.OPTION_MAIN_OPTIMIZE, true);
		defaultEnabledStates.put(Lang.OPTION_OPTIM_CLASS_REMOVE_SRC, true);
		defaultEnabledStates.put(Lang.OPTION_OPTIM_METHOD_REMOVE_LINES, true);
		defaultEnabledStates.put(Lang.OPTION_OPTIM_METHOD_REMOVE_LOCALDATA, true);
		defaultEnabledStates.put(Lang.OPTION_OPTIM_METHOD_REMOVE_PARAMNAME, true);
	}
	
	public static boolean getDefaultState(String setting){
		return defaultEnabledStates.containsKey(setting) && defaultEnabledStates.get(setting).booleanValue();
	}
	
	public static Map<String, Boolean> fromGui(NewGui gui) {
		Map<String, Boolean> values = new HashMap<String, Boolean>();
		for (SettingsPanel panel : gui.getSettingPanels()) {
			for (String group : panel.getGroupNames()) {
				SettingsBox settingBox = panel.getGroup(group);
				for (String setting : settingBox.getSettings()) {
					values.put(setting, settingBox.isSettingActive(setting));
				}
			}
		}
		return values;
	}
}
