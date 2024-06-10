import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.EquilateralTriangleImage;
import javalib.worldimages.FrameImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.OverlayOffsetImage;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.RotateImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

/*
 * EXTRA CREDIT:
 *
 * - Added a time counter
 * - Enhanced graphics with gradient implemented
 * - Added a score counter
 * - Added a restart functionality: Press 'r' to restart
 *
 */

//Utils interface
interface IUtils {

  // Color that represents background of all game pieces
  Color GP_COLOR = new Color(55, 56, 59); // dark gray

  // Color that represents an unlit game piece
  Color UNLIT_COLOR = new Color(98, 100, 105); // light gray

  // Color that represents a lit game piece
  Color LIT_COLOR = new Color(237, 223, 152); // yellow

  // size of the game piece
  int CELL_SIZE = 50;

  // a cell with no wires on it
  WorldImage GP_CELL = new FrameImage(
      new RectangleImage(IUtils.CELL_SIZE, IUtils.CELL_SIZE, OutlineMode.SOLID, IUtils.GP_COLOR),
      Color.BLACK);

  // represents a power station
  WorldImage POWER_STATION = new OverlayImage(
      new EquilateralTriangleImage(20, OutlineMode.SOLID, Color.CYAN),
      new RotateImage(new EquilateralTriangleImage(20, OutlineMode.SOLID, Color.CYAN), 180));
}

//Represents a game world of LightEmAll
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;

  // a list of all nodes
  ArrayList<GamePiece> nodes;

  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;

  // the width and height of the board
  int width; // columns
  int height; // rows

  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  boolean booleanFlag;
  Random rand;
  int tickRate;
  int timeElapsed;
  int score;

  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.rand = new Random();
    this.tickRate = 0;
    this.timeElapsed = 0;
    this.score = 0;

    makeBoard();
    makeNodes();
    kruskals(rand);
    mstApply();
    scrambleBoard(rand);
    continueOn();

  }

  // constructor for testing
  LightEmAll(int width, int height, boolean booleanFlag, Random rand) {
    this.width = width;
    this.height = height;
    this.nodes = new ArrayList<GamePiece>();
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.booleanFlag = booleanFlag;
    this.rand = rand;
    this.tickRate = 0;
    this.timeElapsed = 0;
    this.score = 0;

  }

  // makes the scene
  @Override
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.height * IUtils.CELL_SIZE,
        this.width * IUtils.CELL_SIZE);

    for (int i = 0; i < this.height; i++) {
      for (int j = 0; j < this.width; j++) {
        scene = this.board.get(i).get(j).drawGamePiece(scene, this);
      }
    }

    scene.placeImageXY(
        new TextImage("Time passed: " + String.valueOf(timeElapsed), 15, Color.white),
        (this.width * IUtils.CELL_SIZE) / 2, IUtils.CELL_SIZE);

    scene.placeImageXY(new TextImage("Score: " + String.valueOf(score), 15, Color.yellow),
        (this.width * IUtils.CELL_SIZE) / 2, IUtils.CELL_SIZE * 2);

    return scene;
  }

  // makes a board of empty pieces
  void makeBoard() {
    for (int i = 0; i < this.width; i++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int j = 0; j < this.height; j++) {
        this.board.get(i).add(new GamePiece(j, i));
      }
    }
  }

  // onTick event handler that updates time elapsed per tick
  @Override
  public void onTick() {

    this.tickRate++;

    if (this.tickRate % 45 == 0) {
      this.timeElapsed++;
    }
  }

  // establishes boolean values for wire directions based on edges on the board
  void mstApply() {
    for (Edge e : this.mst) {
      e.getFrom().connectTo(e.getTo());
    }

    this.board.get(this.powerRow).get(this.powerCol).updatePowerStation();
    this.board.get(this.powerRow).get(this.powerCol).power();
  }

  // creates the minimum spanning tree for the game
  void kruskals(Random rand) {
    HashMap<GamePiece, GamePiece> representatives = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = makeEdges(rand);

    worklist.sort(new EdgeComparator()); // all edges in graph, sorted by edge weights;

    for (int i = 0; i < nodes.size(); i++) {
      representatives.put(this.nodes.get(i), this.nodes.get(i));
    }

    while (edgesInTree.size() < this.nodes.size() - 1) {
      Edge nextEdge = worklist.remove(0);

      GamePiece from = nextEdge.getFrom();
      GamePiece to = nextEdge.getTo();

      if (find(representatives, from).sameGamePiece(find(representatives, to))) {
        // discard edge
      }

      else {
        edgesInTree.add(nextEdge);
        representatives = union(representatives, find(representatives, from),
            find(representatives, to));
      }
    }

    this.mst = edgesInTree;
  }

  // helper method that determines the key of a key value pair in a hash map
  public GamePiece find(HashMap<GamePiece, GamePiece> hash, GamePiece gp) {
    while (hash.get(gp) != gp) {
      gp = hash.get(gp);
    }
    return gp;
  }

  // helper method that applies the parent gamepiece from one gamepiece to another
  public HashMap<GamePiece, GamePiece> union(HashMap<GamePiece, GamePiece> representatives,
      GamePiece find1, GamePiece find2) {
    representatives.put(find2, find1);

    return representatives;
  }

  // creates the scrambled board
  void scrambleBoard(Random rand) {
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        int rotations = rand.nextInt(4);

        for (int k = 0; k < rotations; k++) {
          this.board.get(i).get(j).rotate();
        }
      }
    }
  }

  // rotates the game pieces on left click
  @Override
  public void onMouseClicked(Posn posn, String key) {
    if (key.equals("LeftButton")) {
      this.board.get((int) Math.floor(posn.x / IUtils.CELL_SIZE))
      .get((int) Math.floor(posn.y / IUtils.CELL_SIZE)).rotate();

      this.score += 1;

      continueOn();
    }

    if (checkWin()) {
      this.endOfWorld("you won!");
    }

  }

  // creates the last scene of the game, for the winning case
  @Override
  public WorldScene lastScene(String msg) {
    WorldScene scene = new WorldScene(this.width * IUtils.CELL_SIZE,
        this.height * IUtils.CELL_SIZE);

    WorldImage text = new OverlayImage(new TextImage(msg, IUtils.CELL_SIZE / 2, Color.BLACK),
        new RectangleImage(IUtils.CELL_SIZE * 5, IUtils.CELL_SIZE / 2, OutlineMode.SOLID,
            Color.WHITE));

    for (int i = 0; i < this.height; i++) {
      for (int j = 0; j < this.width; j++) {
        scene = this.board.get(i).get(j).drawGamePiece(scene, this);
      }
    }

    scene.placeImageXY(text, scene.height / 2, scene.width / 2);

    return scene;

  }

  // creates the nodes
  void makeNodes() {
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        this.nodes.add(this.board.get(i).get(j));
      }
    }
  }

  // creates the edges on the board with random weights
  public ArrayList<Edge> makeEdges(Random rand) {
    ArrayList<Edge> edges = new ArrayList<Edge>();

    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {

        if (j != this.height - 1) {
          edges.add(
              new Edge(this.board.get(i).get(j), this.board.get(i).get(j + 1), rand.nextInt(25)));
        }

        if (i != this.width - 1) {
          edges.add(
              new Edge(this.board.get(i).get(j), this.board.get(i + 1).get(j), rand.nextInt(25)));
        }
      }
    }
    return edges;
  }

  // checks if the current game state is "won"
  public boolean checkWin() {
    boolean win = true;

    for (GamePiece g : this.nodes) {
      if (!g.won()) {
        win = false;
      }
    }
    return win;
  }

  // updates the power status of all GamePieces
  void continueOn() {

    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>(
        Arrays.asList(this.board.get(powerCol).get(powerRow)));

    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        if (i != this.powerCol || j != this.powerRow) {
          this.board.get(i).get(j).unpower();
        }
        // unpower everything except the power station because what if it's no longer
        // connected
      }
    }
    while (worklist.size() > 0) {
      GamePiece next = worklist.remove(0);

      worklist.addAll(next.lightUp(this.board, this.width, this.height));
    }
  }

  // moves the power station based on the arrow keys
  @Override
  public void onKeyEvent(String key) {
    this.board.get(this.powerCol).get(this.powerRow).updatePowerStation();

    if (this.powerRow > 0 && key.equals("up")
        && board.get(this.powerCol).get(this.powerRow).hasTop()
        && board.get(this.powerCol).get(this.powerRow - 1).won()) {
      this.powerRow = this.powerRow - 1;
    }

    if (this.powerRow != this.height - 1 && key.equals("down")
        && board.get(this.powerCol).get(this.powerRow).hasBottom()
        && board.get(this.powerCol).get(this.powerRow + 1).won()) {
      this.powerRow = this.powerRow + 1;
    }

    if (this.powerCol > 0 && key.equals("left")
        && board.get(this.powerCol).get(this.powerRow).hasLeft()
        && board.get(this.powerCol - 1).get(this.powerRow).won()) {
      this.powerCol = this.powerCol - 1;
    }

    if (this.powerCol != this.width - 1 && key.equals("right")
        && board.get(this.powerCol).get(this.powerRow).hasRight()
        && board.get(this.powerCol + 1).get(this.powerRow).won()) {
      this.powerCol = this.powerCol + 1;
    }
    this.board.get(this.powerCol).get(this.powerRow).updatePowerStation();

    if (key.equals("r")) {

      initializeGame();

    }

  }

  // initializes a new Game board
  void initializeGame() {
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.radius = 0;
    this.score = 0;
    this.timeElapsed = 0;
    makeBoard();
    makeNodes();
    kruskals(rand);
    mstApply();
    scrambleBoard(rand);
    continueOn();
  }

}

