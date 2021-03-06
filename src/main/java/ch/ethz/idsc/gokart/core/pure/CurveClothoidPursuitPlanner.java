// code by gjoel
package ch.ethz.idsc.gokart.core.pure;

import java.util.Optional;
import java.util.function.Predicate;

import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.lcm.mod.PursuitPlanLcm;
import ch.ethz.idsc.owl.math.pursuit.AssistedCurveIntersectionInterface;
import ch.ethz.idsc.owl.math.pursuit.CurvePoint;
import ch.ethz.idsc.sophus.crv.clothoid.ClothoidTerminalRatios;
import ch.ethz.idsc.sophus.lie.se2.Se2GroupElement;
import ch.ethz.idsc.sophus.math.HeadTailInterface;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Last;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;

public class CurveClothoidPursuitPlanner {
  private final ClothoidPursuitConfig clothoidPursuitConfig;
  // ---
  private int prevIndex = 0;

  public CurveClothoidPursuitPlanner(ClothoidPursuitConfig clothoidPursuitConfig) {
    this.clothoidPursuitConfig = clothoidPursuitConfig;
  }

  public Optional<ClothoidPlan> getPlan(Tensor pose, Tensor speed, Tensor curve, boolean isForward) {
    return getPlan(pose, speed, curve, true, isForward);
  }

  /** @param pose of vehicle {x[m], y[m], angle}
   * @param speed of vehicle {vx[m*s^-1], vy[m*s^-1], gyroZ[s^-1]}
   * @param curve in world coordinates
   * @param isForward driving direction, true when forward or stopped, false when driving backwards
   * @param closed whether curve is closed or not
   * @return geodesic plan */
  public Optional<ClothoidPlan> getPlan(Tensor pose, Tensor speed, Tensor curve, boolean closed, boolean isForward) {
    Optional<ClothoidPlan> optional = replanning(pose, speed, curve, closed, isForward);
    // TODO GJOEL/JPH publishing of plan should happen outside of class
    optional.ifPresent(plan -> PursuitPlanLcm.publish(GokartLcmChannel.PURSUIT_PLAN, pose, Last.of(plan.curve()), isForward));
    return optional;
  }

  private Optional<ClothoidPlan> replanning(Tensor pose, Tensor speed, Tensor curve, boolean closed, boolean isForward) {
    TensorUnaryOperator tensorUnaryOperator = new Se2GroupElement(pose).inverse()::combine;
    Tensor tensor = Tensor.of(curve.stream().map(tensorUnaryOperator));
    if (!isForward)
      ClothoidPursuitHelper.mirrorAndReverse(tensor);
    Predicate<Scalar> isCompliant = clothoidPursuitConfig.ratioLimits()::isInside;
    Scalar lookAhead = clothoidPursuitConfig.lookAhead;
    do {
      AssistedCurveIntersectionInterface assistedCurveIntersection = //
          clothoidPursuitConfig.getAssistedCurveIntersection(lookAhead);
      Optional<CurvePoint> curvePoint = closed //
          ? assistedCurveIntersection.cyclic(tensor, prevIndex) //
          : assistedCurveIntersection.string(tensor, prevIndex);
      if (curvePoint.isPresent()) {
        Tensor xya = curvePoint.get().getTensor();
        HeadTailInterface headTailInterface = ClothoidTerminalRatios.of(xya.map(Scalar::zero), xya);
        if (isCompliant.test(headTailInterface.head()) && //
            isCompliant.test(headTailInterface.tail())) {
          Optional<ClothoidPlan> optional = ClothoidPlan.from(xya, pose, isForward);
          if (optional.isPresent()) {
            prevIndex = curvePoint.get().getIndex();
            return optional;
          }
        }
      }
      lookAhead = lookAhead.add(clothoidPursuitConfig.lookAheadResolution);
    } while (Scalars.lessEquals(lookAhead, clothoidPursuitConfig.fallbackLookAhead));
    return Optional.empty();
  }
}