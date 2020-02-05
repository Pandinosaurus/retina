// code by gjoel
package ch.ethz.idsc.gokart.dev.led;

import ch.ethz.idsc.gokart.lcm.led.LEDLcmClient;
import ch.ethz.idsc.gokart.lcm.led.LEDListener;
import ch.ethz.idsc.retina.util.sys.AbstractModule;

public class LEDServerModule extends AbstractModule implements LEDListener {
  private final LEDLcmClient ledLcmClient = new LEDLcmClient();

  @Override // from AbstractModule
  protected void first() {
    LEDSocket.INSTANCE.start();
    ledLcmClient.addListener(this);
    ledLcmClient.startSubscriptions();
  }

  @Override // from AbstractModule
  protected void last() {
    ledLcmClient.stopSubscriptions();
    LEDSocket.INSTANCE.stop();
  }

  @Override // from LEDListener
  public void statusReceived(LEDStatus ledStatus) {
    LEDSocket.INSTANCE.write(ledStatus);
  }
}