//class that represents a GamePiece
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;
  }

  // constructor for testing
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }

  // constructor for edges
  GamePiece(int row, int col) {
    this.row = row;
    this.col = col;
    this.left = false;
    this.right = false;
    this.top = false;
    this.bottom = false;
    this.powerStation = false;
    this.powered = false;
  }

  // setter permitted by John Park Piazza Post, powers a gamepiece
  public void power() {
    this.powered = true;
  }

  // setter permitted by John Park Piazza Post, unpowers a gamepiece
  public void unpower() {
    this.powered = false;
  }

  // determines if two game pieces are the same piece
  public boolean sameGamePiece(GamePiece that) {
    return (this.row == that.row) && (this.col == that.col);
  }

  // determines the boolean values of a gamepiece based on the row/col position of
  // it and the piece it is connected to
  public void connectTo(GamePiece toNode) {
    if (this.row == toNode.row - 1) {
      this.bottom = true;
      toNode.top = true;
    }
    if (this.row - 1 == toNode.row) {
      this.top = true;
      toNode.bottom = true;
    }
    if (this.col == toNode.col - 1) {
      this.right = true;
      toNode.left = true;
    }
    if (this.col - 1 == toNode.col) {
      this.left = true;
      toNode.right = true;
    }
  }

  // changes the top, bottom, left, right fields according to a rotation
  void rotate() {
    boolean newTop = false;
    boolean newBot = false;
    boolean newLeft = false;
    boolean newRight = false;

    if (top) {
      newRight = true;
    }
    if (right) {
      newBot = true;
    }
    if (bottom) {
      newLeft = true;
    }
    if (left) {
      newTop = true;
    }

    this.left = newLeft;
    this.right = newRight;
    this.top = newTop;
    this.bottom = newBot;
  }

  // draws the gamePiece on the WorldScene and puts the gradient effect
  public WorldScene drawGamePiece(WorldScene w, LightEmAll game) {
    Color color = IUtils.UNLIT_COLOR;

    if (powered) {
      int poweredRow = -1;

      int poweredCol = -1;

      for (int i = 0; i < game.height; i++) {
        for (int j = 0; j < game.width; j++) {
          if (game.board.get(j).get(i).powerStation) {
            poweredRow = i;
            poweredCol = j;
            break;
          }
        }
      }

      int distance = Math.abs(row - poweredRow) + Math.abs(col - poweredCol);

      int shade = 255 - (distance * 30);
      shade = Math.max(shade, 0);
      shade = Math.min(shade, 255);
      color = new Color(shade, shade, 12);
    }

    WorldImage cell = new OverlayImage(
        new RectangleImage(IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 4, OutlineMode.SOLID, color),
        IUtils.GP_CELL);

    if (top) {
      cell = new OverlayOffsetImage(
          new RectangleImage(IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID, color),
          0, IUtils.CELL_SIZE / 4, cell);
    }

    if (bottom) {

      cell = new OverlayOffsetImage(
          new RectangleImage(IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID, color),
          0, IUtils.CELL_SIZE / -4, cell);
    }

    if (left) {
      cell = new OverlayOffsetImage(
          new RectangleImage(IUtils.CELL_SIZE / 2, IUtils.CELL_SIZE / 4, OutlineMode.SOLID, color),
          IUtils.CELL_SIZE / 4, 0, cell);
    }

    if (right) {
      cell = new OverlayOffsetImage(
          new RectangleImage(IUtils.CELL_SIZE / 2, IUtils.CELL_SIZE / 4, OutlineMode.SOLID, color),
          IUtils.CELL_SIZE / -4, 0, cell);
    }

    if (powerStation) {
      cell = new OverlayImage(IUtils.POWER_STATION, cell);
    }

    w.placeImageXY(cell, this.col * IUtils.CELL_SIZE + IUtils.CELL_SIZE / 2,
        this.row * IUtils.CELL_SIZE + IUtils.CELL_SIZE / 2);

    return w;
  }

  // observational getter, as instructed by TA, determines if cell is powered
  public boolean won() {
    return this.powered;
  }

  // getter permitted by John Park Piazza Post, determines if cell has a top exit
  public boolean hasTop() {
    return this.top;
  }

  // getter permitted by John Park Piazza Post, determines if cell has a bottom
  // exit
  public boolean hasBottom() {
    return this.bottom;
  }

  // getter permitted by John Park Piazza Post, determines if cell has a left exit
  public boolean hasLeft() {
    return this.left;
  }

  // getter permitted by John Park Piazza Post, determines if cell has a right
  // exit
  public boolean hasRight() {
    return this.right;
  }

  // setter permitted by John Park Piazza Post, changes the state of if a
  // gamepiece contains the power station
  public void updatePowerStation() {
    this.powerStation = !this.powerStation;
  }

  // checks if "power" can be transferred to an adjacent GamePiece
  boolean continuePath(String dir) {
    boolean returnValue = false;
    if (dir.equals("top") && this.bottom && !this.powered) {
      returnValue = true;
      this.powered = true;
    }
    else if (dir.equals("bottom") && this.top && !this.powered) {
      returnValue = true;
      this.powered = true;
    }
    else if (dir.equals("left") && this.right && !this.powered) {
      returnValue = true;
      this.powered = true;
    }
    else if (dir.equals("right") && this.left && !this.powered) {
      returnValue = true;
      this.powered = true;
    }

    return returnValue;

  }

  // lights up the connecting pieces from a currently lit up game piece
  public ArrayList<GamePiece> lightUp(ArrayList<ArrayList<GamePiece>> board, int width,
      int height) {
    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();

    if (this.row > 0 && this.top) {
      if (board.get(this.col).get(this.row - 1).continuePath("top")) {
        board.get(this.col).get(this.row - 1).powered = true;

        worklist.add(board.get(this.col).get(this.row - 1));
      }
    }
    if (this.row != width - 1 && this.bottom) {
      if (board.get(this.col).get(this.row + 1).continuePath("bottom")) {
        board.get(this.col).get(this.row + 1).powered = true;

        worklist.add(board.get(this.col).get(this.row + 1));
      }
    }

    if (this.col > 0 && this.left) {
      if (board.get(this.col - 1).get(this.row).continuePath("left")) {

        board.get(this.col - 1).get(this.row).powered = true;

        worklist.add(board.get(this.col - 1).get(this.row));
      }
    }

    if (this.col != height - 1 && this.right) {
      if (board.get(this.col + 1).get(this.row).continuePath("right")) {
        board.get(this.col + 1).get(this.row).powered = true;

        worklist.add(board.get(this.col + 1).get(this.row));
      }
    }
    return worklist;
  }

}

