// code by jph
package ch.ethz.idsc.demo.jph.lidar.hist;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import ch.ethz.idsc.owl.bot.util.UserHome;
import ch.ethz.idsc.retina.dev.lidar.VelodyneModel;
import ch.ethz.idsc.retina.dev.lidar.vlp16.Vlp16Decoder;
import ch.ethz.idsc.retina.lcm.OfflineLogListener;
import ch.ethz.idsc.retina.lcm.OfflineLogPlayer;
import ch.ethz.idsc.retina.lcm.lidar.VelodyneLcmChannels;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.io.Put;

enum GokartRayLogExport {
  ;
  public static void main(String[] args) throws IOException {
    final String channel = VelodyneLcmChannels.ray(VelodyneModel.VLP16, "center");
    Vlp16Decoder vlp16Decoder = new Vlp16Decoder();
    RotationalHistogram listener = new RotationalHistogram();
    vlp16Decoder.addRayListener(listener);
    TemporalHistogram temporalHistogram = new TemporalHistogram();
    vlp16Decoder.addRayListener(temporalHistogram);
    PlanarHistogram planarHistogram = new PlanarHistogram();
    vlp16Decoder.addRayListener(planarHistogram);
    OfflineLogListener offlineLogListener = new OfflineLogListener() {
      @Override
      public void event(Scalar time, String _channel, ByteBuffer byteBuffer) {
        if (_channel.equals(channel))
          vlp16Decoder.lasers(byteBuffer);
      }
    };
    File file = new File("/media/datahaki/media/ethz/gokartlogs", "20180112T105400_9e1d3699.lcm.00");
    OfflineLogPlayer.process(file, offlineLogListener);
    Put.of(UserHome.file("ray_angles.wmt"), listener.histogram);
    Put.of(UserHome.file("ray_times.wmt"), temporalHistogram.histogram);
    Put.of(UserHome.file("ray_planar.wmt"), planarHistogram.compile());
  }
}
