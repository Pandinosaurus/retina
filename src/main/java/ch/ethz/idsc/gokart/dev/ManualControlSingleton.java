// code by jph
package ch.ethz.idsc.gokart.dev;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.ethz.idsc.gokart.core.joy.ManualConfig;
import ch.ethz.idsc.retina.dev.joystick.ManualControlListener;
import ch.ethz.idsc.retina.dev.joystick.ManualControlProvider;
import ch.ethz.idsc.retina.util.StartAndStoppable;
import ch.ethz.idsc.retina.util.meta.Refactor;

@Refactor // TODO JAN implement listener architecture
enum ManualControlSingleton implements StartAndStoppable {
  INSTANCE;
  // ---
  private final ManualControlProvider manualControlProvider = ManualConfig.GLOBAL.createProvider();
  private final List<ManualControlListener> list = new CopyOnWriteArrayList<>();

  @Override
  public void start() {
    manualControlProvider.start();
  }

  @Override
  public void stop() {
    manualControlProvider.stop();
  }

  public void addListener(ManualControlListener manualControlListener) {
    list.add(manualControlListener);
  }

  public void removeListener(ManualControlListener manualControlListener) {
    list.remove(manualControlListener);
  }
}
