// code by jph
package ch.ethz.idsc.gokart.dev.steer;

import java.nio.ByteBuffer;

import ch.ethz.idsc.retina.util.data.DataEvent;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;

/** information received from micro-autobox about steering
 * 
 * <p>the manufacturer of the steering column does <em>not</em>
 * share details about the exact meaning of the values sent by
 * the device, therefore our documentation also lacks clues.
 * 
 * <p>the documentation in this file originates from the investigation
 * of log files recorded over an extended period of operation. */
public class SteerGetEvent extends DataEvent {
  /* package */ static final int LENGTH = 44;
  // ---
  /** motAsp indicates the rotational velocity of the steering wheel.
   * The value ranges typical in the interval [-150, 150]. */
  public final float motAsp_CANInput;
  /** during nominal operation motAsp_Qual is constant 2f
   * a value of 0f was observed briefly during failure instant */
  public final float motAsp_Qual;
  /** tsuTrq_CANInput indicates the torque exerted by the driver.
   * the value ranges in the interval [-8, 8].
   * When the driver does not touch the steering wheel, the value is close to 0.
   * A positive value indicates a drag to the left, ccw.
   * A negative value indicates a drag to the right, clockwise. */
  private final float tsuTrq_CANInput;
  /** during nominal operation motAsp_Qual is constant 2f
   * a value of 0f was observed briefly during failure instant */
  public final float tsuTrq_Qual;
  /** post processing of data has shown that refMotTrq_CANInput
   * allows the interpretation of reference motor torque.
   * refMotTrq_CANInput closely correlates to the demanded torque
   * as commanded by {@link SteerPutEvent}. Due to communication,
   * there is a time delay until the demanded torque is considered
   * as "reference" by the steering actuator. */
  private final float refMotTrq_CANInput;
  /** when the device {@link #isActive()} then the difference between
   * "estMotTrq_CANInput - refMotTrq_CANInput" is typically small.
   * When the torque command is disabled, the value estMotTrq_CANInput
   * takes an arbitrary value that should be ignored. */
  private final float estMotTrq_CANInput;
  /** {@link SteerPutEvent} commands the steer actuator to be passive or active.
   * In passive mode, no torque is applied by the device.
   * 
   * estMotTrq_Qual == 0.0 means the device was not in operation since power on,
   * or has insufficient supply power, or has another issue.
   * 
   * estMotTrq_Qual == 1.0 means the device is passive
   * 
   * estMotTrq_Qual == 2.0 means the device is active, and then the difference
   * "refMotTrq_CANInput - estMotTrq_CANInput" should be small.
   * in autonomous mode, estMotTrq_Qual should always be 2.0
   * TODO JPH check the requirement during operation!
   * 
   * @see #isActive()
   * @see SteerGetStatus */
  public final float estMotTrq_Qual;
  // ---
  /** angular position relative to fixed but initially unknown offset */
  private final float gcpRelRckPos;
  /** gcpRelRckQual is constant 2f during nominal conditions
   * when a failure occurs the value is constant 1f
   * such an instance was first recorded on 20180607
   * 
   * @see SteerGetStatus */
  public final float gcpRelRckQual;
  /** gearRat is constant 22f
   * a value of 0f was observed briefly during failure instant */
  public final float gearRat;
  /** halfRckPos is constant 72f
   * a value of 0f was observed briefly during failure instant */
  public final float halfRckPos;

  /** @param byteBuffer from which constructor reads 44 bytes */
  public SteerGetEvent(ByteBuffer byteBuffer) {
    motAsp_CANInput = byteBuffer.getFloat();
    motAsp_Qual = byteBuffer.getFloat();
    tsuTrq_CANInput = byteBuffer.getFloat();
    tsuTrq_Qual = byteBuffer.getFloat();
    refMotTrq_CANInput = byteBuffer.getFloat();
    estMotTrq_CANInput = byteBuffer.getFloat();
    estMotTrq_Qual = byteBuffer.getFloat();
    // ---
    gcpRelRckPos = byteBuffer.getFloat();
    gcpRelRckQual = byteBuffer.getFloat();
    gearRat = byteBuffer.getFloat();
    halfRckPos = byteBuffer.getFloat();
  }

