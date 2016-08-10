package me.lpk.lang;

import java.util.HashMap;
import java.util.Map;

public class Lang {
	public static final Map<String, String> translations = new HashMap<String, String>();
	public static final String 
		OPTION_MAIN_OPTIMIZE =                 "main.enabled.optimization",
		OPTION_MAIN_OBFUSCATE =                "main.enabled.obfuscation",
		OPTION_MAIN_RECOMPILE_MAXS =           "main.compilemaxs",
		OPTION_OPTIM_CLASS_REMOVE_SRC =        "optimize.class.remove.src",
		OPTION_OPTIM_CLASS_REMOVE_ANNO =       "optimize.class.remove.annotations",
		OPTION_OPTIM_CLASS_REMOVE_ATRIB =      "optimize.class.remove.attributes",
		OPTION_OPTIM_CLASS_REMOVE_METHODS =    "optimize.class.remove.methods",
		OPTION_OPTIM_METHOD_REMOVE_PARAMNAME = "optimize.method.remove.src",
		OPTION_OPTIM_METHOD_REMOVE_ANNO =      "optimize.method.remove.annotations",
		OPTION_OPTIM_METHOD_REMOVE_LOCALDATA = "optimize.method.remove.attributes",
		OPTION_OPTIM_METHOD_REMOVE_LINES =     "optimize.method.remove.lines",
		OPTION_OPTIM_METHOD_REMOVE_ATTRIB =    "optimize.method.remove.attributes",
		OPTION_OPTIM_METHOD_REMOVE_FRAMES =    "optimize.method.remove.frames",
		OPTION_OBFU_MAIN_STRINGS =             "obfuscate.enabled.strings",
		OPTION_OBFU_MAIN_ANTIDECOMPILE =       "obfuscate.enabled.antidecompile",
		OPTION_OBFU_MAIN_FLOW =                "obfuscate.enabled.flow",
		OPTION_OBFU_STRINGS_INTOARRAY =        "obfuscate.strings.intoarray",
		OPTION_OBFU_ANTI_SYNTHETIC =           "obfuscate.anti.synthetic",
		OPTION_OBFU_ANTI_DECOMPILE_VULNS =     "obfuscate.anti.decompilevulns",
		OPTION_OBFU_FLOW_TRYCATCH =            "obfuscate.flow.synthetic",
		OPTION_OBFU_FLOW_GOTOFLOOD =           "obfuscate.flow.gotoflood",
		//
		GUI_OBFUSCATION_GROUP_STRING = "gui.groups.obfuscate.strings",
		GUI_OBFUSCATION_GROUP_FLOW =   "gui.groups.obfuscate.flow",
		GUI_OBFUSCATION_GROUP_ANTI =   "gui.groups.obfuscate.anti",
		GUI_OPTIM_GROUP_CLASS =        "gui.groups.optimize.class",
		GUI_OPTIM_GROUP_METHOD =       "gui.groups.optimize.method"
		;
	
	static {
		/* TODO: Loading from language files */
		translations.put(OPTION_MAIN_OPTIMIZE,       "Use optimization");
		translations.put(OPTION_MAIN_OBFUSCATE,      "Use obfuscation");
		translations.put(OPTION_MAIN_RECOMPILE_MAXS, "Compile with -noverify");
		//
		translations.put(OPTION_OPTIM_CLASS_REMOVE_SRC,   "Remove sourcename");
		translations.put(OPTION_OPTIM_CLASS_REMOVE_ANNO,  "Remove annotations");
		translations.put(OPTION_OPTIM_CLASS_REMOVE_ATRIB, "Remove attributes");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_PARAMNAME, "Remove parameter names");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_ANNO,      "Remove annotations");
		translations.put(OPTION_OPTIM_CLASS_REMOVE_METHODS,    "Remove unused methods");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_LOCALDATA, "Remove variable data");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_LINES,     "Remove line numbers");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_ATTRIB,    "Remove attributes");
		translations.put(OPTION_OPTIM_METHOD_REMOVE_FRAMES,    "Remove frames");
		//
		translations.put(OPTION_OBFU_MAIN_STRINGS,       "Use string obfuscation");
		translations.put(OPTION_OBFU_MAIN_ANTIDECOMPILE, "Use decompile vulns");
		translations.put(OPTION_OBFU_MAIN_FLOW,          "Use flow control");
		translations.put(OPTION_OBFU_STRINGS_INTOARRAY,  "Break into arrays");
		translations.put(OPTION_OBFU_FLOW_TRYCATCH,      "Add try catches");
		translations.put(OPTION_OBFU_FLOW_GOTOFLOOD,     "Add redundant gotos");
		translations.put(OPTION_OBFU_ANTI_SYNTHETIC,     "Mark as synthetic");
		translations.put(OPTION_OBFU_ANTI_DECOMPILE_VULNS,"Anti-decompiler tricks");
		//
		translations.put(GUI_OBFUSCATION_GROUP_STRING,   "String Settings");
		translations.put(GUI_OBFUSCATION_GROUP_FLOW,     "Flow Settings");
		translations.put(GUI_OBFUSCATION_GROUP_ANTI,     "Anti-Decompiler Settings");
		//
		translations.put(GUI_OPTIM_GROUP_CLASS,     "Class Settings");
		translations.put(GUI_OPTIM_GROUP_METHOD,     "Method Settings");
	}
}
