// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.nio.ByteBuffer;

import ch.ethz.idsc.retina.util.data.BufferInsertable;

public class ControlAndPredictionSteps implements BufferInsertable {
  final ControlAndPredictionStep[] steps;

  public ControlAndPredictionSteps(ControlAndPredictionStep[] controlAndPredictionSteps) {
    steps = controlAndPredictionSteps;
  }

  // TODO JPH can use byteBuffer.remaining() for adaptive size
  public ControlAndPredictionSteps(ByteBuffer byteBuffer) {
    steps = new ControlAndPredictionStep[MPCNative.PREDICTION_SIZE];
    for (int index = 0; index < MPCNative.PREDICTION_SIZE; ++index)
      steps[index] = new ControlAndPredictionStep(byteBuffer);
  }

  @Override // from BufferInsertable
  public void insert(ByteBuffer byteBuffer) {
    for (ControlAndPredictionStep step : steps)
      step.insert(byteBuffer);
  }

  @Override // from BufferInsertable
  public int length() {
    return ControlAndPredictionStep.LENGTH * steps.length;
  }
}
