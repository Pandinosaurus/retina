// code by jph
package ch.ethz.idsc.retina.dev.lidar.urg04lx;

public class Urg04lxEvent {
  public final long timestamp;
  public final double[] range;

  public Urg04lxEvent(long timestamp, double[] range) {
    this.timestamp = timestamp;
    this.range = range;
  }
}