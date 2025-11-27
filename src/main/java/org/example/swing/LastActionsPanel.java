package org.example.swing;

import org.example.service.RecentActionsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class LastActionsPanel extends JPanel {

    private final DefaultListModel<String> filesModel    = new DefaultListModel<>();
    private final DefaultListModel<String> segmentsModel = new DefaultListModel<>();
    private final DefaultListModel<String> projectsModel = new DefaultListModel<>();

    private final JList<String> filesList    = new JList<>(filesModel);
    private final JList<String> segmentsList = new JList<>(segmentsModel);
    private final JList<String> projectsList = new JList<>(projectsModel);

    public LastActionsPanel() {
        setLayout(new GridLayout(3, 1, 6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        filesList.setFont(mono);
        segmentsList.setFont(mono);
        projectsList.setFont(mono);

        
        filesList.setCellRenderer(withTooltip(new DefaultListCellRenderer()));
        segmentsList.setCellRenderer(withTooltip(new DefaultListCellRenderer()));
        projectsList.setCellRenderer(withTooltip(new DefaultListCellRenderer()));

        
        filesList.setFixedCellHeight(-1);
        segmentsList.setFixedCellHeight(-1);
        projectsList.setFixedCellHeight(-1);

        add(wrap("Last files",       new JScrollPane(filesList)));
        add(wrap("Last selections",  new JScrollPane(segmentsList)));
        add(wrap("Last projects",    new JScrollPane(projectsList)));

        filesList.setVisibleRowCount(3);
        segmentsList.setVisibleRowCount(3);
        projectsList.setVisibleRowCount(3);

        
        setPlaceholders();
    }

    private ListCellRenderer<? super String> withTooltip(DefaultListCellRenderer base) {
        return (list, value, index, isSelected, cellHasFocus) -> {
            Component c = base.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (c instanceof JComponent jc) {
                jc.setToolTipText(value);
            }
            return c;
        };
    }

    private JPanel wrap(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        p.setBorder(tb);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    private void setPlaceholders() {
        if (filesModel.isEmpty())    filesModel.addElement("— empty —");
        if (segmentsModel.isEmpty()) segmentsModel.addElement("— empty —");
        if (projectsModel.isEmpty()) projectsModel.addElement("— empty —");
    }

    
    public void refreshFrom(RecentActionsService svc) {
        // files
        filesModel.clear();
        List<String> f = svc.filesDisplay();
        if (f.isEmpty()) filesModel.addElement("— empty —");
        else f.forEach(filesModel::addElement);

        
        segmentsModel.clear();
        List<String> s = svc.segmentsDisplay();
        if (s.isEmpty()) segmentsModel.addElement("— empty —");
        else s.forEach(segmentsModel::addElement);

        
        projectsModel.clear();
        List<String> p = svc.projectsDisplay();
        if (p.isEmpty()) projectsModel.addElement("— empty —");
        else p.forEach(projectsModel::addElement);
    }
}
