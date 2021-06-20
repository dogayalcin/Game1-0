package base;

import base.gameObjects.GameObject;
import base.gameObjects.Player;
import base.gameObjects.Rat;
import base.gameObjects.AnimatedSprite;
import base.graphicsService.RenderHandler;
import base.graphicsService.SpriteSheet;
import base.graphicsService.Rectangle;
import base.gui.GUI;
import base.gui.GUIButton;
import base.gui.SDKButton;
import base.map.GameMap;
import base.map.Tile;
import base.map.TileService;
import base.navigationService.KeyboardListener;
import base.navigationService.MouseEventListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Game extends JFrame implements Runnable {

    public static final int ALPHA = 0xFF80FF00;
    public static final int TILE_SIZE = 32;
    public static final int ZOOM = 2;

    public static final String PLAYER_SHEET_PATH = "img/betty.png";
    public static final String RAT_SHEET_PATH = "img/rat.png";
    public static final String MOUSE_SHEET_PATH = "img/mouse.png";
    public static final String SPRITES_PATH = "img/sprites.png";
    public static final String TILE_LIST_PATH = "src/main/java/base/map/config/Tile.txt";
    public static final String GAME_MAP_PATH = "src/main/java/base/map/config/GameMap.txt";

    private final Canvas canvas = new Canvas();

    private RenderHandler renderer;
    private SpriteSheet spriteSheet;
    private GameMap gameMap;
    private List<GameObject> gameObjectsList;
    private Player player;
    private Rat rat;
    private Rat rat2;
    private Rat mouse;
    private SpriteSheet playerSheet;
    private SpriteSheet ratSheet;
    private SpriteSheet mouseSheet;
    private AnimatedSprite playerAnimations;
    private AnimatedSprite ratAnimations;
    private AnimatedSprite ratAnimations2;
    private AnimatedSprite mouseAnimations;
    private TileService tileService;

    private GUI gui;
    private GUIButton[] buttons;
    private int selectedTileId = 2;

    private KeyboardListener keyboardListener = new KeyboardListener(this);
    private MouseEventListener mouseEventListener = new MouseEventListener(this);

    public Game() {
        loadUI();
        loadControllers();
        loadPlayerAnimatedImages();
        loadMap();
        loadSDKGUI();
        loadGameObjects();
    }

    public static void main(String[] args) {
        Game game = new Game();
        Thread gameThread = new Thread(game);
        gameThread.start();
    }

    public void run() {
        long lastTime = System.nanoTime(); //long 2^63
        double nanoSecondConversion = 1000000000.0 / 60; //60 frames per second
        double changeInSeconds = 0;

        while (true) {
            long now = System.nanoTime();

            changeInSeconds += (now - lastTime) / nanoSecondConversion;
            while (changeInSeconds >= 1) {
                update();
                changeInSeconds--;
            }

            render();
            lastTime = now;
        }
    }

    private void render() {
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();
        Graphics graphics = bufferStrategy.getDrawGraphics();
        super.paint(graphics);

        gameMap.renderMap(renderer, gameObjectsList, ZOOM, ZOOM);

//        for (GameObject gameObject : gameObjects) {
//            gameObject.render(renderer, ZOOM, ZOOM);
//        }

        renderer.render(graphics);

        graphics.dispose();
        bufferStrategy.show();
        renderer.clear();
    }

    private void update() {
        for (GameObject object : gameObjectsList) {
            object.update(this);
        }
    }

    private void loadUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, 0, 1400, 1000);
        setLocationRelativeTo(null);
        add(canvas);
        setVisible(true);
        canvas.createBufferStrategy(3);
        renderer = new RenderHandler(getWidth(), getHeight());
    }

    // TODO: refactor next two methods into one
    private void loadMap() {
        System.out.println("Game map loading started");

        loadSpriteSheet();
        tileService = new TileService(new File(TILE_LIST_PATH), spriteSheet);
        gameMap = new GameMap(new File(GAME_MAP_PATH), tileService);

        System.out.println("Game map loaded");
    }

    public void loadSecondaryMap(String mapPath) {
        System.out.println("Game map loading started");

        loadSpriteSheet();
        tileService = new TileService(new File(TILE_LIST_PATH), spriteSheet);
        gameMap = new GameMap(new File(mapPath), tileService);

        System.out.println("Game map loaded");

        loadGameObjects();
    }

    private void loadSpriteSheet() {
        System.out.println("Sprite sheet loading started");

        BufferedImage bufferedImage = loadImage(SPRITES_PATH);
        spriteSheet = new SpriteSheet(bufferedImage);
        spriteSheet.loadSprites(TILE_SIZE, TILE_SIZE, 0);

        System.out.println("Sprite sheet loading done");
    }

    private BufferedImage loadImage(String path) {
        try {
            System.out.println("Will try to load - " + path);
            BufferedImage image = ImageIO.read(Game.class.getResource(path));
            BufferedImage formattedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            formattedImage.getGraphics().drawImage(image, 0, 0, null);
            return formattedImage;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadControllers() {
        addListeners();

        canvas.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                int newWidth = canvas.getWidth();
                int newHeight = canvas.getHeight();

                if (newWidth > renderer.getMaxWidth())
                    newWidth = renderer.getMaxWidth();

                if (newHeight > renderer.getMaxHeight())
                    newHeight = renderer.getMaxHeight();

                renderer.getCamera().setWidth(newWidth);
                renderer.getCamera().setHeight(newHeight);
                canvas.setSize(newWidth, newHeight);
                pack();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
    }

    private void addListeners() {
        canvas.addKeyListener(keyboardListener);
        canvas.addFocusListener(keyboardListener);
        canvas.addMouseListener(mouseEventListener);
        canvas.addMouseMotionListener(mouseEventListener);
    }

    private void loadPlayerAnimatedImages() {
        System.out.println("Loading player animations");

        BufferedImage playerSheetImage = loadImage(PLAYER_SHEET_PATH);
        playerSheet = new SpriteSheet(playerSheetImage);
        playerSheet.loadSprites(TILE_SIZE, TILE_SIZE, 0);
        playerAnimations = new AnimatedSprite(playerSheet, 5, true);

        BufferedImage ratSheetImage = loadImage(RAT_SHEET_PATH);
        ratSheet = new SpriteSheet(ratSheetImage);
        ratSheet.loadSprites(TILE_SIZE, TILE_SIZE, 0);
        ratAnimations = new AnimatedSprite(ratSheet, 9, false);
        ratAnimations2 = new AnimatedSprite(ratSheet, 9, false);

        BufferedImage mouseSheetImage = loadImage(MOUSE_SHEET_PATH);
        mouseSheet = new SpriteSheet(mouseSheetImage);
        mouseSheet.loadSprites(TILE_SIZE, TILE_SIZE, 0);
        mouseAnimations = new AnimatedSprite(mouseSheet, 9, false);

        System.out.println("Player animations loaded");
    }

    private void loadGameObjects() {
        player = new Player(playerAnimations, getWidth()/2, getHeight()/2);
        rat = new Rat(ratAnimations,getWidth()/2 + 2, getHeight()/2 + 2);
        rat2 = new Rat(ratAnimations2,getWidth()/2 + 2, getHeight()/2 + 2);
        mouse = new Rat(mouseAnimations,getWidth()/2 + 2, getHeight()/2 + 2);
        gui = new GUI(buttons, 5, 5, true);

        gameObjectsList = new ArrayList<>();
        gameObjectsList.add(player);
        gameObjectsList.add(rat);
        gameObjectsList.add(mouse);
        gameObjectsList.add(rat2);
        gameObjectsList.add(gui);
    }

    private void loadSDKGUI() {
        List<Tile> tiles = tileService.getTiles();
        GUIButton[] buttons = new GUIButton[tiles.size()];

        for (int i = 0; i < buttons.length; i++) {
//            Rectangle tileRectangle = new Rectangle(0, i * (TILE_SIZE * ZOOM + 2), TILE_SIZE * ZOOM, TILE_SIZE * ZOOM);       // vertical on top left side
            Rectangle tileRectangle = new Rectangle(i * (TILE_SIZE * ZOOM + 2), 0, TILE_SIZE * ZOOM, TILE_SIZE * ZOOM);  //horizontal on top left
            buttons[i] = new SDKButton(this, i, tiles.get(i).getSprite(), tileRectangle);
        }

        this.buttons = buttons;
    }

    public void changeTile(int tileId) {
        System.out.println("changing tile to new tile : " + tileId);
        selectedTileId = tileId;
    }

    public void leftClick(int x, int y) {
        Rectangle mouseRectangle = new Rectangle(x, y, 1, 1);
        boolean stoppedChecking = false;

        for (GameObject gameObject : gameObjectsList) {
            if (!stoppedChecking) {
                stoppedChecking = gameObject.handleMouseClick(mouseRectangle, renderer.getCamera(), ZOOM, ZOOM);
            }
        }
        if (!stoppedChecking) {
            x = (int) Math.floor((x + renderer.getCamera().getX()) / (32.0 * ZOOM));
            y = (int) Math.floor((y + renderer.getCamera().getY()) / (32.0 * ZOOM));
            gameMap.setTile(x, y, selectedTileId);

        }
    }

    public void rightClick(int x, int y) {
        x = (int) Math.floor((x + renderer.getCamera().getX()) / (32.0 * ZOOM));
        y = (int) Math.floor((y + renderer.getCamera().getY()) / (32.0 * ZOOM));
        gameMap.removeTile(x, y, tileService.getLayerById(selectedTileId));
    }

    public void handleCTRL(boolean[] keys) {
        if (keys[KeyEvent.VK_S]) {
            gameMap.saveMap();
        }
    }

    public void handleQ(boolean[] keys) {
        if (keys[KeyEvent.VK_Q] && gameObjectsList.contains(gui)) {
            gameObjectsList.remove(gui);
        } else {
            gameObjectsList.add(gui);
        }
    }

    public int getSelectedTileId() {
        return selectedTileId;
    }

    public KeyboardListener getKeyboardListener() {
        return keyboardListener;
    }

    public RenderHandler getRenderer() {
        return renderer;
    }

    public GameMap getGameMap() {
        return gameMap;
    }
}