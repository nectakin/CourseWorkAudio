package org.example.composite;

import javax.swing.*;

public class UIElement implements UIComponent {
    private JComponent component;

    public UIElement(JComponent component) {
        this.component = component;
    }

    @Override
    public void operation() {
        component.repaint();
    }
}
/*UIElement*/