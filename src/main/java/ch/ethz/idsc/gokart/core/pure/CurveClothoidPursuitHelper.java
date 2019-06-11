// code by gjoel
package ch.ethz.idsc.gokart.core.pure;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import ch.ethz.idsc.owl.bot.se2.glc.DynamicRatioLimit;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Reverse;
import ch.ethz.idsc.tensor.alg.VectorQ;

// TODO JPH rename
/* package */ enum CurveClothoidPursuitHelper {
  ;
  /** mirror the points along the y axis and invert their orientation
   * @param se2points curve given by points {x,y,a} */
  public static void mirrorAndReverse(Tensor se2points) {
    if (VectorQ.of(se2points)) {
      se2points.set(Scalar::negate, 0);
      se2points.set(Scalar::negate, 2);
    } else {
      se2points.set(Scalar::negate, Tensor.ALL, 0);
      se2points.set(Scalar::negate, Tensor.ALL, 2);
      Tensor reverse = Reverse.of(se2points);
      IntConsumer swap = i -> se2points.set(reverse.get(i), i);
      IntStream.range(0, se2points.length()).forEach(swap);
    }
  }

  /** @param ratioLimits depending on pose and speed
   * @param pose of vehicle {x[m], y[m], angle}
   * @param speed of vehicle [m*s^-1]
   * @return predicate to determine whether ratio is compliant with all posed turning ratio limits */
  public static Predicate<Scalar> isCompliant(List<DynamicRatioLimit> ratioLimits, Tensor pose, Scalar speed) {
    return ratio -> ratioLimits.stream() //
        .map(dynamicRatioLimit -> dynamicRatioLimit.at(pose, speed)) //
        .allMatch(dynamicRatioLimit -> dynamicRatioLimit.isInside(ratio));
  }
}