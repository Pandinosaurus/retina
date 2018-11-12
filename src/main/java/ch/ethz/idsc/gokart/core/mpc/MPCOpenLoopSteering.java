// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.tensor.Scalar;

public class MPCOpenLoopSteering extends MPCSteering {
  MPCStateEstimationProvider mpcStateProvider;
  MPCOptimizationConfig config = MPCOptimizationConfig.GLOBAL;

  @Override
  public Scalar getSteering(Scalar time) {
    Scalar controlTime = time.add(config.steerAntiLag);
    ControlAndPredictionStep cnpStep = getStep(controlTime);
    Scalar timeSinceLastStep = getTimeSinceLastStep(controlTime);
    Scalar rampUp = timeSinceLastStep.multiply(cnpStep.control.getudotS());
    //System.out.println("Time: "+ timeSinceLastStep +" Steering value: "+cnpStep.state.getS().add(rampUp));
    //System.out.println("Time "+ time);
    return cnpStep.state.getS().add(rampUp);
  }

  @Override
  public void getControlAndPredictionSteps(ControlAndPredictionSteps controlAndPredictionSteps) {
    cns = controlAndPredictionSteps;
  }

  @Override
  public void setStateProvider(MPCStateEstimationProvider mpcstateProvider) {
    this.mpcStateProvider = mpcstateProvider;
  }
}