  @Override // from BufferInsertable
  public void insert(ByteBuffer byteBuffer) {
    byteBuffer.putFloat(motAsp_CANInput);
    byteBuffer.putFloat(motAsp_Qual);
    byteBuffer.putFloat(tsuTrq_CANInput);
    byteBuffer.putFloat(tsuTrq_Qual);
    byteBuffer.putFloat(refMotTrq_CANInput);
    byteBuffer.putFloat(estMotTrq_CANInput);
    byteBuffer.putFloat(estMotTrq_Qual);
    // ---
    byteBuffer.putFloat(gcpRelRckPos);
    byteBuffer.putFloat(gcpRelRckQual);
    byteBuffer.putFloat(gearRat);
    byteBuffer.putFloat(halfRckPos);
  }

  @Override // from BufferInsertable
  public int length() {
    return LENGTH;
  }

  /** gcpRelRckPos == offset + factor * steering_angle
   * 
   * the offset has to be determined in a calibration procedure
   * 
   * @return relative angular position with respect to positive z-axis, i.e.
   * increasing values correspond to ccw rotation */
  public float getGcpRelRckPos() {
    return gcpRelRckPos;
  }

  /** @return true if the device is active and receives and follows torque commands */
  public boolean isActive() {
    return SteerGetStatus.OPERATIONAL.of(estMotTrq_Qual);
  }

  /** function introduced as a consequence of the report
   * 20180616_power_steering_breakdown
   * 
   * @return true if the device is operational */
  public boolean isRelRckQual() {
    return SteerGetStatus.OPERATIONAL.of(gcpRelRckQual);
  }

  /** post processing of data has shown that refMotTrq_CANInput
   * allows the interpretation of reference motor torque.
   * refMotTrq_CANInput closely correlates to the demanded torque
   * as commanded by {@link SteerPutEvent}. Due to communication,
   * there is a time delay until the demanded torque is considered
   * as "reference" by the steering actuator.
   * 
   * @return quantity with unit "SCT" */
  public Scalar refMotTrq() {
    return Quantity.of(refMotTrq_CANInput, SteerPutEvent.UNIT_RTORQUE);
  }

  /** when the device {@link #isActive()} then the difference between
   * "estMotTrq_CANInput - refMotTrq_CANInput" is typically small.
   * When the torque command is disabled, the value estMotTrq_CANInput
   * takes an arbitrary value that should be ignored.
   * 
   * @return quantity with unit "SCT" */
  public Scalar estMotTrq() {
    return Quantity.of(estMotTrq_CANInput, SteerPutEvent.UNIT_RTORQUE);
  }

  /** motAsp indicates the rotational velocity of the steering wheel.
   * The value ranges in the interval [-100, 100]. */
  // TODO investigate unit
  public Scalar motAsp() {
    return RealScalar.of(motAsp_CANInput);
  }

  /** tsuTrq indicates the torque exerted by the driver.
   * When the driver does not touch the steering wheel, the value is close to 0[SCT].
   * A positive value indicates a drag to the left, ccw.
   * A negative value indicates a drag to the right, clockwise.
   * 
   * @return value in the interval [-1[SCT], 1[SCT]] */
  public Scalar tsuTrq() {
    return Quantity.of(tsuTrq_CANInput * 0.125, SteerPutEvent.UNIT_RTORQUE);
  }

  /** @return vector of length 11 */
  @Override
  public Tensor asVector() {
    return Tensors.vector( //
        motAsp_CANInput, // .. 0
        motAsp_Qual, // ...... 1
        tsuTrq_CANInput, // .. 2
        tsuTrq_Qual, // ...... 3
        refMotTrq_CANInput, // 4
        estMotTrq_CANInput, // 5
        estMotTrq_Qual, // ... 6
        gcpRelRckPos, // ..... 7
        gcpRelRckQual, // .... 8
        gearRat, // .......... 9
        halfRckPos // ....... 10
    );
  }
}
