// code by jph
package ch.ethz.idsc.gokart.core.track;

import ch.ethz.idsc.gokart.core.perc.SpacialXZObstaclePredicate;
import junit.framework.TestCase;

public class TrackReconConfigTest extends TestCase {
  public void testSimple() {
    SpacialXZObstaclePredicate createSpacialXZObstaclePredicate = //
        TrackReconConfig.GLOBAL.createSpacialXZObstaclePredicate();
    assertTrue(createSpacialXZObstaclePredicate.isObstacle(3f, -.6f));
    assertFalse(createSpacialXZObstaclePredicate.isObstacle(3f, .6f));
  }
}
