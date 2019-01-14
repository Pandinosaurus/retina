// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Differences;
import ch.ethz.idsc.tensor.alg.Normalize;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.opt.BSplineFunction;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.sca.Clip;
import ch.ethz.idsc.tensor.sca.Floor;
import ch.ethz.idsc.tensor.sca.Power;

public class BSplineTrack implements TrackInterface {
  private static final int SPLINE_ORDER_TRACK = 2;
  private static final int SPLINE_ORDER_RADIUS = 2;
  private static final TensorUnaryOperator NORMALIZE = Normalize.with(Norm._2);
  // ---
  protected final Tensor controlPoints;
  protected final Tensor controlPointsR;
  protected final boolean closed;
  final Scalar length;
  final int numPoints;
  final BSplineFunction trackSpline;
  final BSplineFunction trackSplineDerivation;
  final BSplineFunction trackSpline2ndDerivation;
  final BSplineFunction radiusTrackSpline;
  // for fast lookup
  // using floats
  final float lookupRes = 0.005f;
  final int lookupSkip = 200;
  final float[] posX;
  final float[] posY;

  // TODO jph/MH implementation is not good
  public BSplineTrack(Tensor controlPointsX, Tensor controlPointsY, Tensor radiusControlPoints, boolean closed) {
    // TODO ensure control points are of same size and [m]
    // controlPointsX.get();
    this.closed = closed;
    int toAdd = Math.max(SPLINE_ORDER_TRACK, SPLINE_ORDER_RADIUS) + 2;
    numPoints = controlPointsX.length();
    this.controlPoints = Transpose.of(Tensors.of(controlPointsX, controlPointsY));
    this.controlPointsR = radiusControlPoints.copy();
    final int pathLength = controlPointsX.length();
    if (closed)
      length = RealScalar.of(pathLength);
    else
      // length = RealScalar.of(pathLength-SPLINE_ORDER_TRACK/2);
      // TODO find out exactly how the formula is
      length = RealScalar.of(pathLength - 1);
    // add points at the end in order to close the loop
    int next = 0;
    while (toAdd > 0) {
      if (next >= pathLength)
        next = 0;
      // TODO MH check if still issue
      this.controlPoints.append(Tensors.of(controlPointsX.get(next), controlPointsY.get(next)));
      this.controlPointsR.append(radiusControlPoints.get(next));
      ++next;
      --toAdd;
    }
    trackSpline = BSplineFunction.of(SPLINE_ORDER_TRACK, controlPoints);
    Tensor devControl = Differences.of(controlPoints);
    trackSplineDerivation = BSplineFunction.of(SPLINE_ORDER_TRACK - 1, devControl);
    Tensor devDevControl = Differences.of(devControl);
    trackSpline2ndDerivation = BSplineFunction.of(SPLINE_ORDER_TRACK - 2, devDevControl);
    radiusTrackSpline = BSplineFunction.of(SPLINE_ORDER_RADIUS, this.controlPointsR);
    // prepare lookup
    if (closed) {
      posX = new float[(int) (controlPointsX.length() / lookupRes)];
      posY = new float[(int) (controlPointsY.length() / lookupRes)];
      for (int i = 0; i < controlPointsX.length() / lookupRes; ++i) {
        Tensor pos = getPosition(RealScalar.of(i * lookupRes));
        posX[i] = pos.Get(0).number().floatValue();
        posY[i] = pos.Get(1).number().floatValue();
      }
    } else {
      posX = new float[(int) ((controlPointsX.length() - 1) / lookupRes)];
      posY = new float[(int) ((controlPointsY.length() - 1) / lookupRes)];
      for (int i = 0; i < (controlPointsX.length() - 1) / lookupRes; ++i) {
        Tensor pos = getPosition(RealScalar.of(i * lookupRes));
        posX[i] = pos.Get(0).number().floatValue();
        posY[i] = pos.Get(1).number().floatValue();
      }
    }
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  public Tensor getControlPoints() {
    return controlPoints.copy();
  }

  private Scalar wrap(Scalar pathProgress) {
    // TODO check if there any specialized functions in the tensor library
    if (closed) {
      Scalar offset = Quantity.of(Math.max(SPLINE_ORDER_TRACK, SPLINE_ORDER_RADIUS) / 2.0 - 0.5, SI.ONE);
      Scalar startPoint = Floor.of(pathProgress.subtract(offset).divide(length)).multiply(length);
      return pathProgress.subtract(startPoint);
    }
    return Clip.function(length.zero(), length).apply(pathProgress);
  }

  /** get position at a certain path value
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return position [m] */
  public Tensor getPosition(Scalar pathProgress) {
    return trackSpline.apply(wrap(pathProgress));
  }

  /** get radius at a certain path value
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return radius [m] */
  public Scalar getRadius(Scalar pathProgress) {
    return (Scalar) radiusTrackSpline.apply(wrap(pathProgress));
  }

  // public Scalar getCurvature(Scalar pathPro)
  /** get the path derivative with respect to path progress
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return change rate of position unit [m/1] */
  public Tensor getDerivation(Scalar pathProgress) {
    Scalar devPathProgress = pathProgress.add(RealScalar.of(-0.5));
    return trackSplineDerivation.apply(wrap(devPathProgress));
  }

  /** get the path direction with respect to path progress
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return direction of the path [1] */
  public Tensor getDirection(Scalar pathProgress) {
    return NORMALIZE.apply(getDerivation(pathProgress));
  }

  /** get perpendicular vector to the right of the path
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return direction of the path [1] */
  public Tensor getRightDirection(Scalar pathProgress) {
    Tensor direction = getDerivation(pathProgress);
    return NORMALIZE.apply(Tensors.of(direction.Get(1), direction.Get(0).negate()));
  }

  /** get the 2nd path derivative with respect to path progress
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return change rate of position unit [m/1^2] */
  public Tensor get2ndDerivation(Scalar pathProgress) {
    Scalar devPathProgress = pathProgress.add(RealScalar.of(-1));
    return trackSpline2ndDerivation.apply(wrap(devPathProgress));
  }

  /** get the curvature
   * 
   * @param pathProgress progress along path
   * corresponding to control point indices [1]
   * @return curvature unit [1/m] */
  public Scalar getCurvature(Scalar pathProgress) {
    Tensor firstDer = getDerivation(pathProgress);
    Tensor secondDer = get2ndDerivation(pathProgress);
    // TODO MH use Det2D+
    // Scalar upper1 = Det2D.of(firstDer, secondDer);
    Scalar upper = firstDer.Get(0).multiply(secondDer.Get(1)) //
        .subtract(firstDer.Get(1).multiply(secondDer.Get(0)));
    Scalar under = Power.of(Norm._2.of(firstDer), 3.0);
    return upper.divide(under);
  }

  public Scalar getLocalRadius(Scalar pathProgress) {
    // TODO MH reciprocal
    return RealScalar.ONE.divide(getCurvature(pathProgress).abs());
  }

  Scalar getNearestPathProgress(Tensor position) {
    if (closed)
      return getFastNearestPathProgress(position);
    return getFastNearestPathProgressOpen(position);
  }

  /** problem: using normal BSpline implementation takes more time than full MPC optimization
   * solution: fast position lookup: from 45000 micro s -> 15 micro s */
  private Scalar getFastNearestPathProgress(Tensor position) {
    float gPosX = position.Get(0).number().floatValue();
    float gPosY = position.Get(1).number().floatValue();
    // first control point
    float bestDist = 10000f;
    int bestGuess = 0;
    // initial guesses
    for (int i = 0; i < numPoints; i++) {
      int index = i * lookupSkip;
      // quadratic distances
      float dist = getFastQuadraticDistance(index, gPosX, gPosY);
      if (dist < bestDist) {
        bestDist = dist;
        bestGuess = index;
      }
    }
    // refinement
    int precision = 4;
    while (precision > 1) {
      int upper = bestGuess + precision;
      if (upper >= posX.length)
        upper -= posX.length;
      if (upper < 0)
        upper += posX.length;
      float upperDist = getFastQuadraticDistance(upper, gPosX, gPosY);
      int lower = bestGuess - precision;
      if (lower >= posX.length)
        lower -= posX.length;
      if (lower < 0)
        lower += posX.length;
      float lowerDist = getFastQuadraticDistance(lower, gPosX, gPosY);
      if (lowerDist < bestDist) {
        bestGuess = lower;
        bestDist = lowerDist;
      } else //
      if (upperDist < bestDist) {
        bestGuess = upper;
        bestDist = upperDist;
      } else
        precision = precision / 2;
      // System.out.println("pos: "+bestGuess*lookupRes);
      // System.out.println(Math.sqrt(getFastQuadraticDistance(bestGuess, gPosX, gPosY)));
    }
    return RealScalar.of(bestGuess * lookupRes);
  }

  /** problem: using normal BSpline implementation takes more time than full MPC optimization
   * solution: fast position lookup: from 45000 micro s -> 15 micro s */
  private Scalar getFastNearestPathProgressOpen(Tensor position) {
    float gPosX = position.Get(0).number().floatValue();
    float gPosY = position.Get(1).number().floatValue();
    // first control point
    float bestDist = 10000f;
    int bestGuess = 0;
    // initial guesses
    for (int i = 0; i < numPoints - SPLINE_ORDER_TRACK; i++) {
      int index = i * lookupSkip;
      // quadratic distances
      float dist = getFastQuadraticDistance(index, gPosX, gPosY);
      if (dist < bestDist) {
        bestDist = dist;
        bestGuess = index;
      }
    }
    // refinement
    int precision = 4;
    while (precision > 1) {
      int upper = bestGuess + precision;
      if (upper >= posX.length)
        upper = posX.length - 1;
      if (upper < 0)
        upper = 0;
      float upperDist = getFastQuadraticDistance(upper, gPosX, gPosY);
      int lower = bestGuess - precision;
      if (lower >= posX.length)
        lower = posX.length - 1;
      if (lower < 0)
        lower = 0;
      float lowerDist = getFastQuadraticDistance(lower, gPosX, gPosY);
      if (lowerDist < bestDist) {
        bestGuess = lower;
        bestDist = lowerDist;
      } else //
      if (upperDist < bestDist) {
        bestGuess = upper;
        bestDist = upperDist;
      } else
        precision = precision / 2;
      // System.out.println("pos: "+bestGuess*lookupRes);
      // System.out.println(Math.sqrt(getFastQuadraticDistance(bestGuess, gPosX, gPosY)));
    }
    return RealScalar.of(bestGuess * lookupRes);
  }

  float getFastQuadraticDistance(int index, float gPosX, float gPosY) {
    float dx = gPosX - posX[index];
    float dy = gPosY - posY[index];
    // quadratic distances
    return dx * dx + dy * dy;
  }

  private Scalar getDist(Tensor from, Scalar pathProgress) {
    return Norm._2.of(getPosition(pathProgress).subtract(from));
  }

  @Override
  public boolean isInTrack(Tensor position) {
    Scalar prog = getNearestPathProgress(position);
    Scalar dist = getDist(position, prog);
    return Scalars.lessThan(dist, getRadius(prog));
  }

  @Override
  public Tensor getNearestPosition(Tensor position) {
    return getPosition(getNearestPathProgress(position));
  }

  @Override
  public Tensor getMiddleLine(int resolution) {
    Tensor line = Tensors.empty();
    Scalar step = length.divide(RealScalar.of(resolution));
    for (int i = 0; i < resolution; i++) {
      Scalar prog = RealScalar.of(i).multiply(step);
      line.append(getPosition(prog));
    }
    return line;
  }

  @Override
  public Tensor getLeftLine(int resolution) {
    // this is not accurate for large changes in radius
    Tensor line = Tensors.empty();
    Scalar step = length.divide(RealScalar.of(resolution));
    for (int i = 0; i < resolution; i++) {
      Scalar prog = RealScalar.of(i).multiply(step);
      Tensor linepos = //
          getPosition(prog) //
              .add(getRightDirection(prog) //
                  .multiply(getRadius(prog)));
      line.append(linepos);
    }
    return line;
  }

  // TODO refactor so that left and right reuse code
  @Override
  public Tensor getRightLine(int resolution) {
    // this is not accurate for large changes in radius
    Tensor line = Tensors.empty();
    Scalar step = length.divide(RealScalar.of(resolution));
    for (int i = 0; i < resolution; i++) {
      Scalar prog = RealScalar.of(i).multiply(step);
      Tensor linepos = //
          getPosition(prog).//
              subtract(getRightDirection(prog).//
                  multiply(getRadius(prog)));
      line.append(linepos);
    }
    return line;
  }
}
