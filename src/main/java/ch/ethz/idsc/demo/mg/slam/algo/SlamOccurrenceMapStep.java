// code by mg
package ch.ethz.idsc.demo.mg.slam.algo;

import java.util.Objects;

import ch.ethz.idsc.demo.mg.slam.SlamConfig;
import ch.ethz.idsc.demo.mg.slam.SlamContainer;

/** update of occurrence map using the particles */
/* package */ class SlamOccurrenceMapStep extends EventActionSlamStep {
  private final int relevantParticles;

  protected SlamOccurrenceMapStep(SlamConfig slamConfig, SlamContainer slamContainer) {
    super(slamContainer);
    relevantParticles = slamConfig.relevantParticles.number().intValue();
  }

  @Override
  void davisDvsAction() {
    if (Objects.nonNull(slamContainer.getEventGokartFrame()))
      SlamOccurrenceMapStepUtil.updateOccurrenceMap(slamContainer.getSlamParticles(), slamContainer.getOccurrenceMap(), //
          slamContainer.getEventGokartFrame(), relevantParticles);
  }
}
