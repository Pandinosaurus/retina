package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.tensor.Scalar;

public interface MPCSteering extends MPCStateReceiver {
  void Update(ControlAndPredictionSteps controlAndPredictionSteps);

  Scalar getSteering(Scalar time);
}
