package org.example.slider;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseEvent;

public class RangeSliderUI extends BasicSliderUI {

    private final Rectangle upperThumbRect;

    public RangeSliderUI(RangeSlider slider) {
        super(slider);
        this.upperThumbRect = new Rectangle();
    }

    @Override
    protected void calculateThumbSize() {
        super.calculateThumbSize();
        upperThumbRect.setSize(thumbRect.width, thumbRect.height);
    }

    @Override
    protected void calculateThumbLocation() {
        super.calculateThumbLocation();
        RangeSlider rs = (RangeSlider) this.slider;
        int upperPos = xPositionForValue(rs.getUpperValue());
        upperThumbRect.x = upperPos - (upperThumbRect.width / 2);
        upperThumbRect.y = trackRect.y;
    }

    @Override
    public void paintThumb(Graphics g) {
        
        super.paintThumb(g);
        
        g.setColor(Color.BLUE);
        g.fillRect(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height);
    }

    @Override
    protected void scrollDueToClickInTrack(int direction) {
        
    }

    @Override
    protected TrackListener createTrackListener(JSlider slider) {
        return new RangeTrackListener();
    }

    class RangeTrackListener extends TrackListener {
        private boolean upperDragging = false;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!slider.isEnabled()) return;

            Point p = e.getPoint();
            if (upperThumbRect.contains(p)) {
                
                upperDragging = true;
                slider.requestFocus();
                slider.setValueIsAdjusting(true);
            } else if (thumbRect.contains(p)) {
                
                upperDragging = false;
                super.mousePressed(e);
            } else {
                
                upperDragging = false;
                super.mousePressed(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!slider.isEnabled()) return;

            if (upperDragging) {
                RangeSlider rs = (RangeSlider) slider;
                int x = Math.max(trackRect.x, Math.min(e.getX(), trackRect.x + trackRect.width));
                int newUpper = valueForXPosition(x);
                rs.setUpperValue(newUpper);  
                slider.repaint(upperThumbRect);
            } else {
                super.mouseDragged(e); 
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (upperDragging) {
                slider.setValueIsAdjusting(false);
                upperDragging = false;
                slider.repaint();
            } else {
                super.mouseReleased(e);
            }
        }
    }
}
