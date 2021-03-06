// code by jph
package ch.ethz.idsc.gokart.offline.gui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.idsc.gokart.calib.steer.SteerColumnEvent;
import ch.ethz.idsc.gokart.calib.steer.SteerColumnListener;
import ch.ethz.idsc.gokart.core.mpc.ControlAndPredictionSteps;
import ch.ethz.idsc.gokart.core.mpc.ControlAndPredictionStepsMessage;
import ch.ethz.idsc.gokart.core.mpc.MPCControlUpdateListener;
import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.GokartPoseListener;
import ch.ethz.idsc.gokart.core.pure.ClothoidPlan;
import ch.ethz.idsc.gokart.core.pure.ClothoidPlanListener;
import ch.ethz.idsc.gokart.core.track.BSplineTrackListener;
import ch.ethz.idsc.gokart.dev.linmot.LinmotGetEvent;
import ch.ethz.idsc.gokart.dev.linmot.LinmotGetListener;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetListener;
import ch.ethz.idsc.gokart.dev.steer.SteerGetEvent;
import ch.ethz.idsc.gokart.dev.steer.SteerGetListener;
import ch.ethz.idsc.gokart.dev.u3.GokartLabjackFrame;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.lcm.OfflineLogListener;
import ch.ethz.idsc.gokart.lcm.OfflineLogPlayer;
import ch.ethz.idsc.gokart.lcm.autobox.LinmotLcmServer;
import ch.ethz.idsc.gokart.lcm.autobox.RimoLcmServer;
import ch.ethz.idsc.gokart.lcm.autobox.SteerLcmServer;
import ch.ethz.idsc.gokart.lcm.davis.DavisDvsBlockPublisher;
import ch.ethz.idsc.gokart.lcm.lidar.VelodyneLcmChannels;
import ch.ethz.idsc.gokart.lcm.mod.ClothoidPlanLcm;
import ch.ethz.idsc.gokart.lcm.mod.Se2CurveLcm;
import ch.ethz.idsc.retina.davis.data.DavisDvsDatagramDecoder;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrame;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrameListener;
import ch.ethz.idsc.retina.joystick.ManualControlListener;
import ch.ethz.idsc.retina.lidar.VelodyneModel;
import ch.ethz.idsc.retina.lidar.VelodynePosEvent;
import ch.ethz.idsc.retina.util.gps.Gprmc;
import ch.ethz.idsc.retina.util.gps.GprmcListener;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.ref.TensorListener;
import ch.ethz.idsc.tensor.sca.Round;

public class GokartLogFileIndexer implements OfflineLogListener {
  public static GokartLogFileIndexer create(File file) throws IOException {
    GokartLogFileIndexer gokartLogFileIndexer = new GokartLogFileIndexer(file);
    // ---
    gokartLogFileIndexer.append(0);
    Scalar mb = RationalScalar.of(file.length(), 1000_000_000);
    System.out.print("building index... " + mb.map(Round._2) + " GB ");
    OfflineLogPlayer.process(file, gokartLogFileIndexer);
    System.out.println("done.");
    return gokartLogFileIndexer;
  }

  public static GokartLogFileIndexer empty() {
    return new GokartLogFileIndexer(null);
  }

