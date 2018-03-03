// code by jph
package ch.ethz.idsc.gokart.gui;

import java.util.Arrays;
import java.util.List;

import ch.ethz.idsc.gokart.core.AutoboxSocketModule;
import ch.ethz.idsc.gokart.core.fuse.DavisImuWatchdog;
import ch.ethz.idsc.gokart.core.fuse.LinmotCoolingModule;
import ch.ethz.idsc.gokart.core.fuse.LinmotTakeoverModule;
import ch.ethz.idsc.gokart.core.fuse.MiscEmergencyModule;
import ch.ethz.idsc.gokart.core.fuse.SteerEmergencyModule;
import ch.ethz.idsc.gokart.core.fuse.Vlp16ClearanceModule;
import ch.ethz.idsc.gokart.core.joy.DeadManSwitchModule;
import ch.ethz.idsc.gokart.core.joy.JoystickGroupModule;
import ch.ethz.idsc.gokart.core.pos.GokartPoseLcmModule;
import ch.ethz.idsc.gokart.core.pure.PurePursuitModule;
import ch.ethz.idsc.gokart.gui.lab.AutoboxTestingModule;
import ch.ethz.idsc.gokart.gui.top.GlobalViewLcmModule;
import ch.ethz.idsc.gokart.lcm.mod.AutoboxLcmServerModule;
import ch.ethz.idsc.gokart.lcm.mod.Vlp16LcmServerModule;
import ch.ethz.idsc.retina.sys.AppCustomization;
import ch.ethz.idsc.retina.sys.LoggerModule;
import ch.ethz.idsc.retina.sys.SpyModule;
import ch.ethz.idsc.retina.sys.TabbedTaskGui;
import ch.ethz.idsc.retina.util.gui.WindowConfiguration;

enum RunTabbedTaskGui {
  ;
  static final List<Class<?>> MODULES_DEV = Arrays.asList( //
      AutoboxSocketModule.class, // sensing and actuation
      Vlp16LcmServerModule.class, // sensing
      AutoboxLcmServerModule.class, //
      GokartStatusLcmModule.class, //
      GokartPoseLcmModule.class, // move to DEV list
      LoggerModule.class //
  );
  static final List<Class<?>> MODULES_LAB = Arrays.asList( //
      // Urg04lxLcmServerModule.class, // sensing
      SpyModule.class, //
      ParametersModule.class, //
      AutoboxIntrospectionModule.class, //
      AutoboxTestingModule.class, //
      // LocalViewLcmModule.class, //
      GlobalViewLcmModule.class, //
      DavisDetailModule.class, //
      PanoramaViewModule.class // , //
  // DavisOverviewModule.class //
  );
  static final List<Class<?>> MODULES_FUSE = Arrays.asList( //
      // Urg04lxEmergencyModule.class, //
      MiscEmergencyModule.class, //
      SteerEmergencyModule.class, //
      // LinmotEmergencyModule.class, //
      LinmotCoolingModule.class, //
      LinmotTakeoverModule.class, //
      Vlp16ClearanceModule.class, //
      DavisImuWatchdog.class
  // Urg04lxClearanceModule.class //
  );
  static final List<Class<?>> MODULES_JOY = Arrays.asList( //
      DeadManSwitchModule.class, // joystick
      JoystickGroupModule.class //
  // LinmotJoystickModule.class, //
  // SteerJoystickModule.class, //
  // RimoTorqueJoystickModule.class //
  );
  static final List<Class<?>> MODULES_AUT = Arrays.asList( //
      PurePursuitModule.class //
  );

  public static void main(String[] args) {
    WindowConfiguration wc = AppCustomization.load(RunTabbedTaskGui.class, new WindowConfiguration());
    TabbedTaskGui taskTabGui = new TabbedTaskGui();
    taskTabGui.tab("dev", MODULES_DEV);
    taskTabGui.tab("lab", MODULES_LAB);
    taskTabGui.tab("fuse", MODULES_FUSE);
    taskTabGui.tab("joy", MODULES_JOY);
    taskTabGui.tab("aut", MODULES_AUT);
    wc.attach(RunTabbedTaskGui.class, taskTabGui.jFrame);
    taskTabGui.jFrame.setVisible(true);
  }
}
