// code by mh, jph
package ch.ethz.idsc.gokart.offline.tab;

import java.nio.ByteBuffer;

import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.GokartPoseHelper;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.offline.api.OfflineTableSupplier;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.TableBuilder;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Round;
import ch.ethz.idsc.tensor.sca.Sign;

/** table with pose messages
 * 
 * pose messages are typically published at 50[Hz] */
public class GokartPoseTable implements OfflineTableSupplier {
  private final TableBuilder tableBuilder = new TableBuilder();
  private final Scalar delta;
  // ---
  private Scalar time_next = Quantity.of(0, SI.SECOND);

  public GokartPoseTable(Scalar delta) {
    this.delta = Sign.requirePositiveOrZero(delta);
  }

  @Override // from OfflineLogListener
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals(GokartLcmChannel.POSE_LIDAR) && //
        Scalars.lessThan(time_next, time)) {
      GokartPoseEvent gokartPoseEvent = new GokartPoseEvent(byteBuffer);
      time_next = time.add(delta);
      Tensor pose = GokartPoseHelper.toUnitless(gokartPoseEvent.getPose());
      // TODO change order to {time, pose, quality}
      tableBuilder.appendRow( //
          time.map(Magnitude.SECOND).map(Round._6), //
          gokartPoseEvent.getQuality().map(Round._3), // 1
          pose.map(Round._6) // 2
      );
    }
  }

  @Override // from OfflineTableSupplier
  public Tensor getTable() {
    return tableBuilder.toTable();
  }
}
