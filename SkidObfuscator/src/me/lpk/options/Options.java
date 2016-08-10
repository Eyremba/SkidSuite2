package me.lpk.options;

import java.util.HashMap;
import java.util.Map;

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
}
