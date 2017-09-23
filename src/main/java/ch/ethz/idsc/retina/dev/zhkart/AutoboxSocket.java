// code by jph
package ch.ethz.idsc.retina.dev.zhkart;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.idsc.retina.dev.linmot.LinmotGetListener;
import ch.ethz.idsc.retina.dev.rimo.RimoGetListener;
import ch.ethz.idsc.retina.util.StartAndStoppable;
import ch.ethz.idsc.retina.util.io.ByteArrayConsumer;
import ch.ethz.idsc.retina.util.io.DatagramSocketManager;

/** template argument T is {@link RimoGetListener}, {@link LinmotGetListener}, ...
 * 
 * Example interface on datahaki's computer
 * 
 * enx9cebe8143edb Link encap:Ethernet HWaddr 9c:eb:e8:14:3e:db inet
 * addr:192.168.1.1 Bcast:192.168.1.255 Mask:255.255.255.0 inet6 addr:
 * fe80::9eeb:e8ff:fe14:3edb/64 Scope:Link UP BROADCAST RUNNING MULTICAST
 * MTU:1500 Metric:1 RX packets:466380 errors:0 dropped:0 overruns:0 frame:0 TX
 * packets:233412 errors:0 dropped:0 overruns:0 carrier:0 collisions:0
 * txqueuelen:1000 RX bytes:643249464 (643.2 MB) TX bytes:17275914 (17.2 MB) */
public abstract class AutoboxSocket<T, R, P extends PutProvider<R>> implements //
    StartAndStoppable, ByteArrayConsumer {
  private final DatagramSocketManager datagramSocketManager;
  protected final List<T> listeners = new LinkedList<>();
  protected final List<P> providers = new ArrayList<>();
  protected Timer timer;

  public AutoboxSocket(DatagramSocketManager datagramSocketManager) {
    this.datagramSocketManager = datagramSocketManager;
    datagramSocketManager.addListener(this);
  }

  @Override
  public final void start() {
    datagramSocketManager.start();
    timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          for (PutProvider<R> putProvider : providers) {
            Optional<R> optional = putProvider.pollPutEvent();
            if (optional.isPresent()) {
              datagramSocketManager.send(getDatagramPacket(optional.get()));
              break;
            }
          }
          throw new RuntimeException("no command provided");
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      }
    }, 100, getPeriod());
  }

  protected abstract long getPeriod();

  protected abstract DatagramPacket getDatagramPacket(R optional);

  @Override
  public final void stop() {
    timer.cancel();
    datagramSocketManager.stop();
  }

  public final void addProvider(P putProvider) {
    providers.add(putProvider);
    Collections.sort(providers, PutProviderComparator.INSTANCE);
  }

  public final void addListener(T getListener) {
    listeners.add(getListener);
  }

  public final void removeListener(T getListener) {
    listeners.remove(getListener);
  }

  public final boolean hasListeners() {
    return !listeners.isEmpty();
  }
}
