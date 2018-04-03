package ch.ethz.idsc.demo.mg.pipeline;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.demo.mg.LogfileLocations;
import ch.ethz.idsc.retina.lcm.OfflineLogPlayer;

// choose log file to run pipeline
public enum RunPipeline {
  ;
  public static void main(String[] args) throws IOException {
    File file = new File(LogfileLocations.DUBI4a);
    InputSubModule input = new InputSubModule();
    OfflineLogPlayer.process(file, input);
  }
}
