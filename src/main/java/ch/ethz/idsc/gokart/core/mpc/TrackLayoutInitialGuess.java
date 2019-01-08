// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.mat.LeastSquares;
import ch.ethz.idsc.tensor.mat.LinearSolve;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.red.Norm;

public class TrackLayoutInitialGuess implements RenderInterface {
  private class Cell {
    final int x;
    final int y;
    Scalar cost;
    Boolean inQ = false;
    Boolean processed = false;
    Cell lastCell = null;
    ArrayList<Cell> neighBors = null;
    ArrayList<Scalar> neighBorCost = null;

    // ArrayList<Scalar> neighBorCost;
    public Cell(int x, int y) {
      this.x = x;
      this.y = y;
      this.cost = DoubleScalar.POSITIVE_INFINITY;
    }

    public Tensor getPos() {
      return occupancyGrid.getTransform().dot(Tensors.vector(x, y, 1));
    }

    @Override
    public String toString() {
      return x + " / " + y + " : " + cost + (inQ ? " in Q" : "") + (processed ? " is processed" : "");
    }

    public LinkedList<Cell> getRoute() {
      LinkedList<Cell> result;
      if (lastCell == null)
        result = new LinkedList<>();
      else
        result = lastCell.getRoute();
      result.add(this);
      return result;
    }

    public void findNeighbors() {
      if (neighBors == null) {
        neighBors = new ArrayList<>();
        neighBorCost = new ArrayList<>();
        for (Neighbor n : possibleNeighbors) {
          Cell newNeighBor = n.getFrom(this);
          if (newNeighBor != null) {
            neighBors.add(newNeighBor);
            neighBorCost.add(n.cost);
          }
        }
      }
    }
  }

  private class Neighbor {
    final int dx;
    final int dy;
    final Scalar cost;

    public Neighbor(int dx, int dy) {
      this.dx = dx;
      this.dy = dy;
      cost = RealScalar.of(Math.hypot(dx, dy));
      // Norm._2.of(Tensors.of(RealScalar.of(dx), RealScalar.of(dy)));
    }

    public Cell getFrom(Cell cell) {
      int nx = cell.x + dx;
      int ny = cell.y + dy;
      if (!occupancyGrid.isCellOccupied(nx, ny))
        return cellGrid[nx][ny];
      return null;
    }
  }

  private final PlanableOccupancyGrid occupancyGrid;

  public TrackLayoutInitialGuess(PlanableOccupancyGrid occupancyGrid) {
    this.occupancyGrid = occupancyGrid;
  }

  // private function for dijkstra
  Cell startingPoint;
  PriorityQueue<Cell> Q;
  int m;
  int n;
  // starting point for Dijkstra
  Cell dijkstraStart;
  // goal for dijkstra
  Cell dijkstraGokartBack;
  Cell dijkstraTarget;
  // actual goal
  Cell actualTarget;
  Cell[][] cellGrid;
  ArrayList<Neighbor> possibleNeighbors;
  LinkedList<Cell> route;
  LinkedList<Cell> forwardRoute;
  Tensor routePolygon;
  boolean closed = false;

  // this is potentially slow
  Cell getFarthestCell() {
    Cell farthest = dijkstraStart;
    for (int i = 0; i < m; i++) {
      for (int ii = 0; ii < n; ii++) {
        if (cellGrid[i][ii] != null && cellGrid[i][ii].lastCell != null//
            && Scalars.lessThan(farthest.cost, cellGrid[i][ii].cost)) {
          farthest = cellGrid[i][ii];
        }
      }
    }
    return farthest;
  }

  public boolean isClosed() {
    return closed;
  }