// class that represents an edge
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }

  // permitted getter that accesses the fromNode() field
  public GamePiece getFrom() {
    return this.fromNode;
  }

  // permitted getter that accesses the toNode() field
  public GamePiece getTo() {
    return this.toNode;
  }
}

// class that compares two edges by weight
class EdgeComparator implements Comparator<Edge> {

  @Override
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

// class for testing lightEmAll games
class ExamplesLightEmAll { // LEFT RIGHT TOP BOT

  // LightEmAll test = new LightEmAll(3, 3);

  GamePiece gpA = new GamePiece(0, 0, false, false, false, true, false);
  GamePiece gpB = new GamePiece(0, 1, true, true, false, false, false);
  GamePiece gpC = new GamePiece(0, 2, true, true, true, true, false);

  GamePiece gpA2 = new GamePiece(0, 0, false, false, false, true, false);
  GamePiece gpB2 = new GamePiece(0, 1, true, true, false, false, false);
  GamePiece gpC2 = new GamePiece(0, 2, true, true, true, true, false);

  GamePiece gpD = new GamePiece(1, 0, false, false, true, true, false);
  GamePiece gpE = new GamePiece(1, 1, true, true, true, true, false);
  GamePiece gpF = new GamePiece(1, 2, true, true, false, false, false);

