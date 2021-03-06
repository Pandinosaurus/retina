// code by jph
package ch.ethz.idsc.retina.util.time;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** demo was developed to design timer and semaphore interplay in AbstractClockedModule */
/* package */ class TimerDemo {
  Semaphore semaphore = new Semaphore(1);
  Timer timer = new Timer();

  public TimerDemo() {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (semaphore.tryAcquire()) {
          System.out.println("task acquire req");
          System.out.println("task acquired");
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println("task release");
          semaphore.release();
        } else {
          System.out.println("task acquire fail");
        }
      }
    }, 10, 10);
  }

  public static void main(String[] args) throws InterruptedException {
    TimerDemo timerDemo = new TimerDemo();
    Thread.sleep(1000);
    System.out.println("cancel issued");
    timerDemo.timer.cancel();
    System.out.println("cancel acquire");
    timerDemo.semaphore.tryAcquire(3, TimeUnit.SECONDS); // wait for all task to finish
    System.out.println("cancel guarantee");
  }
}