  // ---
  static final Scalar RESOLUTION = Quantity.of(0.25, SI.SECOND);
  private static final String VLP16_CENTER_POS = VelodyneLcmChannels.pos(VelodyneModel.VLP16, "center");
  private static final String DVS_CHANNEL = DavisDvsBlockPublisher.channel("overview");
  // ---
  private int gprmc_errors = 0;
  private final DvsCountRow dvsCountRow = new DvsCountRow();
  private final TrajectoryCountRow trajectoryCountRow = new TrajectoryCountRow();
  // private final DavisDvsDatagramDecoder davisDvsDatagramDecoder = new DavisDvsDatagramDecoder();
  // ---
  private final File file;
  private final List<Integer> raster2event = new ArrayList<>();
  // ---
  final List<GokartLogImageRow> gokartLogImageRows = new LinkedList<>();
  private final List<SteerGetListener> steerGetListeners = new LinkedList<>();
  private final List<LinmotGetListener> linmotGetListeners = new LinkedList<>();
  private final List<SteerColumnListener> steerColumnListeners = new LinkedList<>();
  private final List<ManualControlListener> manualControlListeners = new LinkedList<>();
  private final List<GokartPoseListener> gokartPoseListeners = new LinkedList<>();
  private final List<RimoGetListener> rimoGetListeners = new LinkedList<>();
  private final List<Vmu931ImuFrameListener> vmu931ImuFrameListeners = new LinkedList<>();
  private final List<MPCControlUpdateListener> mpcControlUpdateListeners = new LinkedList<>();
  private final List<TensorListener> tensorListeners = new LinkedList<>();
  private final List<ClothoidPlanListener> clothoidPlanListeners = new LinkedList<>();
  private final List<BSplineTrackListener> bsplineTrackListeners = new LinkedList<>();
  private final List<GprmcListener> gprmcListeners = new LinkedList<>();
  // ---
  private int event_count;

  private GokartLogFileIndexer(File file) {
    this.file = file;
    // actuators
    addRow(new SteerAngleRow());
    addRow(new SteerTsuTrqRow());
    addRow(new SteerRefMotTrqRow());
    addRow(new SteerStatusRow());
    addRow(new RimoRateRow(0));
    addRow(new RimoRateRow(1));
    addRow(new LinmotPositionRow());
    addRow(new LinmotTemperatureRow());
    addRow(new LinmotStatusRow());
    // sensors
    addRow(new Vmu931RateRow());
    addRow(dvsCountRow);
    addRow(new GprmcRow());
    // user
    addRow(new AutonomousButtonRow());
    addRow(new ResetButtonRow());
    // state estimation
    addRow(new PoseQualityRow());
    // planning
    addRow(trajectoryCountRow);
    // addRow(new BSplineTrackRow());
    addRow(new MpcCountRow());
    addRow(new CurveMessageRow());
    addRow(new ClothoidPlanRow());
  }

