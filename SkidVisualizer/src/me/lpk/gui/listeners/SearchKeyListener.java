package me.lpk.gui.listeners;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.gui.VisualizerWindow;
import me.lpk.gui.component.SearchResultEntry;
import me.lpk.util.SearchUtil;

public class SearchKeyListener implements KeyListener {

	@Override
	public void keyPressed(KeyEvent e) {
		handle(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	private void handle(KeyEvent e) {
		if (VisualizerWindow.instance.getSearchText().length() > 3 || e.getKeyCode() == KeyEvent.VK_ENTER) {
			List<SearchResultEntry> results = null;
			switch (VisualizerWindow.instance.getSearchType()) {
			case "Class":
				ClassNode node = VisualizerWindow.instance.getNodes().get(VisualizerWindow.instance.getSearchText());
				if (node == null){
					return;
				}
				results = SearchUtil.findReferences(node);
				break;
			case "Method":
				results = SearchUtil.findMethods(VisualizerWindow.instance.getSearchText());
				break;
			case "Field":
				results = SearchUtil.findFields(VisualizerWindow.instance.getSearchText());
				break;
			case "LDC":
				results = SearchUtil.findStringsContaining(VisualizerWindow.instance.getSearchText());
				break;
			}
			if (results != null) {
				VisualizerWindow.instance.getResultPanel().clearResults();
				for (SearchResultEntry result : results) {
					VisualizerWindow.instance.getResultPanel().addResult(result);
				}
			}
		}
	}
}