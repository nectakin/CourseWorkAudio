package org.example.composite;

import java.util.ArrayList;
import java.util.List;

public class UIContainer implements UIComponent {
    private List<UIComponent> children = new ArrayList<>();

    public void add(UIComponent component) {
        children.add(component);
    }

    public void remove(UIComponent component) {
        children.remove(component);
    }

    @Override
    public void operation() {
        for (UIComponent child : children) {
            child.operation();
        }
    }
}
/*UIContainer*/