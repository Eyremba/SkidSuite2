package me.lpk.gui.panel;

import me.lpk.lang.Lang;

public class ObfuscationPanel extends SettingsPanel {
	private static final long serialVersionUID = 12222L;

	@Override
	public void setup() {
		addGroup(createGroup(
				Lang.translations.get(Lang.GUI_OBFUSCATION_GROUP_STRING), 
					Lang.OPTION_OBFU_STRINGS_INTOARRAY));
		addGroup(createGroup(
				Lang.translations.get(Lang.GUI_OBFUSCATION_GROUP_FLOW), 
					Lang.OPTION_OBFU_FLOW_TRYCATCH,
					Lang.OPTION_OBFU_FLOW_GOTOFLOOD,
					Lang.OPTION_OBFU_FLOW_MATH));
		addGroup(createGroup(
				Lang.translations.get(Lang.GUI_OBFUSCATION_GROUP_ANTI), 
					Lang.OPTION_OBFU_ANTI_DECOMPILE_VULNS,
					Lang.OPTION_OBFU_ANTI_SYNTHETIC));
		}

}
