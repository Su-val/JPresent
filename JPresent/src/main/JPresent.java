package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class JPresent extends JFrame {

    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = 0;
    private JPanel slidePanel;
    private JPanel sidebarPanel;
    private JButton editButton;
    private JButton displayButton;
    private JButton newSlideButton;
    private Dimension originalSize = new Dimension(800, 600);
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private boolean isEditing = false;

    public JPresent() {
        setTitle("Simple Slide Editor");
        setSize(originalSize);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Sidebar Panel
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(150, getHeight()));
        add(sidebarPanel, BorderLayout.WEST);

        // Slide Panel
        slidePanel = new JPanel();
        slidePanel.setLayout(null);  // Use null layout to allow absolute positioning
        slidePanel.setBackground(Color.WHITE);
        slidePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEditing) {
                    if (e.getComponent() == slidePanel) {
                        createTextFieldAt(e.getX(), e.getY());
                    }
                }
            }
        });
        slidePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeSlideComponents();
            }
        });
        add(slidePanel, BorderLayout.CENTER);

        // Control Panel for mode switching and new slide creation
        JPanel controlPanel = new JPanel();
        editButton = new JButton("Edit Mode");
        editButton.addActionListener(e -> switchToEditMode());
        displayButton = new JButton("Display Mode");
        displayButton.addActionListener(e -> switchToDisplayMode());
        newSlideButton = new JButton("New Slide");
        newSlideButton.addActionListener(e -> createNewSlide());

        controlPanel.add(editButton);
        controlPanel.add(displayButton);
        controlPanel.add(newSlideButton);
        add(controlPanel, BorderLayout.SOUTH);

        slides.add(new Slide());  // Create the first slide

        updateSlideView();
        updateSidebar();
    }

    private void updateSlideView() {
        slidePanel.removeAll();
        Slide currentSlide = slides.get(currentSlideIndex);
        Component[] slideComponents = currentSlide.getContent();
        for (Component component : slideComponents) {
            slidePanel.add(component);
        }
        slidePanel.revalidate();
        slidePanel.repaint();
    }

    private void updateSidebar() {
        sidebarPanel.removeAll();
        for (int i = 0; i < slides.size(); i++) {
            JButton thumbnailButton = createThumbnailButton(i);
            sidebarPanel.add(thumbnailButton);
        }
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private JButton createThumbnailButton(int slideIndex) {
        JButton thumbnailButton = new JButton("Slide " + (slideIndex + 1));
        thumbnailButton.addActionListener(e -> {
            saveCurrentSlideContent();
            currentSlideIndex = slideIndex;
            updateSlideView();
        });
        return thumbnailButton;
    }

    private void switchToEditMode() {
        isEditing = true;
        editButton.setEnabled(false);
        displayButton.setEnabled(true);
    }

    private void switchToDisplayMode() {
        isEditing = false;
        editButton.setEnabled(true);
        displayButton.setEnabled(false);
    }

    private void saveCurrentSlideContent() {
        Slide currentSlide = slides.get(currentSlideIndex);
        currentSlide.setContent(slidePanel.getComponents());
    }

    private void createTextFieldAt(int x, int y) {
        JTextField textField = createEditableTextField("", true);
        textField.setBounds(x, y, 200, 30);  // Set position and initial size
        slidePanel.add(textField); // Add directly to slidePanel
        textField.requestFocus();
    }

    private JTextField createEditableTextField(String text, boolean newTextField) {
        JTextField textField = new JTextField(text);
        textField.setFont(new Font("Arial", Font.PLAIN, 24));
        textField.setColumns(10);  // Default size of the text field (in columns)

        textField.addActionListener(e -> finishEditingTextField(textField));
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                finishEditingTextField(textField);
            }
        });

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                adjustTextFieldSize(textField);
            }
        });

        return textField;
    }

    private void adjustTextFieldSize(JTextField textField) {
        FontMetrics metrics = textField.getFontMetrics(textField.getFont());
        int width = metrics.stringWidth(textField.getText()) + 20;
        textField.setPreferredSize(new Dimension(width, textField.getPreferredSize().height));
        textField.setSize(width, textField.getPreferredSize().height);
        textField.revalidate();
    }

    private void finishEditingTextField(JTextField textField) {
        if (!isEditing) return;  // Don't allow editing if not in edit mode

        Container parent = textField.getParent();
        if (parent == null) return;

        String text = textField.getText().trim();
        if (!text.isEmpty()) {
            JLabel label = createEditableLabel(text);
            label.setBounds(textField.getBounds());
            parent.remove(textField);
            parent.add(label);
            parent.revalidate();
            parent.repaint();
        } else {
            parent.remove(textField);
            parent.revalidate();
            parent.repaint();
        }
    }

    private JLabel createEditableLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 24));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                editLabel(label);
            }
        });
        return label;
    }

    private void editLabel(JLabel label) {
        if (!isEditing) return;  // Don't allow editing if not in edit mode

        JTextField textField = createEditableTextField(label.getText(), false);
        textField.setBounds(label.getBounds());
        textField.setFont(label.getFont());
        Container parent = label.getParent();
        int index = parent.getComponentZOrder(label);
        parent.remove(label);
        parent.add(textField, index);
        textField.requestFocus();
        textField.selectAll();
        parent.revalidate();
        parent.repaint();
    }

    private void resizeSlideComponents() {
        double newScaleX = (double) slidePanel.getWidth() / originalSize.width;
        double newScaleY = (double) slidePanel.getHeight() / originalSize.height;
        scaleX = newScaleX;
        scaleY = newScaleY;

        for (Component component : slidePanel.getComponents()) {
            if (component instanceof JTextField || component instanceof JLabel) {
                Rectangle bounds = component.getBounds();
                bounds.x = (int) (bounds.x * scaleX);
                bounds.y = (int) (bounds.y * scaleY);
                bounds.width = (int) (bounds.width * scaleX);
                bounds.height = (int) (bounds.height * scaleY);
                component.setBounds(bounds);

                Font originalFont = component.getFont();
                int newFontSize = (int) (originalFont.getSize() * Math.min(scaleX, scaleY));
                component.setFont(new Font(originalFont.getName(), originalFont.getStyle(), newFontSize));
            }
        }
        slidePanel.revalidate();
        slidePanel.repaint();
    }

    private void createNewSlide() {
        saveCurrentSlideContent();  // Save the current slide content
        slides.add(new Slide());   // Add a new slide
        currentSlideIndex = slides.size() - 1;  // Set to the new slide
        updateSlideView();
        updateSidebar();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JPresent().setVisible(true));
    }

    // Inner class to represent a slide
    private class Slide {
        private List<Component> components;

        public Slide() {
            components = new ArrayList<>();
        }

        public Component[] getContent() {
            return components.toArray(new Component[0]);
        }

        public void setContent(Component[] newComponents) {
            components.clear();
            for (Component component : newComponents) {
                if (component.getParent() != null) {
                    component.getParent().remove(component);
                }
                components.add(component);
            }
        }
    }
}
