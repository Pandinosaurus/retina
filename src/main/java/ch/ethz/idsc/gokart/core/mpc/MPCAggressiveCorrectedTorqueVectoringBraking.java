// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.util.Objects;

import ch.ethz.idsc.gokart.calib.SensorsConfig;
import ch.ethz.idsc.gokart.calib.brake.SelfCalibratingBrakeFunction;
import ch.ethz.idsc.gokart.calib.steer.RimoTwdOdometry;
import ch.ethz.idsc.gokart.core.slam.LidarLocalizationModule;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetListener;
import ch.ethz.idsc.gokart.dev.rimo.RimoSocket;
import ch.ethz.idsc.gokart.gui.top.BrakeCalibrationRender;
import ch.ethz.idsc.gokart.lcm.imu.Vmu931ImuLcmClient;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrame;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrameListener;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.sys.ModuleAuto;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Ramp;

/* package */ class MPCAggressiveCorrectedTorqueVectoringBraking extends MPCBraking implements Vmu931ImuFrameListener, RimoGetListener {
  private final MPCOptimizationConfig mpcOptimizationConfig = MPCOptimizationConfig.GLOBAL;
  private final SelfCalibratingBrakeFunction selfCalibratingBrakeFunction = new SelfCalibratingBrakeFunction();
  private final Vmu931ImuLcmClient vmu931imuLcmClient = new Vmu931ImuLcmClient();
  private final LidarLocalizationModule lidarLocalizationModule = //
      ModuleAuto.INSTANCE.getInstance(LidarLocalizationModule.class);
  // ---
  private Scalar currentAcceleration = Quantity.of(0, SI.ACCELERATION);
  private Scalar wheelSpeed = Quantity.of(0, SI.VELOCITY);

  @Override // from MPCBraking
  Scalar getBraking(Scalar time) {
    Scalar controlTime = time.add(mpcOptimizationConfig.brakingAntiLag);
    ControlAndPredictionStep cnsStep = getStep(controlTime);
    if (Objects.isNull(cnsStep))
      return RealScalar.ZERO;
    Scalar braking = Ramp.FUNCTION.apply(cnsStep.gokartControl().getaB().negate());
    // self calibration
    Scalar gokartSpeed = lidarLocalizationModule.getVelocity().Get(0);
    Scalar realBraking = currentAcceleration.negate();
    selfCalibratingBrakeFunction.correctBraking(braking, realBraking, gokartSpeed, wheelSpeed);
    BrakeCalibrationRender.calibrationValue = selfCalibratingBrakeFunction.getBrakeFadeFactor(); // TODO JPH
    return selfCalibratingBrakeFunction.getRelativeBrakeActuation(braking);
  }

  @Override
  public void start() {
    vmu931imuLcmClient.addListener(this);
    vmu931imuLcmClient.startSubscriptions();
    RimoSocket.INSTANCE.addGetListener(this);
  }

  @Override
  public void stop() {
    vmu931imuLcmClient.stopSubscriptions();
    RimoSocket.INSTANCE.removeGetListener(this);
  }

  @Override // from Vmu931ImuFrameListener
  public void vmu931ImuFrame(Vmu931ImuFrame vmu931ImuFrame) {
    currentAcceleration = SensorsConfig.GLOBAL.getPlanarVmu931Imu().accXY(vmu931ImuFrame).Get(0);
  }

  @Override // from RimoGetListener
  public void getEvent(RimoGetEvent rimoGetEvent) {
    // wheel speed determines braking
    wheelSpeed = RimoTwdOdometry.tangentSpeed(rimoGetEvent);
  }
}
