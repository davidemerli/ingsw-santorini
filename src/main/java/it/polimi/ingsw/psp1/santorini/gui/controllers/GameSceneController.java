package it.polimi.ingsw.psp1.santorini.gui.controllers;

import com.interactivemesh.jfx.importer.obj.ObjModelImporter;
import it.polimi.ingsw.psp1.santorini.gui.GuiObserver;
import it.polimi.ingsw.psp1.santorini.model.map.Point;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameSceneController extends GuiController {

    private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    private static GameSceneController instance;

    private final List<Node> validMoves = new ArrayList<>();

    private final DoubleProperty angleX = new SimpleDoubleProperty(40 * 4);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);

    public Map<Point, Group> map = new HashMap<>();
    public Map<Point, Group> workers = new HashMap<>();

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;

    private boolean isAnimating;

    private Group board;

    @FXML
    private SubScene mainScene;
    @FXML
    private AnchorPane pane;
    @FXML
    private Button button;
    @FXML
    private Button interactButton;
    @FXML
    private Button undoButton;
    @FXML
    private ImageView requestBackground;
    @FXML
    private Label requestText;

    public static GameSceneController getInstance() {
        if (instance == null) {
            instance = new GameSceneController();
        }

        return instance;
    }

    @FXML
    private void interactPressed(ActionEvent event) {
        getInstance().notifyObservers(GuiObserver::interactPressed);
    }

    @FXML
    private void undoPressed(ActionEvent event) {
        getInstance().notifyObservers(GuiObserver::undoPressed);
    }

    @FXML
    public void initialize() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-45.5);

        board = loadModel("/mesh/Board.obj", "/textures/Cliff_v007.png");
        Group cliff = loadModel("/mesh/cliff.obj", "/textures/Cliff_v007.png");
        Group sea = loadModel("/mesh/sea.obj", "/textures/Sea_v006.png");
        Group islands = loadModel("/mesh/islands.obj", "/textures/Islands_v002.png");
        sea.setTranslateY(2);
        islands.setTranslateY(2);

        Group root = new Group(board, cliff, sea, islands);

        mainScene = new SubScene(root, -1, -1, true, SceneAntialiasing.BALANCED);
        mainScene.setRoot(root);
        mainScene.setCamera(camera);
        mainScene.widthProperty().bind(pane.widthProperty());
        mainScene.heightProperty().bind(pane.heightProperty());
        pane.getChildren().add(1, mainScene);

        initMouseControl(root, mainScene);

        button.prefWidthProperty().bind(pane.widthProperty().divide(20));
        button.prefHeightProperty().bind(pane.widthProperty().divide(20));

        getInstance().requestBackground = requestBackground;
        getInstance().requestText = requestText;
        getInstance().interactButton = interactButton;
        getInstance().undoButton = undoButton;

        getInstance().board = board;
    }

    private Point3D convert2DTo3D(double x, double y) {
        double width = board.layoutBoundsProperty().getValue().getWidth() * 0.95;

        return new Point3D(x * width / 5, 0, (4 - y) * width / 5)
                .subtract(width / 2, 0, width / 2)
                .add(width / 10, 0, width / 10);
    }

    public void showValidMoves(List<Point> moves) {
        runMapChange(() -> {
            double width = getInstance().board.layoutBoundsProperty().getValue().getWidth() * 0.95;

            getInstance().board.getChildren().removeAll(getInstance().validMoves);
            getInstance().validMoves.clear();

            for (Point p : moves) {
                Point3D p3D = convert2DTo3D(p.x, p.y);
                Cylinder box = new Cylinder(width / 15, 0.1);
                box.setTranslateX(p3D.getX());
                box.setTranslateY(-3.5 - (getInstance().map.get(p) == null ? 0 : getInstance().map.get(p).getLayoutBounds().getHeight()));
                box.setTranslateZ(p3D.getZ());
                PhongMaterial m = new PhongMaterial(Color.valueOf("#3DABFF99"),
                        null, null, null, null);
                box.setMaterial(m);

                ScaleTransition st = new ScaleTransition(Duration.millis(500), box);
                st.setCycleCount(Animation.INDEFINITE);
                st.setAutoReverse(true);
                st.setFromX(0.8);
                st.setFromZ(0.8);
                st.setToX(1);
                st.setToZ(1);
                st.play();

                box.setOnMouseClicked(event -> {
                    getInstance().notifyObservers(o -> o.onMoveSelected(p));

                    getInstance().board.getChildren().removeAll(getInstance().validMoves);
                    getInstance().validMoves.clear();
                });

                getInstance().validMoves.add(box);
            }

            getInstance().board.getChildren().addAll(validMoves);
        });
    }

    public void addWorker(int x, int y, Color color, boolean isOwn) {
        runMapChange(() -> {
            Point p = new Point(x, y);

            if (workers.containsKey(p))
                return;

            Point3D p3D = convert2DTo3D(x, y);

            Group worker = loadModel("/mesh/MaleBuilder_Blue.obj",
                    "/textures/MaleBuilder_Tan_v001.png",
                    color);

            Group blocks = map.get(p);
            double height = blocks != null && blocks.getChildren().size() > 0 ? -blocks.getLayoutBounds().getHeight() : 0;

            worker.getTransforms().add(new Translate(p3D.getX(), height, p3D.getZ()));
            worker.getTransforms().add(new Rotate(new Random().nextInt(360), Rotate.Y_AXIS));

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), worker);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.setFromY(-20);
            tt.setToY(worker.getLayoutY());
            tt.play();

            addWorkerClickAction(worker, p, isOwn);

            getInstance().workers.put(p, worker);
            getInstance().board.getChildren().add(worker);
        });
    }

    public void addBlockAt(int x, int y, boolean forceDome) {
        runMapChange(() -> {
            getInstance().isAnimating = true;

            Point p = new Point(x, y);
            Point3D p3D = convert2DTo3D(x, y);

            if (!getInstance().map.containsKey(p)) {
                Group g = new Group();
                getInstance().map.put(p, g);
                getInstance().board.getChildren().add(g);
            }

            Group group = getInstance().map.get(p);

            int level = forceDome ? 4 : (group.getChildren().size() + 1);

            Group block = loadModel("/mesh/level" + level + ".obj", "/textures/BuildingBlock0" + level + "_v001.png");

            block.getTransforms().add(new Translate(
                    p3D.getX(),
                    group.getChildren().size() > 0 ? -group.getLayoutBounds().getHeight() : 0,
                    p3D.getZ()));

            getInstance().map.get(p).getChildren().add(block);

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), block);
            tt.setInterpolator(Interpolator.EASE_IN);
            tt.setFromY(-20);
            tt.setToY(block.getLayoutY());
            tt.setOnFinished(event -> getInstance().isAnimating = false);

            tt.play();
        });
    }

    public void moveWorker(Point from, Point to, boolean isOwn) {
        Platform.runLater(() -> {
            getInstance().isAnimating = true;

            Group worker = getInstance().workers.get(from);
            Point3D from3D = convert2DTo3D(from.x, from.y);
            Point3D to3D = convert2DTo3D(to.x, to.y);

            Point3D diff = to3D.subtract(from3D);

            if (worker == null) {
                throw new NoSuchElementException("Worker not found at given position");
            }

            double heightTo = getInstance().map.get(to) != null ? -getInstance().map.get(to).getLayoutBounds().getHeight() : 0;

            TranslateTransition tt = new TranslateTransition(Duration.millis(400), worker);
            tt.setByX(diff.getX());
            tt.setToY(heightTo);
            tt.setByZ(diff.getZ());
            tt.setInterpolator(Interpolator.TANGENT(Duration.millis(400), 2));
            tt.setOnFinished(event -> getInstance().workers.put(to, worker));
            tt.play();

            addWorkerClickAction(worker, to, isOwn);
        });
    }

    private Group loadModel(String obj, String texture) {
        return loadModel(obj, texture, Color.BLACK);
    }

    private Group loadModel(String obj, String texture, Color color) {
        Group modelRoot = new Group();

        ObjModelImporter importer = new ObjModelImporter();
        importer.read(getClass().getResource(obj));

        for (MeshView view : importer.getImport()) {
            modelRoot.getChildren().add(view);

            Image im = new Image(getClass().getResourceAsStream(texture));

            PhongMaterial material = new PhongMaterial(color);

            if (color == null) {
                Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                    try {
                        Color rainbowColor = makeColorGradient(System.currentTimeMillis() / 50, 0.3, 0, 2, 4, -1, -1)
                                .darker();
                        material.setDiffuseColor(rainbowColor);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, 0, 100, TimeUnit.MILLISECONDS);
            }

            material.setSelfIlluminationMap(im);
            view.setMaterial(material);
        }

        return modelRoot;
    }

    private Color makeColorGradient(long value, double freq, double p1, double p2, double p3, int center, int width) {
        if (center == -1)
            center = 128;
        if (width == -1)
            width = 127;

        int r = (int) (Math.sin(freq * value + p1) * width) + center;
        int g = (int) (Math.sin(freq * value + p2) * width) + center;
        int b = (int) (Math.sin(freq * value + p3) * width) + center;

        return Color.rgb(r, g, b);
    }

    private void initMouseControl(Group group, SubScene scene) {
        Rotate rotateX = new Rotate(40 * 4, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

        group.getTransforms().addAll(rotateX, rotateY);
        rotateX.angleProperty().bind(angleX.divide(4));
        rotateY.angleProperty().bind(angleY.divide(4));

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX.get();
            anchorAngleY = angleY.get();
        });

        scene.setOnMouseDragged(event -> {
            angleX.set(anchorAngleX - anchorY + event.getSceneY());
            angleY.set(anchorAngleY + anchorX - event.getSceneX());

            if (angleX.get() > 90 * 4) {
                angleX.set(90 * 4);
            }

            if (angleX.get() < 40 * 4) {
                angleX.set(40 * 4);
            }
        });
    }

    public void showInteract(boolean show) {
        if (getInstance().interactButton == null) {
            return;
        }

        Platform.runLater(() -> getInstance().interactButton.setVisible(show));
    }

    public void showUndo(boolean show) {
        if (getInstance().undoButton == null) {
            return;
        }

        Platform.runLater(() -> getInstance().undoButton.setVisible(show));
    }

    public void showRequest(String request) {
        if (getInstance().requestBackground == null || getInstance().requestText == null) {
            return;
        }

        Platform.runLater(() -> {
            getInstance().requestBackground.setVisible(true);
            getInstance().requestText.setVisible(true);
            getInstance().requestText.setText(request);
        });
    }

    public void hideRequest() {
        if (getInstance().requestBackground == null || getInstance().requestText == null) {
            return;
        }

        Platform.runLater(() -> {
            getInstance().requestBackground.setVisible(false);
            getInstance().requestText.setVisible(false);
        });
    }

    private void runMapChange(Runnable toRun) {
        pool.schedule(() -> Platform.runLater(toRun), 200, TimeUnit.MILLISECONDS);
    }

    private void addWorkerClickAction(Group worker, Point positionToSend, boolean isOwn) {
        worker.setOnMouseClicked(event -> {
            if (isOwn) {
                TranslateTransition selectAnimation = new TranslateTransition(Duration.millis(200), worker);
                selectAnimation.setInterpolator(Interpolator.EASE_BOTH);
                selectAnimation.setByY(-0.15);
                selectAnimation.setCycleCount(4);
                selectAnimation.setAutoReverse(true);
                selectAnimation.play();

                getInstance().notifyObservers(o -> o.onWorkerSelected(positionToSend));
            } else {
                getInstance().notifyObservers(o -> o.onMoveSelected(positionToSend));
            }
        });
    }

    @Override
    public void reset() {
        Platform.runLater(() -> {
            getInstance().workers.forEach((point, group) -> getInstance().board.getChildren().remove(group));
            getInstance().workers.clear();

            getInstance().map.forEach((point, group) -> getInstance().board.getChildren().remove(group));
            getInstance().map.clear();
        });
    }
}