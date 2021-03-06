// code by jph
package ch.ethz.idsc.gokart.lcm.mod;

import ch.ethz.idsc.gokart.lcm.lidar.VelodyneLcmServers;
import ch.ethz.idsc.retina.lidar.VelodyneModel;
import ch.ethz.idsc.retina.lidar.VelodyneStatics;
import ch.ethz.idsc.retina.util.sys.StartAndStoppableModule;

public final class Vlp16RayLcmServerModule extends StartAndStoppableModule {
  public Vlp16RayLcmServerModule() {
    super(VelodyneLcmServers.ray(VelodyneModel.VLP16, "center", VelodyneStatics.RAY_PORT_DEFAULT));
  }
}
