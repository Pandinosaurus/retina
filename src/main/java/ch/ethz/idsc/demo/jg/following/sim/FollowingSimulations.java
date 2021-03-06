// code by gjoel
package ch.ethz.idsc.demo.jg.following.sim;

import java.util.Optional;

import ch.ethz.idsc.demo.jg.following.analysis.ErrorInterface;
import ch.ethz.idsc.demo.jg.following.analysis.FollowingError;
import ch.ethz.idsc.gokart.core.pure.ClothoidPlan;
import ch.ethz.idsc.gokart.core.pure.ClothoidPursuitConfig;
import ch.ethz.idsc.gokart.core.pure.CurveClothoidPursuitPlanner;
import ch.ethz.idsc.gokart.core.pure.CurvePurePursuitHelper;
import ch.ethz.idsc.gokart.core.pure.PurePursuitConfig;
import ch.ethz.idsc.gokart.dev.steer.SteerConfig;
import ch.ethz.idsc.owl.bot.se2.Se2CarIntegrator;
import ch.ethz.idsc.owl.bot.se2.glc.Se2CarFlows;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.sophus.math.MinMax;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Timing;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clips;
import ch.ethz.idsc.tensor.sca.Round;
import ch.ethz.idsc.tensor.sca.Sign;

/* package */ enum FollowingSimulations implements ErrorInterface {
  PURE {
    @Override
    public Optional<Scalar> setup(Tensor pose, Tensor speed, Tensor curve) {
      return CurvePurePursuitHelper.getRatio(pose, curve, Sign.isPositiveOrZero(speed.Get(0)), lookAhead());
    }

    @Override
    public Scalar lookAhead() {
      return PurePursuitConfig.GLOBAL.lookAhead;
    }
  },
  CLOTHOID_3_5 {
    private final CurveClothoidPursuitPlanner planner = new CurveClothoidPursuitPlanner(ClothoidPursuitConfig.GLOBAL);

    @Override
    public Optional<Scalar> setup(Tensor pose, Tensor speed, Tensor curve) {
      ClothoidPursuitConfig.GLOBAL.lookAhead = lookAhead();
      return planner.getPlan(pose, speed, curve, //
          Sign.isPositiveOrZero(speed.Get(0))).map(ClothoidPlan::ratio);
    }

    @Override
    public Scalar lookAhead() {
      return Quantity.of(3.5, SI.METER);
    }
  },
  CLOTHOID_5 {
    private final CurveClothoidPursuitPlanner planner = new CurveClothoidPursuitPlanner(ClothoidPursuitConfig.GLOBAL);

    @Override
    public Optional<Scalar> setup(Tensor pose, Tensor speed, Tensor curve) {
      ClothoidPursuitConfig.GLOBAL.lookAhead = lookAhead();
      return planner.getPlan(pose, speed, curve, //
          Sign.isPositiveOrZero(speed.Get(0))).map(ClothoidPlan::ratio);
    }

    @Override
    public Scalar lookAhead() {
      return Quantity.of(5, SI.METER);
    }
  },
  CLOTHOID_7 {
    private final CurveClothoidPursuitPlanner planner = new CurveClothoidPursuitPlanner(ClothoidPursuitConfig.GLOBAL);

    @Override
    public Optional<Scalar> setup(Tensor pose, Tensor speed, Tensor curve) {
      ClothoidPursuitConfig.GLOBAL.lookAhead = lookAhead();
      return planner.getPlan(pose, speed, curve, //
          Sign.isPositiveOrZero(speed.Get(0))).map(ClothoidPlan::ratio);
    }

    @Override
    public Scalar lookAhead() {
      return Quantity.of(7, SI.METER);
    }
  };
  // ---
  private Tensor trail;
  private Tensor ratios;
  private FollowingError followingError;
  private Timing timing;

  public String identifier() {
    return name().split("_")[0] + " (" + lookAhead() + ")";
  }

  /** @param curve reference
   * @param initialPose of vehicle {x[m], y[m], angle}
   * @param speed of vehicle {vx[m*s^-1], vy[m*s^-1], gyroZ[s^-1]}
   * @param duration of simulation [s]
   * @param timeStep of simulation [s] */
  public void run(Tensor curve, Tensor initialPose, Tensor speed, Scalar duration, Scalar timeStep) {
    trail = Tensors.empty();
    ratios = Tensors.empty();
    followingError = new FollowingError();
    timing = Timing.started();
    // ---
    followingError.setReference(curve);
    Tensor pose = initialPose;
    Scalar ratio = Quantity.of(0, SI.PER_METER);
    for (Scalar time = Quantity.of(0, SI.SECOND); Scalars.lessEquals(time, duration); time = time.add(timeStep)) {
      trail.append(pose);
      followingError.insert(time, pose);
      Optional<Scalar> optional = setup(pose, speed, curve);
      if (optional.isPresent())
        ratio = Clips.absolute(SteerConfig.GLOBAL.turningRatioMax).apply(optional.get());
      ratios.append(ratio);
      pose = Se2CarIntegrator.INSTANCE.step(Se2CarFlows.singleton(speed.Get(0), ratio), pose, timeStep);
    }
    timing.stop();
  }

  /** @return vehicle trail {{x[m], y[m], angle}, ...} */
  public Optional<Tensor> trail() {
    return Optional.ofNullable(trail);
  }

  /** @return ratios {[m^-1], ...} */
  public Optional<Tensor> ratios() {
    return Optional.ofNullable(ratios);
  }

  /** @return min and max ratio [m^-1] */
  public Optional<MinMax> ratioRange() {
    return ratios().map(MinMax::of);
  }

  /** @return simulation duation [s] */
  public Optional<Scalar> simulationTime() {
    return Optional.ofNullable(timing).map(t -> Quantity.of(t.seconds(), SI.SECOND));
  }

  @Override // from ErrorInterface
  public final Optional<Tensor> averageError() {
    return followingError.averageError();
  }

  @Override // from ErrorInterface
  public final Optional<Tensor> maximumError() {
    return followingError.maximumError();
  }

  @Override // from ErrorInterface
  public final Optional<Tensor> accumulatedError() {
    return followingError.accumulatedError();
  }

  public final Tensor errors() {
    return followingError.errors();
  }

  @Override // from ErrorInterface
  public Optional<String> getReport() {
    return followingError.getReport().map(report -> //
    identifier() + ratioRange().map(range -> " " + report + //
        "\n\tratios:\tmin = " + Round._4.apply(range.min().Get()) + ", max = " + Round._4.apply(range.max().Get()) + //
        "\n\tsimulation duration:\t" + simulationTime().get()) //
        .orElse(" not yet run"));
  }

  /** @param pose of vehicle {x[m], y[m], angle}
   * @param speed of vehicle {vx[m*s^-1], vy[m*s^-1], gyroZ[s^-1]}
   * @param curve reference
   * @return ratio [m^-1] */
  public abstract Optional<Scalar> setup(Tensor pose, Tensor speed, Tensor curve);

  public abstract Scalar lookAhead();
}
