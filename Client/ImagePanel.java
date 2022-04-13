package Client;

import javax.swing.*;
import java.awt.*;

public final class ImagePanel extends JPanel {
  private final Image img;
  private final int y;
  private final int x;

  public ImagePanel(String img, int y, int x) {
    this.img = new ImageIcon(img).getImage();
    Dimension size = new Dimension(this.img.getWidth(null), this.img.getHeight(null));
    setPreferredSize(size);
    setMinimumSize(size);
    setMaximumSize(size);
    setSize(size);
    setLayout(null);
    this.y = y;
    this.x = x;
  }

  public int findX() {
    return x;
  }

  public int findY() {
    return y;
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(img, 0, 0, null);
  }
}
