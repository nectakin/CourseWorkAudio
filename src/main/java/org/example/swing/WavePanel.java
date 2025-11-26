package org.example.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

public class WavePanel extends JPanel {

    private int[] data;

    
    private Double lastClickNormX = null;

    public WavePanel() {
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (getWidth() > 0) {
                    double nx = e.getX() / (double) getWidth();
                    // clamp
                    if (nx < 0) nx = 0;
                    if (nx > 1) nx = 1;
                    lastClickNormX = nx;
                }
            }
        });
        setBackground(Color.WHITE);
    }

    public void setData(int[] data) {
        this.data = data;
        
        lastClickNormX = null;
        repaint();
    }

    
    public Double getLastClickNormX() {
        return lastClickNormX;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (data == null || data.length < 2) {
            
            g.setColor(Color.LIGHT_GRAY);
            g.drawString("No audio loaded", 10, 20);
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.BLACK);

            int maxAbs = Math.max(Math.abs(findMax(data)), Math.abs(findMin(data)));
            if (maxAbs == 0) maxAbs = 1;

            int w = getWidth();
            int h = getHeight();
            double mid = h / 2.0;

            for (int i = 1; i < data.length; i++) {
                double x  = (i        / (double) data.length) * w;
                double y  = (data[i]   / (double) maxAbs) * (h / 2.0) + mid;
                double x1 = ((i - 1)  / (double) data.length) * w;
                double y1 = (data[i-1] / (double) maxAbs) * (h / 2.0) + mid;
                g2d.draw(new Line2D.Double(x1, y1, x, y));
            }
        } finally {
            g2d.dispose();
        }
    }

    private int findMax(int[] a) {
        int max = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > max) max = a[i];
        return max;
    }
    private int findMin(int[] a) {
        int min = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < min) min = a[i];
        return min;
    }
}
