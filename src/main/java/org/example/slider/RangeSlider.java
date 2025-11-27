package org.example.slider;

import javax.swing.JSlider;

public class RangeSlider extends JSlider {

    
    private int minGap = 100;

    public int getMinGap() { return minGap; }
    public void setMinGap(int minGap) { this.minGap = Math.max(0, minGap); }

    public RangeSlider() {
        initSlider();
    }

    public RangeSlider(int min, int max) {
        super(min, max);
        initSlider();
    }

    private void initSlider() {
        setOrientation(HORIZONTAL);
    }

    @Override
    public void updateUI() {
        setUI(new RangeSliderUI(this));
        
        updateLabelUIs();
    }

    
    @Override
    public int getValue() {
        return super.getValue();
    }

    
    @Override
    public void setValue(int value) {
        int oldLower = super.getValue();
        if (oldLower == value) {
            return;
        }
        int upper = getUpperValue();                  
        int newLower = Math.min(Math.max(getMinimum(), value), upper - minGap);
        int newExtent = upper - newLower;             

        getModel().setRangeProperties(
                newLower, newExtent, getMinimum(), getMaximum(), getValueIsAdjusting()
        );
    }

    
    public int getUpperValue() {
        return getValue() + getExtent();
    }

    
    public void setUpperValue(int value) {
        int lower = getValue();
        int maxUpper = getMaximum();
        int newUpper = Math.max(lower + minGap, Math.min(value, maxUpper));
        setExtent(newUpper - lower);
    }
}