  boolean prepareCells(Tensor gridsize, int startx, int starty, double startorientation, Tensor currPos, boolean searchFromGokart) {
    m = gridsize.Get(0).number().intValue();
    n = gridsize.Get(1).number().intValue();
    cellGrid = new Cell[m][n];
    Comparator<Cell> comparator = new Comparator<TrackLayoutInitialGuess.Cell>() {
      @Override
      public int compare(Cell o1, Cell o2) {
        if (Scalars.lessThan(o1.cost, o2.cost))
          return -1;
        else if (o1.cost.equals(o2.cost))
          return 0;
        else
          return 1;
      }
    };
    double dirx = Math.cos(startorientation);
    double diry = Math.sin(startorientation);
    int sfx = (int) Math.round(startx + 2 * dirx);
    int sfy = (int) Math.round(starty + 2 * diry);
    Q = new PriorityQueue<>(comparator);
    // prepare grid
    for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
      for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
        Cell newCell = new Cell(i, ii);
        if ((i == sfx && ii == sfy) || !occupancyGrid.isCellOccupied(//
            newCell.x, newCell.y))
          cellGrid[i][ii] = newCell;
      }
    }
    // add limit at start
    addStartingLine(startx, starty, startorientation);
    // add to Q
    /* Q = new PriorityQueue<>(100000, comparator);
     * for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
     * for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
     * if (cellGrid[i][ii] != null) {
     * Q.add(cellGrid[i][ii]);
     * cellGrid[i][ii].inQ = true;
     * }
     * }
     * } */
    // gokart immediate target
    if (currPos != null)
      dijkstraGokartBack = cellGrid[currPos.Get(0).number().intValue()][currPos.Get(1).number().intValue()];
    // add start to Q
    if (!searchFromGokart)
      dijkstraStart = cellGrid[sfx][sfy];
    else
      dijkstraStart = dijkstraGokartBack;
    if (dijkstraStart == null)
      return false;
    dijkstraStart.cost = RealScalar.ZERO;
    dijkstraStart.inQ = true;
    Q.add(dijkstraStart);
    // add target
    dijkstraTarget = cellGrid[(int) Math.round(startx - 3 * dirx)][(int) Math.round(starty - 3 * diry)];
    // add neighbors
    /* for (int i = 0; i < gridsize.Get(0).number().intValue(); i++) {
     * System.out.println("row: "+i);
     * for (int ii = 0; ii < gridsize.Get(1).number().intValue(); ii++) {
     * if (cellGrid[i][ii] != null)
     * cellGrid[i][ii].findNeighbors();
     * }
     * } */
    return true;
  }

  void addStartingLine(int startx, int starty, double startorientation) {
    double xforward = Math.cos(startorientation);
    double yforward = Math.sin(startorientation);
    double xsideward = yforward;
    double ysideward = -xforward;
    // find right end
    double currentx = startx;
    double currenty = starty;
    while (currentx >= 0 && currentx < m && currenty >= 0 && currenty < n && cellGrid[(int) currentx][(int) currenty] != null) {
      currentx += xsideward;
      currenty += ysideward;
    }
    int rightx = (int) currentx;
    int righty = (int) currenty;
    // find left end
    currentx = startx;
    currenty = starty;
    while (currentx >= 0 && currentx < m && currenty >= 0 && currenty < n && cellGrid[(int) currentx][(int) currenty] != null) {
      currentx -= xsideward;
      currenty -= ysideward;
    }
    int leftx = (int) currentx;
    int lefty = (int) currenty;
    // delete all cells on line
    int steps = (int) (Math.sqrt(1.0 * (rightx - leftx) * (rightx - leftx) + 1.0 * (righty - lefty) * (righty - lefty)) + 1.0);
    for (int i = 0; i < steps; i++) {
      int posx = (int) Math.round(leftx + (rightx - leftx) * (1.0 * i / (steps - 1.0)));
      int posy = (int) Math.round(lefty + (righty - lefty) * (1.0 * i / (steps - 1.0)));
      if (posx > 0 && posx < m - 1 && posy > 0 && posy < n - 1) {
        // set the neightbors to zero
        cellGrid[posx - 1][posy - 1] = null;
        cellGrid[posx - 1][posy] = null;
        cellGrid[posx - 1][posy + 1] = null;
        cellGrid[posx][posy - 1] = null;
        cellGrid[posx][posy] = null;
        cellGrid[posx][posy + 1] = null;
        cellGrid[posx + 1][posy - 1] = null;
        cellGrid[posx + 1][posy] = null;
        cellGrid[posx + 1][posy + 1] = null;
      }
    }
  }

  boolean initialise(int x, int y, double orientation, Tensor currPos, boolean searchFromGokart) {
    Tensor gridSize = occupancyGrid.getGridSize();
    possibleNeighbors = new ArrayList<>();
    for (int dx = -2; dx <= 2; dx++)
      for (int dy = -2; dy <= 2; dy++)
        if (dx != 0 || dy != 0)
          possibleNeighbors.add(new Neighbor(dx, dy));
    return prepareCells(gridSize, x, y, orientation, currPos, searchFromGokart);
  }

  public List<Cell> getWayTo(Cell target) {
    LinkedList<Cell> solution = new LinkedList<>();
    solution.add(target);
    while (!solution.getFirst().equals(startingPoint)) {
      solution.addFirst(solution.getFirst().lastCell);
    }
    return solution;
  }

  public void processDijkstra() {
    while (!Q.isEmpty()) {
      Cell currentCell = Q.poll();
      currentCell.findNeighbors();
      currentCell.processed = true;
      currentCell.inQ = false;
      int nCount = 0;
      for (Cell n : currentCell.neighBors) {
        if (!n.processed) {
          // TODO maybe use neighborcost (not that important)
          Scalar alternativ = currentCell.cost.add(currentCell.neighBorCost.get(nCount));
          nCount++;
          if (Scalars.lessThan(alternativ, n.cost)) {
            // this could potentially be too slow
            Q.remove(n);
            n.cost = alternativ;
            n.lastCell = currentCell;
            n.inQ = true;
            Q.add(n);
          }
        }
      }
    }
  }

  public Tensor getPixelPosition(Tensor worldPosition) {
    Tensor transform = occupancyGrid.getTransform();
    // TODO MH try the following line:
    // Tensor wp = worldPosition.extract(0, 2).append(Quantity.of(1, SI.METER));
    Tensor wp = Tensors.empty();
    wp.append(worldPosition.Get(0));
    wp.append(worldPosition.Get(1));
    wp.append(Quantity.of(1, SI.METER));
    return LinearSolve.of(transform, wp);
  }

  public void update(int startX, int startY, double startorientation) {
    update(startX, startY, startorientation, null);
  }

  public void update(int startX, int startY, double startorientation, Tensor gokartPosition) {
    // position if map
    Tensor curPos = null;
    if (gokartPosition != null)
      curPos = getPixelPosition(gokartPosition);
    if (initialise(startX, startY, startorientation, curPos, false)) {
      processDijkstra();
      // check if we can reach target
      if (reachable(dijkstraTarget)) // we can reach target;
      {
        System.out.println("target found");
        closed = true;
        route = dijkstraTarget.getRoute();
      } else {
        System.out.println("target not found");
        closed = false;
        actualTarget = getFarthestCell();
        LinkedList<Cell> routeFromStart = actualTarget.getRoute();
        route = routeFromStart;
        // can we reach gokart?
        /* if (reachable(dijkstraGokartBack)) {
         * System.out.println("start->gokart found. Expanding beyond gokart");
         * route = routeFromStart;
         * } else {
         * // search from gokart toward
         * System.out.println("searching from gokart towards starting line");
         * boolean targetAvailable = initialise(startX, startY, startorientation, curPos, true);
         * if (targetAvailable)
         * processDijkstra();
         * if (targetAvailable && reachable(dijkstraTarget)) {
         * // found way from gokart
         * route = dijkstraTarget.getRoute();
         * route.addAll(routeFromStart);
         * } else {
         * System.out.println("no route found to gokart");
         * route = routeFromStart;
         * }
         * } */
      }
    } else {
      System.out.println("Target not available.");
    }
    routePolygon = null;
  }

  boolean reachable(Cell target) {
    return target != null && target.processed;
  }

  public Tensor getRoutePolygon() {
    if (routePolygon == null) {
      routePolygon = Tensors.empty();
      if (route != null) {
        Tensor grid2model = occupancyGrid.getTransform();
        for (Cell c : route) {
          routePolygon.append(grid2model.dot(Tensors.vector(c.x, c.y, 1)));
        }
      }
    }
    return routePolygon;
  }

  public Tensor getControlPointGuess(Scalar spacing, Scalar controlPointResolution) {
    Tensor wantedPositionsX = Tensors.empty();
    Tensor wantedPositionsY = Tensors.empty();
    Tensor lastPosition = route.getFirst().getPos();
    for (Cell c : route) {
      Tensor pos = c.getPos();
      Tensor dist = pos.subtract(lastPosition);
      if (Scalars.lessThan(spacing, Norm._2.of(dist))) {
        lastPosition = pos;
        wantedPositionsX.append(pos.Get(0));
        wantedPositionsY.append(pos.Get(1));
      }
    }
    if (wantedPositionsX.length() > 3) {
      wantedPositionsX.append(route.getLast().getPos().Get(0));
      wantedPositionsY.append(route.getLast().getPos().Get(1));
      wantedPositionsX = wantedPositionsX.multiply(Quantity.of(1, SI.METER));
      wantedPositionsY = wantedPositionsY.multiply(Quantity.of(1, SI.METER));
      // solve for bspline points
      // number of bspline query points
      int m = wantedPositionsX.length();
      // number of control points
      int n = (int) (wantedPositionsX.length() * controlPointResolution.number().doubleValue());
      // first possible value is 0
      // last possible value is n-2
      Tensor splineMatrix;
      if (closed) {
        // we found closed solution
        Tensor queryPositions = Tensors.vector((i) -> RealScalar.of((n + 0.0) * (i / (m + 0.0))), m);
        // query points
        splineMatrix = MPCBSpline.getBasisMatrix(n, queryPositions, 0, true);
      } else {
        Tensor queryPositions = Tensors.vector((i) -> RealScalar.of((n - 2.0) * (i / (m - 1.0))), m);
        // query points
        splineMatrix = MPCBSpline.getBasisMatrix(n, queryPositions, 0, false);
      }
      // solve for control points: x
      // TODO MH you are doing SVD of the same matrix twice !
      Tensor controlpointsX = LeastSquares.usingSvd(splineMatrix, wantedPositionsX);
      Tensor controlpointsY = LeastSquares.usingSvd(splineMatrix, wantedPositionsY);
      return Tensors.of(controlpointsX, controlpointsY);
    }
    System.out.println("no usable track!");
    return null;
  }

  @Override
  public void render(GeometricLayer geometricLayer, Graphics2D graphics) {
    Tensor routePolygon = getRoutePolygon();
    Path2D path2d = geometricLayer.toPath2D(routePolygon);
    graphics.draw(path2d);
    /* for (Tensor t : freeLines) {
     * path2d = geometricLayer.toPath2D(t);
     * graphics.draw(path2d);
     * } */
  }
}