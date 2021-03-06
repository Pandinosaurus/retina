// code by jph
package ch.ethz.idsc.retina.util.sys;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;

/** used in tabbed task gui */
/* package */ class TaskComponent {
  private final Map<Class<? extends AbstractModule>, JToggleButton> map = new HashMap<>();
  private final JPanel jpanel = new JPanel();
  private final JPanel top = new JPanel(new BorderLayout());
  final JScrollPane jScrollPane = new JScrollPane(top, //
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, //
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

  public TaskComponent(List<Class<? extends AbstractModule>> modules, Properties properties) {
    jpanel.setLayout(new GridLayout(modules.size(), 1));
    for (Class<? extends AbstractModule> module : modules) {
      final String key = module.getSimpleName();
      JToggleButton jToggleButton = GuiConfig.GLOBAL.createToggleButton(getName(module));
      // new JToggleButton(getName(module));
      // jToggleButton.setFont(GuiConfig.GLOBAL.getFont());
      // jToggleButton.setPreferredSize(new Dimension(120, 36));
      jToggleButton.addActionListener(e -> {
        if (jToggleButton.isSelected())
          try {
            ModuleAuto.INSTANCE.runOne(module);
          } catch (Exception exception) {
            exception.printStackTrace();
          }
        else
          ModuleAuto.INSTANCE.endOne(module);
      });
      if (properties.containsKey(key)) {
        String value = properties.getProperty(key);
        if (!value.isEmpty())
          jToggleButton.setToolTipText(value);
      }
      jpanel.add(jToggleButton);
      map.put(module, jToggleButton);
    }
    top.add("North", jpanel);
    // ---
    JScrollBar jScrollBar = jScrollPane.getVerticalScrollBar();
    jScrollBar.setPreferredSize(new Dimension(28, 28));
  }

  public void terminateAll() {
    for (Entry<Class<? extends AbstractModule>, JToggleButton> entry : map.entrySet()) {
      ModuleAuto.INSTANCE.endOne(entry.getKey());
      entry.getValue().setSelected(false);
    }
  }

  private static String getName(Class<?> module) {
    String name = StaticHelper.putSpaceBefCaps(module.getSimpleName());
    return name.endsWith(" Module") //
        ? name.substring(0, name.length() - 7)
        : name;
  }
}