  GamePiece gpAEmpty = new GamePiece(0, 0);
  GamePiece gpBEmpty = new GamePiece(0, 1);
  GamePiece gpCEmpty = new GamePiece(0, 2);

  GamePiece gpDEmpty = new GamePiece(1, 0);
  GamePiece gpEEmpty = new GamePiece(1, 1);
  GamePiece gpFEmpty = new GamePiece(1, 2);

  LightEmAll l = new LightEmAll(3, 3, true, new Random(5));
  LightEmAll l2 = new LightEmAll(3, 3, true, new Random(3));

  Edge testEdge = new Edge(this.gpA, this.gpD, 10);
  Edge testEdge2 = new Edge(this.gpB, this.gpE, 15);

  GamePiece gp1 = new GamePiece(0, 0, false, false, false, true, false); // BR
  GamePiece gp2 = new GamePiece(0, 1, false, false, false, true, false); // BLR
  GamePiece gp3 = new GamePiece(0, 2, false, false, false, true, false); // BL
  GamePiece gp4 = new GamePiece(1, 0, false, true, true, true, false); // TBR
  GamePiece gp5 = new GamePiece(1, 1, true, true, true, true, true, true); // TBLR
  GamePiece gp6 = new GamePiece(1, 2, true, false, true, true, false); // TBL
  GamePiece gp7 = new GamePiece(2, 0, false, false, true, false, false); // TR
  GamePiece gp8 = new GamePiece(2, 1, false, false, true, false, false); // TLR
  GamePiece gp9 = new GamePiece(2, 2, false, false, true, false, false); // TL

  GamePiece gpInit1 = new GamePiece(0, 0, false, false, false, false, false); // BR
  GamePiece gpInit2 = new GamePiece(0, 1, false, false, false, false, false); // BR
  GamePiece gpInit3 = new GamePiece(0, 2, false, false, false, false, false); // BR
  GamePiece gpInit4 = new GamePiece(1, 0, false, false, false, false, false); // BR
  GamePiece gpInit5 = new GamePiece(1, 1, false, false, false, false, false); // BR
  GamePiece gpInit6 = new GamePiece(1, 2, false, false, false, false, false); // BR
  GamePiece gpInit7 = new GamePiece(2, 0, false, false, false, false, false); // BR
  GamePiece gpInit8 = new GamePiece(2, 1, false, false, false, false, false); // BR
  GamePiece gpInit9 = new GamePiece(2, 2, false, false, false, false, false); // BR

  Edge edge1 = new Edge(this.gpInit6, this.gpInit9, 3);
  Edge edge2 = new Edge(this.gpInit2, this.gpInit3, 4);
  Edge edge3 = new Edge(this.gpInit2, this.gpInit5, 5);
  Edge edge4 = new Edge(this.gpInit7, this.gpInit8, 6);
  Edge edge5 = new Edge(this.gpInit3, this.gpInit6, 6);
  Edge edge6 = new Edge(this.gpInit1, this.gpInit4, 12);
  Edge edge7 = new Edge(this.gpInit5, this.gpInit8, 16);
  Edge edge8 = new Edge(this.gpInit1, this.gpInit2, 17);

  void reset() {
    this.gpA = new GamePiece(0, 0, false, false, false, true, false);
    this.gpB = new GamePiece(0, 1, false, false, true, true, false);
    this.gpC = new GamePiece(0, 2, true, true, true, true, false);

    this.gpA2 = new GamePiece(0, 0, false, false, false, true, false);
    this.gpB2 = new GamePiece(0, 1, true, true, false, false, false);
    this.gpC2 = new GamePiece(0, 2, true, true, true, true, false);

    this.gp1 = new GamePiece(0, 0, false, false, false, true, false); // BR
    this.gp2 = new GamePiece(0, 1, false, false, false, true, false); // BLR
    this.gp3 = new GamePiece(0, 2, false, false, false, true, false); // BL
    this.gp4 = new GamePiece(1, 0, false, true, true, true, false); // TBR
    this.gp5 = new GamePiece(1, 1, true, true, true, true, true, true); // TBLR
    this.gp6 = new GamePiece(1, 2, true, false, true, true, false); // TBL
    this.gp7 = new GamePiece(2, 0, false, false, true, false, false); // TR
    this.gp8 = new GamePiece(2, 1, false, false, true, false, false); // TLR
    this.gp9 = new GamePiece(2, 2, false, false, true, false, false); // TL

    this.edge1 = new Edge(this.gpInit6, this.gpInit9, 3);
    this.edge2 = new Edge(this.gpInit2, this.gpInit3, 4);
    this.edge3 = new Edge(this.gpInit2, this.gpInit5, 5);
    this.edge4 = new Edge(this.gpInit7, this.gpInit8, 6);
    this.edge5 = new Edge(this.gpInit3, this.gpInit6, 6);
    this.edge6 = new Edge(this.gpInit1, this.gpInit4, 12);
    this.edge7 = new Edge(this.gpInit5, this.gpInit8, 16);
    this.edge8 = new Edge(this.gpInit1, this.gpInit2, 17);

  }

