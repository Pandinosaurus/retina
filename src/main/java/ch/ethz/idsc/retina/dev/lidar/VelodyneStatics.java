// code by jph
package ch.ethz.idsc.retina.dev.lidar;

public enum VelodyneStatics {
  ;
  // CONSTANTS
  /** length of firing packet */
  public static final int RAY_PACKET_LENGTH = 1206;
  /** length of positioning packet */
  public static final int POS_PACKET_LENGTH = 512;
  // ---
  // DEFAULT VALUES
  /** default port on which vlp16/hdl32e publishes firing data */
  public static final int RAY_DEFAULT_PORT = 2368;
  /** default port on which vlp16/hdl32e publishes positioning data */
  public static final int POS_DEFAULT_PORT = 8308;
  // /** default local address */
  // public static final String DEFAULT_LADDR = "192.168.1.1";
  /**
   * 
   */
  public static final double TO_METER = 0.002;
  public static final float TO_METER_FLOAT = (float) TO_METER;
}
