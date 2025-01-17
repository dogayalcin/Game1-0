package base.graphicsservice;

import base.Game;
import base.gameobjects.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class RenderHandler {

    private BufferedImage view;
    private int[] pixels;
    private Rectangle camera;
    private int maxScreenWidth;
    private int maxScreenHeight;

    protected static final Logger logger = LoggerFactory.getLogger(RenderHandler.class);

    public RenderHandler(int width, int height) {

        setSizeBasedOnScreenSize();

        //Create a BufferedImage that will represent our view.
        view = new BufferedImage(maxScreenWidth, maxScreenHeight, BufferedImage.TYPE_INT_RGB);

        camera = new Rectangle(0, 0, width, height);

        //Create an array for pixels
        pixels = ((DataBufferInt) view.getRaster().getDataBuffer()).getData();
    }

    private void setSizeBasedOnScreenSize() {
        GraphicsDevice[] graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice device : graphicsDevices) {
            if (maxScreenWidth < device.getDisplayMode().getWidth()) {
                maxScreenWidth = device.getDisplayMode().getWidth();
            }
            if (maxScreenHeight < device.getDisplayMode().getHeight()) {
                maxScreenHeight = device.getDisplayMode().getHeight();
            }
        }
    }

    public void render(Graphics graphics) {
        graphics.drawImage(view.getSubimage( 0, 0, camera.getWidth(), camera.getHeight()), 0,0, camera.getWidth(), camera.getHeight(), null);
    }

    public void renderRectangle(Rectangle rectangle, int xZoom, int yZoom, boolean fixed) {
        int[] rectanglePixels = rectangle.getPixels();
        if (rectanglePixels != null) {
            renderPixelsArrays(rectanglePixels, rectangle.getWidth(), rectangle.getHeight(), rectangle.getX(), rectangle.getY(), xZoom, yZoom, fixed);
        }
    }

    public void renderRectangle(Rectangle rectangle, Rectangle rectangleOffset, int xZoom, int yZoom, boolean fixed) {
        int[] rectanglePixels = rectangle.getPixels();
        if (rectanglePixels != null) {
            renderPixelsArrays(rectanglePixels, rectangle.getWidth(), rectangle.getHeight(), rectangle.getX() + rectangleOffset.getX(), rectangle.getY() + rectangleOffset.getY(), xZoom, yZoom, fixed);
        }
    }

    public Rectangle getCamera() {
        return camera;
    }

    public void renderSprite(Sprite sprite, int xPosition, int yPosition, int xZoom, int yZoom, boolean fixed) {
        renderPixelsArrays(sprite.getPixels(), sprite.getWidth(), sprite.getHeight(), xPosition, yPosition, xZoom, yZoom, fixed);
    }

    public void renderPixelsArrays(int[] renderPixels, int renderWidth, int renderHeight, int xPosition, int yPosition, int xZoom, int yZoom, boolean fixed) {
        for (int y = 0; y < renderHeight; y++) {
            for (int x = 0; x < renderWidth; x++) {
                for (int yZ = 0; yZ < yZoom; yZ++) {
                    for (int xZ = 0; xZ < xZoom; xZ++) {
                        int pixel = renderPixels[renderWidth * y + x];
                        int xPos = (x * xZoom + xZ + xPosition);
                        int yPos = (y * yZoom + yZ + yPosition);
                        setPixel(pixel, xPos, yPos, fixed);
                    }
                }
            }
        }
    }

    public void setPixel(int pixel, int x, int y, boolean fixed) {
        int pixelIndex = 0;
        if (!fixed && isInRangeOfCamera(x, y)) {
            pixelIndex = (x - camera.getX()) + (y - camera.getY()) * view.getWidth();
        }
        if (fixed && x >= 0 && y >= 0 && x <= camera.getWidth() && y <= camera.getHeight()) {
            pixelIndex = x + y * view.getWidth();
        }
        if (isInGlobalRange(pixelIndex) && !isAlphaColor(pixel)) {
            pixels[pixelIndex] = pixel;
        }
    }

    private boolean isAlphaColor(int pixel) {
        return pixel == Game.ALPHA;
    }

    private boolean isInGlobalRange(int pixelIndex) {
        return pixelIndex < pixels.length;
    }

    private boolean isInRangeOfCamera(int x, int y) {
        return x >= camera.getX() &&
                y >= camera.getY() &&
                x <= camera.getX() + camera.getWidth() &&
                y <= camera.getY() + camera.getHeight();
    }

    public void clear() {
        Arrays.fill(pixels, 0);
    }

    public int getMaxWidth() {
        return maxScreenWidth;
    }

    public int getMaxHeight() {
        return maxScreenHeight;
    }

    public void adjustCamera(Game game, Player player) {
        logger.info("Adjusting camera");
        Rectangle playerRect = player.getPlayerRectangle();

        logger.info("Adjusting X");
        int mapEnd = game.getGameMap().getMapWidth() * (Game.TILE_SIZE * Game.ZOOM);
        int diffToEnd = mapEnd - playerRect.getX();
        if (diffToEnd < 96) {
            logger.info("Adjustment will be on the right side");
            camera.setX(mapEnd + 64 - game.getWidth());
        }

        if (playerRect.getX() < 96) {
            logger.info("Adjustment will be on the left side");
            camera.setX(-64);
        }

        logger.info("Adjusting Y");
        mapEnd = game.getGameMap().getMapHeight() * (Game.TILE_SIZE * Game.ZOOM);
        diffToEnd = mapEnd - playerRect.getY();

        if (diffToEnd < 96) {
            logger.info("Adjustment will be on the bottom side");
            camera.setY(mapEnd + 96 - game.getHeight());
        }

        if (playerRect.getY() < 96) {
            logger.info("Adjustment will be on the top side");
            camera.setY(-64);
        }
    }
}
