// code by jph
package ch.ethz.idsc.retina.dev.rimo;

import java.nio.ByteBuffer;

public class RimoPutEvent {
  public short command;
  /** speed in rad/min */
  public short speed;

  public void insert(ByteBuffer byteBuffer) {
    byteBuffer.putShort(command);
    byteBuffer.putShort(speed);
  }
}
