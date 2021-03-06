// code by jph
package ch.ethz.idsc.demo.jph.sys;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.idsc.demo.GokartLogFiles;
import ch.ethz.idsc.gokart.offline.api.LogFile;
import ch.ethz.idsc.gokart.offline.api.LogFileLocator;

public enum DatahakiLogFileLocator implements LogFileLocator {
  INSTANCE;
  // ---
  /** the archive of all log files is kept on an external hard-drive */
  private static final File EXT_HD1 = new File("/media/datahaki/backup/gokartlogs");
  private static final File EXT_HD2 = new File("/media/datahaki/gokart/gokartlogs");
  private static final List<File> LOG_ROOT = Arrays.asList( //
      new File("/media/datahaki/media/ethz/gokartlogs"), //
      EXT_HD1, //
      EXT_HD2);

  @Override // from LogFileLocator
  public File getAbsoluteFile(LogFile logFile) {
    String title = logFile.getFilename();
    String date = title.substring(0, 8); // e.g. "20180924"
    for (File dir : LOG_ROOT) {
      File file = new File(new File(dir, date), title);
      if (file.isFile())
        return file;
    }
    return null;
  }

  /** @param logFile
   * @return valid log file, or null if file could not be found */
  public static File file(LogFile logFile) {
    return INSTANCE.getAbsoluteFile(logFile);
  }

  /** @return */
  public static Collection<LogFile> all() {
    Collection<LogFile> collection = new LinkedList<>();
    for (File root : LOG_ROOT)
      try {
        collection.addAll(GokartLogFiles.all(root));
      } catch (Exception exception) {
        // ---
      }
    return collection;
  }
}