  private void addRow(GokartLogImageRow gokartLogImageRow) {
    gokartLogImageRows.add(gokartLogImageRow);
    if (gokartLogImageRow instanceof SteerGetListener)
      steerGetListeners.add((SteerGetListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof LinmotGetListener)
      linmotGetListeners.add((LinmotGetListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof RimoGetListener)
      rimoGetListeners.add((RimoGetListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof SteerColumnListener)
      steerColumnListeners.add((SteerColumnListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof ManualControlListener)
      manualControlListeners.add((ManualControlListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof GokartPoseListener)
      gokartPoseListeners.add((GokartPoseListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof Vmu931ImuFrameListener)
      vmu931ImuFrameListeners.add((Vmu931ImuFrameListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof MPCControlUpdateListener)
      mpcControlUpdateListeners.add((MPCControlUpdateListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof TensorListener)
      tensorListeners.add((TensorListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof ClothoidPlanListener)
      clothoidPlanListeners.add((ClothoidPlanListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof BSplineTrackListener)
      bsplineTrackListeners.add((BSplineTrackListener) gokartLogImageRow);
    if (gokartLogImageRow instanceof GprmcListener)
      gprmcListeners.add((GprmcListener) gokartLogImageRow);
  }

  private void append(int count) {
    raster2event.add(count);
    gokartLogImageRows.forEach(GokartLogImageRow::append);
  }

  @Override // from OfflineLogListener
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    int index = time.divide(RESOLUTION).number().intValue();
    if (raster2event.size() <= index)
      append(event_count);
    if (channel.equals(SteerLcmServer.CHANNEL_GET)) {
      SteerGetEvent steerGetEvent = new SteerGetEvent(byteBuffer);
      steerGetListeners.forEach(listener -> listener.getEvent(steerGetEvent));
    } else //
    if (channel.equals(LinmotLcmServer.CHANNEL_GET)) {
      LinmotGetEvent linmotGetEvent = new LinmotGetEvent(byteBuffer);
      linmotGetListeners.forEach(listener -> listener.getEvent(linmotGetEvent));
    } else //
    if (channel.equals(RimoLcmServer.CHANNEL_GET)) {
      RimoGetEvent rimoGetEvent = new RimoGetEvent(byteBuffer);
      rimoGetListeners.forEach(listener -> listener.getEvent(rimoGetEvent));
    } else //
    if (channel.equals(GokartLcmChannel.POSE_LIDAR)) {
      GokartPoseEvent gokartPoseEvent = GokartPoseEvent.of(byteBuffer);
      gokartPoseListeners.forEach(listener -> listener.getEvent(gokartPoseEvent));
    } else //
    if (channel.equals(GokartLcmChannel.LABJACK_U3_ADC)) {
      GokartLabjackFrame gokartLabjackFrame = new GokartLabjackFrame(byteBuffer);
      manualControlListeners.forEach(listener -> listener.manualControl(gokartLabjackFrame));
    } else //
    if (channel.equals(GokartLcmChannel.STATUS)) {
      SteerColumnEvent steerColumnEvent = new SteerColumnEvent(byteBuffer);
      steerColumnListeners.forEach(listener -> listener.getEvent(steerColumnEvent));
    } else //
    if (channel.equals(GokartLcmChannel.VMU931_AG)) {
      Vmu931ImuFrame vmu931ImuFrame = new Vmu931ImuFrame(byteBuffer);
      vmu931ImuFrameListeners.forEach(listener -> listener.vmu931ImuFrame(vmu931ImuFrame));
    } else //
    if (channel.equals(GokartLcmChannel.TRAJECTORY_XYAT_STATETIME) || //
        channel.equals(GokartLcmChannel.TRAJECTORY_XYAVT_STATETIME)) {
      trajectoryCountRow.increment();
    } else //
    if (channel.equals(GokartLcmChannel.MPC_FORCES_CNS)) {
      ControlAndPredictionSteps controlAndPredictionSteps = //
          new ControlAndPredictionStepsMessage(byteBuffer).getPayload();
      mpcControlUpdateListeners.forEach(listener -> listener.getControlAndPredictionSteps(controlAndPredictionSteps));
    } else //
    if (channel.equals(GokartLcmChannel.PURSUIT_CURVE_SE2)) {
      Tensor tensor = Se2CurveLcm.decode(byteBuffer).unmodifiable();
      tensorListeners.forEach(listener -> listener.tensorReceived(tensor));
    } else //
    if (channel.equals(GokartLcmChannel.PURSUIT_PLAN)) {
      ClothoidPlan clothoidPlan = ClothoidPlanLcm.decode(byteBuffer);
      clothoidPlanListeners.forEach(listener -> listener.planReceived(clothoidPlan));
    } else //
    // for now, only closed tracks are relevant
    if (channel.equals(GokartLcmChannel.XYR_TRACK_CYCLIC)) {
      // TODO include again, once publishing rate is reduced
      // Optional<BSplineTrack> optional = BSplineTrackLcm.decode(channel, byteBuffer);
      // bsplineTrackListeners.forEach(listener -> listener.bSplineTrack(optional));
    } else //
    if (channel.equals(VLP16_CENTER_POS)) {
      VelodynePosEvent velodynePosEvent = VelodynePosEvent.vlp16(byteBuffer);
      try {
        Gprmc gprmc = velodynePosEvent.gprmc();
        gprmcListeners.forEach(listener -> listener.gprmcReceived(gprmc));
      } catch (Exception exception) {
        if (++gprmc_errors <= 3)
          exception.printStackTrace();
      }
    } else //
    if (channel.equals(DVS_CHANNEL)) {
      int eventCount = DavisDvsDatagramDecoder.eventCount(byteBuffer);
      dvsCountRow.increment(eventCount);
    }
    ++event_count;
  }

  public File file() {
    return file;
  }

  public int getEventIndex(int x0) {
    return raster2event.get(x0);
  }

  public int getRasterSize() {
    return raster2event.size();
  }
}