  // tests all GamePiece methods, building up to a completely constructed cell
  void testGamePiece(Tester t) {
    this.reset();

    // test power method
    this.gpA.power();
    this.gpB.power();

    t.checkExpect(this.gpA.powered, true);
    t.checkExpect(this.gpB.powered, true);

    // test unpower method
    this.gpA.unpower();
    this.gpB.unpower();

    t.checkExpect(this.gpA.powered, false);
    t.checkExpect(this.gpB.powered, false);

    this.reset();

    // test rotate method
    this.gpA.rotate();
    this.gpB.rotate();
    this.gpC.rotate();

    t.checkExpect(this.gpA.left, true);
    t.checkExpect(this.gpA.right, false);
    t.checkExpect(this.gpA.top, false);
    t.checkExpect(this.gpA.bottom, false);

    t.checkExpect(this.gpB.left, true);
    t.checkExpect(this.gpB.right, true);
    t.checkExpect(this.gpB.top, false);
    t.checkExpect(this.gpB.bottom, false);

    t.checkExpect(this.gpC.left, true);
    t.checkExpect(this.gpC.right, true);
    t.checkExpect(this.gpC.top, true);
    t.checkExpect(this.gpC.bottom, true);

    WorldScene w = new WorldScene(50, 50);

    this.reset();

    w.placeImageXY(new OverlayOffsetImage(
        new RectangleImage(IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID,
            IUtils.UNLIT_COLOR),
        0, IUtils.CELL_SIZE / -4, new OverlayImage(new RectangleImage(IUtils.CELL_SIZE / 4,
            IUtils.CELL_SIZE / 4, OutlineMode.SOLID, IUtils.UNLIT_COLOR), IUtils.GP_CELL)),
        25, 25);

    this.reset();

    WorldScene w2 = new WorldScene(50, 50);

    w2.placeImageXY(new OverlayOffsetImage(
        new RectangleImage(
            IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
        0, IUtils.CELL_SIZE / -4,
        (new OverlayOffsetImage(
            new RectangleImage(IUtils.CELL_SIZE / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID,
                IUtils.UNLIT_COLOR),
            0, IUtils.CELL_SIZE / 4, new OverlayImage(new RectangleImage(IUtils.CELL_SIZE / 4,
                IUtils.CELL_SIZE / 4, OutlineMode.SOLID, IUtils.UNLIT_COLOR), IUtils.GP_CELL)))),
        75, 25);

    WorldScene w3 = new WorldScene(50, 50);

    w3.placeImageXY(
        new OverlayOffsetImage(
            new RectangleImage(
                IUtils.CELL_SIZE / 2, IUtils.CELL_SIZE / 4, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
            IUtils.CELL_SIZE / -4, 0,
            (new OverlayOffsetImage(
                new RectangleImage(IUtils.CELL_SIZE
                    / 2, IUtils.CELL_SIZE / 4, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
                IUtils.CELL_SIZE / 4, 0,
                (new OverlayOffsetImage(
                    new RectangleImage(IUtils.CELL_SIZE
                        / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
                    0, IUtils.CELL_SIZE / -4,
                    (new OverlayOffsetImage(
                        new RectangleImage(IUtils.CELL_SIZE
                            / 4, IUtils.CELL_SIZE / 2, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
                        0, IUtils.CELL_SIZE / 4,
                        new OverlayImage(new RectangleImage(IUtils.CELL_SIZE / 4,
                            IUtils.CELL_SIZE / 4, OutlineMode.SOLID, IUtils.UNLIT_COLOR),
                            IUtils.GP_CELL)))))))),
        125, 25);

    // test drawGamePiece method
    t.checkExpect(gpA.drawGamePiece(new WorldScene(IUtils.CELL_SIZE, IUtils.CELL_SIZE), l), w);
    t.checkExpect(gpB.drawGamePiece(new WorldScene(IUtils.CELL_SIZE, IUtils.CELL_SIZE), l), w2);
    t.checkExpect(gpC.drawGamePiece(new WorldScene(IUtils.CELL_SIZE, IUtils.CELL_SIZE), l), w3);

    this.reset();

    this.gpC.power();

    // test won method
    t.checkExpect(this.gpA.won(), false);
    t.checkExpect(this.gpB.won(), false);
    t.checkExpect(this.gpC.won(), true);

    // test hasTop method
    t.checkExpect(this.gpA.hasTop(), false);
    t.checkExpect(this.gpB.hasTop(), true);
    t.checkExpect(this.gpC.hasTop(), true);

    // test hasBottom method
    t.checkExpect(this.gpA.hasBottom(), true);
    t.checkExpect(this.gpB.hasBottom(), true);
    t.checkExpect(this.gpC.hasBottom(), true);

    // test hasLeft method
    t.checkExpect(this.gpA.hasLeft(), false);
    t.checkExpect(this.gpB.hasLeft(), false);
    t.checkExpect(this.gpC.hasLeft(), true);

    // test hasRight method
    t.checkExpect(this.gpA.hasRight(), false);
    t.checkExpect(this.gpB.hasRight(), false);
    t.checkExpect(this.gpC.hasRight(), true);

    // test updatePowerStation method
    this.gpA.updatePowerStation();
    this.gpB.updatePowerStation();
    this.gpC.updatePowerStation();
    this.gpC.updatePowerStation();

    t.checkExpect(this.gpA.powerStation, true);
    t.checkExpect(this.gpB.powerStation, true);
    t.checkExpect(this.gpC.powerStation, false);

    this.reset();

    ArrayList<GamePiece> row1 = new ArrayList<GamePiece>(
        Arrays.asList(this.gpA, this.gpB, this.gpC));
    ArrayList<GamePiece> row2 = new ArrayList<GamePiece>(
        Arrays.asList(this.gpD, this.gpE, this.gpF));
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>(
        Arrays.asList(row1, row2));

    // test continuePath method
    t.checkExpect(this.gpA2.continuePath("top"), true);
    t.checkExpect(this.gpB2.continuePath("top"), false);
    t.checkExpect(this.gpC2.continuePath("top"), true);
    t.checkExpect(this.gpA2.powered, true);
    t.checkExpect(this.gpB2.powered, false);
    t.checkExpect(this.gpC2.powered, true);

    // test lightUp method
    t.checkExpect(this.gpD.lightUp(board, 2, 1), new ArrayList<GamePiece>(Arrays.asList(this.gpA)));
    t.checkExpect(this.gpB.lightUp(board, 2, 1), new ArrayList<GamePiece>(Arrays.asList(this.gpE)));
    t.checkExpect(this.gpA.powered, true);
    t.checkExpect(this.gpE.powered, true);

    // test sameGamePiece method
    t.checkExpect(this.gpA.sameGamePiece(this.gpA), true);
    t.checkExpect(this.gpA.sameGamePiece(this.gpB), false);

    // test connectTo method
    this.gpAEmpty.connectTo(this.gpDEmpty);
    this.gpBEmpty.connectTo(this.gpEEmpty);
    this.gpEEmpty.connectTo(this.gpFEmpty);

    t.checkExpect(this.gpAEmpty, new GamePiece(0, 0, false, false, false, true, false));
    t.checkExpect(this.gpBEmpty, new GamePiece(0, 1, false, false, false, true, false));
    t.checkExpect(this.gpDEmpty, new GamePiece(1, 0, false, false, true, false, false));
    t.checkExpect(this.gpEEmpty, new GamePiece(1, 1, false, true, true, false, false));
    t.checkExpect(this.gpFEmpty, new GamePiece(1, 2, true, false, false, false, false));

  }

  // testing all methods in Edge
  void testEdge(Tester t) {
    this.reset();

    // test getFrom method
    t.checkExpect(testEdge.getFrom(), this.gpA);
    // test getTo method
    t.checkExpect(testEdge.getTo(), this.gpD);

  }

  // test the EdgeComparator class method
  void testEdgeComparator(Tester t) {

    this.reset();

    t.checkExpect(new EdgeComparator().compare(this.testEdge, this.testEdge), 0);
    t.checkExpect(new EdgeComparator().compare(this.testEdge, this.testEdge2), -5);
  }

  // testing all methods in LightEmAll
  void testLightEmAll(Tester t) {
    this.reset();

    // test the onTick method
    t.checkExpect(l.timeElapsed, 0);

    for (int i = 0; i < 51; i++) {
      l.onTick();
    }

    t.checkExpect(l.tickRate, 51);
    t.checkExpect(l.timeElapsed, 1);

    t.checkExpect(l.board, new ArrayList<ArrayList<GamePiece>>());

    // test makeBoard method
    this.l.makeBoard();

    t.checkExpect(this.l.board,
        new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit1, this.gpInit4, this.gpInit7)),
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit2, this.gpInit5, this.gpInit8)),
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit3, this.gpInit6, this.gpInit9)))));

    // test makeNodes method
    this.l.makeNodes();

    t.checkExpect(this.l.nodes,
        new ArrayList<GamePiece>(Arrays.asList(this.gpInit1, this.gpInit4, this.gpInit7,
            this.gpInit2, this.gpInit5, this.gpInit8, this.gpInit3, this.gpInit6, this.gpInit9)));

    // test makeEdges method

    this.l2.makeBoard();
    this.l2.makeNodes();

    t.checkExpect(this.l2.makeEdges(this.l2.rand),
        new ArrayList<Edge>(
            Arrays.asList(new Edge(this.l2.board.get(0).get(0), this.l2.board.get(0).get(1), 9),
                new Edge(this.l2.board.get(0).get(0), this.l2.board.get(1).get(0), 10),
                new Edge(this.l2.board.get(0).get(1), this.l2.board.get(0).get(2), 10),
                new Edge(this.l2.board.get(0).get(1), this.l2.board.get(1).get(1), 6),
                new Edge(this.l2.board.get(0).get(2), this.l2.board.get(1).get(2), 3),
                new Edge(this.l2.board.get(1).get(0), this.l2.board.get(1).get(1), 2),
                new Edge(this.l2.board.get(1).get(0), this.l2.board.get(2).get(0), 24),
                new Edge(this.l2.board.get(1).get(1), this.l2.board.get(1).get(2), 14),
                new Edge(this.l2.board.get(1).get(1), this.l2.board.get(2).get(1), 9),
                new Edge(this.l2.board.get(1).get(2), this.l2.board.get(2).get(2), 11),
                new Edge(this.l2.board.get(2).get(0), this.l2.board.get(2).get(1), 10),
                new Edge(this.l2.board.get(2).get(1), this.l2.board.get(2).get(2), 7))));

    // test find method

    HashMap<GamePiece, GamePiece> hashTest = new HashMap<GamePiece, GamePiece>();

    hashTest.put(this.gpA, this.gpB);
    hashTest.put(this.gpB, this.gpB);
    hashTest.put(this.gpC, this.gpD);
    hashTest.put(this.gpD, this.gpD);
    hashTest.put(this.gpE, this.gpE);

    t.checkExpect(this.l2.find(hashTest, this.gpA), gpB);
    t.checkExpect(this.l2.find(hashTest, this.gpB), gpB);
    t.checkExpect(this.l2.find(hashTest, this.gpC), gpD);

    // test union method
    this.l2.union(hashTest, this.gpA, this.gpE);
    this.l2.union(hashTest, this.gpD, this.gpB);

    t.checkExpect(this.l2.find(hashTest, this.gpB), gpD);
    t.checkExpect(this.l2.find(hashTest, this.gpE), gpD);

    // test kruskals method
    this.l.kruskals(this.l.rand);
    t.checkExpect(this.l.mst, new ArrayList<Edge>(Arrays.asList(this.edge1, this.edge2, this.edge3,
        this.edge4, this.edge5, this.edge6, this.edge7, this.edge8)));

    this.l.mstApply();

    this.gpInit1.right = true;
    this.gpInit1.bottom = true;
    this.gpInit1.powerStation = true;
    this.gpInit1.powered = true;

    this.gpInit4.top = true;
    this.gpInit7.right = true;

    this.gpInit2.left = true;
    this.gpInit2.right = true;
    this.gpInit2.bottom = true;

    this.gpInit5.top = true;
    this.gpInit5.bottom = true;

    this.gpInit8.left = true;
    this.gpInit8.top = true;

    this.gpInit3.left = true;
    this.gpInit3.bottom = true;

    this.gpInit6.top = true;
    this.gpInit6.bottom = true;

    this.gpInit9.top = true;

    t.checkExpect(this.l.board,
        new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit1, this.gpInit4, this.gpInit7)),
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit2, this.gpInit5, this.gpInit8)),
            new ArrayList<GamePiece>(Arrays.asList(this.gpInit3, this.gpInit6, this.gpInit9)))));

    // test scrambleBoard method
    this.l.scrambleBoard(this.l.rand);

    t.checkExpect(this.l.board.get(0).get(0),
        new GamePiece(0, 0, true, false, false, true, true, true));
    t.checkExpect(this.l.board.get(0).get(1),
        new GamePiece(1, 0, false, true, false, false, false));
    t.checkExpect(this.l.board.get(0).get(2),
        new GamePiece(2, 0, false, false, true, false, false));
    t.checkExpect(this.l.board.get(1).get(0), new GamePiece(0, 1, false, true, true, true, false));
    t.checkExpect(this.l.board.get(1).get(1), new GamePiece(1, 1, false, false, true, true, false));
    t.checkExpect(this.l.board.get(1).get(2), new GamePiece(2, 1, false, true, true, false, false));
    t.checkExpect(this.l.board.get(2).get(0), new GamePiece(0, 2, false, true, false, true, false));
    t.checkExpect(this.l.board.get(2).get(1), new GamePiece(1, 2, false, false, true, true, false));
    t.checkExpect(this.l.board.get(2).get(2),
        new GamePiece(2, 2, true, false, false, false, false));

    // testing onMouseClicked()

    this.l.onMouseClicked(new Posn(0, 0), "LeftButton");
    this.l.onMouseClicked(new Posn(0, 0), "LeftButton");
    this.l.onMouseClicked(new Posn(50, 0), "LeftButton");
    this.l.onMouseClicked(new Posn(100, 100), "LeftButton");

    t.checkExpect(this.l.board.get(0).get(0),
        new GamePiece(0, 0, false, true, true, false, true, true));
    t.checkExpect(this.l.board.get(1).get(0),
        new GamePiece(0, 1, true, true, false, true, false, true));
    t.checkExpect(this.l.board.get(2).get(2),
        new GamePiece(2, 2, false, false, true, false, false));

    // testing onKeyEvent()

    t.checkExpect(this.l.powerRow, 0);
    t.checkExpect(this.l.powerCol, 0);

    this.l.onKeyEvent("right");

    t.checkExpect(this.l.powerRow, 0);
    t.checkExpect(this.l.powerCol, 1);

    this.l.onKeyEvent("down");
    t.checkExpect(this.l.powerRow, 1);
    t.checkExpect(this.l.powerCol, 1);

    this.l.onKeyEvent("left");
    t.checkExpect(this.l.powerRow, 1);
    t.checkExpect(this.l.powerCol, 1);

    this.l.onKeyEvent("down");
    t.checkExpect(this.l.powerRow, 2);
    t.checkExpect(this.l.powerCol, 1);

    this.gpInit1 = new GamePiece(0, 0, false, true, true, false, true, true);
    this.gpInit4 = new GamePiece(1, 0, false, true, false, false, false);
    this.gpInit7 = new GamePiece(2, 0, false, false, true, false, false);
    this.gpInit2 = new GamePiece(0, 1, true, true, false, true, false);
    this.gpInit5 = new GamePiece(1, 1, false, false, true, true, false);
    this.gpInit8 = new GamePiece(2, 1, false, true, true, false, false);
    this.gpInit3 = new GamePiece(0, 2, false, true, false, true, false);
    this.gpInit6 = new GamePiece(1, 2, false, false, true, true, false);
    this.gpInit9 = new GamePiece(2, 2, true, false, false, false, false);

    t.checkExpect(this.gpInit1.powered, true);

    // test makeScene Method
    WorldScene scene = new WorldScene(IUtils.CELL_SIZE * 3, IUtils.CELL_SIZE * 3);

    this.l.board.get(0).get(0).drawGamePiece(scene, l);
    this.l.board.get(0).get(1).drawGamePiece(scene, l);
    this.l.board.get(0).get(2).drawGamePiece(scene, l);
    this.l.board.get(1).get(0).drawGamePiece(scene, l);
    this.l.board.get(1).get(1).drawGamePiece(scene, l);
    this.l.board.get(1).get(2).drawGamePiece(scene, l);
    this.l.board.get(2).get(0).drawGamePiece(scene, l);
    this.l.board.get(2).get(1).drawGamePiece(scene, l);
    this.l.board.get(2).get(2).drawGamePiece(scene, l);

    scene.placeImageXY(new TextImage("Time passed: 1", 15, Color.white), 75, 50);

    scene.placeImageXY(new TextImage("Score: 4", 15, Color.yellow), 75, 100);

    t.checkExpect(this.l.makeScene(), scene);

    this.l.onMouseClicked(new Posn(100, 100), "LeftButton");
    this.l.onMouseClicked(new Posn(100, 100), "LeftButton");
    this.l.onMouseClicked(new Posn(100, 100), "LeftButton");

    // testing continueOn() method

    this.l.continueOn();

    t.checkExpect(this.l.board.get(0).get(0).powered, true);
    t.checkExpect(this.l.board.get(1).get(0).powered, true);
    t.checkExpect(this.l.board.get(2).get(0).powered, false);
    t.checkExpect(this.l.board.get(0).get(1).powered, false);
    t.checkExpect(this.l.board.get(1).get(1).powered, true);
    t.checkExpect(this.l.board.get(2).get(1).powered, false);
    t.checkExpect(this.l.board.get(0).get(2).powered, false);
    t.checkExpect(this.l.board.get(1).get(2).powered, true);
    t.checkExpect(this.l.board.get(2).get(2).powered, true);

    // testing checkWin() method

    t.checkExpect(this.l.checkWin(), false);

    this.l.board.get(0).get(0).power();
    this.l.board.get(1).get(0).power();
    this.l.board.get(2).get(0).power();
    this.l.board.get(0).get(1).power();
    this.l.board.get(1).get(1).power();
    this.l.board.get(2).get(1).power();
    this.l.board.get(0).get(2).power();
    this.l.board.get(1).get(2).power();
    this.l.board.get(2).get(2).power();

    this.l.makeNodes();

    t.checkExpect(this.l.checkWin(), true);

    // testing lastScene() method
    WorldScene endScene = new WorldScene(IUtils.CELL_SIZE * 3, IUtils.CELL_SIZE * 3);

    this.l.board.get(0).get(0).drawGamePiece(endScene, l);
    this.l.board.get(0).get(1).drawGamePiece(endScene, l);
    this.l.board.get(0).get(2).drawGamePiece(endScene, l);
    this.l.board.get(1).get(0).drawGamePiece(endScene, l);
    this.l.board.get(1).get(1).drawGamePiece(endScene, l);
    this.l.board.get(1).get(2).drawGamePiece(endScene, l);
    this.l.board.get(2).get(0).drawGamePiece(endScene, l);
    this.l.board.get(2).get(1).drawGamePiece(endScene, l);
    this.l.board.get(2).get(2).drawGamePiece(endScene, l);

    WorldImage text = new OverlayImage(new TextImage("you won!", IUtils.CELL_SIZE / 2, Color.BLACK),
        new RectangleImage(IUtils.CELL_SIZE * 5, IUtils.CELL_SIZE / 2, OutlineMode.SOLID,
            Color.WHITE));

    endScene.placeImageXY(text, 75, 75);

    t.checkExpect(this.l.lastScene("you won!"), endScene);


    // tests the initaliazeGame() method
    this.l.initializeGame();

    this.gpInit1 = new GamePiece(0, 0, false, true, false, false, true, true);
    this.gpInit4 = new GamePiece(1, 0, true, false, false, true, false);
    this.gpInit7 = new GamePiece(2, 0, true, false, false, true, false);
    this.gpInit2 = new GamePiece(0, 1, true, false, false, true, false, true);
    this.gpInit5 = new GamePiece(1, 1, true, false, true, false, false, true);
    this.gpInit8 = new GamePiece(2, 1, true, true, false, false, false);
    this.gpInit3 = new GamePiece(0, 2, true, false, false, false, false);
    this.gpInit6 = new GamePiece(1, 2, false, false, true, true, false);
    this.gpInit9 = new GamePiece(2, 2, false, true, true, false, false);

    ArrayList<GamePiece> row1 = new ArrayList<GamePiece>(
        Arrays.asList(this.gpInit1, this.gpInit4, this.gpInit7));
    ArrayList<GamePiece> row2 = new ArrayList<GamePiece>(
        Arrays.asList(this.gpInit2, this.gpInit5, this.gpInit8));
    ArrayList<GamePiece> row3 = new ArrayList<GamePiece>(
        Arrays.asList(this.gpInit3, this.gpInit6, this.gpInit9));

    t.checkExpect(this.l.board,
        new ArrayList<ArrayList<GamePiece>>(Arrays.asList(row1, row2, row3)));
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.powerCol, 0);
    t.checkExpect(l.tickRate, 51);
    t.checkExpect(l.timeElapsed, 0);
    t.checkExpect(l.score, 0);
  }

  // big bang
  void testGame(Tester t) {
    LightEmAll l = new LightEmAll(6, 6);

    l.bigBang(l.width * IUtils.CELL_SIZE, l.width * IUtils.CELL_SIZE, 0.02);
  }
}