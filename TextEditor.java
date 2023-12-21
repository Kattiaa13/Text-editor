import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
    private JTextPane textPane;
    private JLabel cursorLabel, charCountLabel;
    private JCheckBoxMenuItem boldMenuItem, italicMenuItem, underlineMenuItem, resizeMenuItem;

    public TextEditor() {
        setTitle("Text Editor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textPane = new JTextPane();
        textPane.setEditorKit(new WrapEditorKit());
        textPane.getDocument().addUndoableEditListener(new UndoHandler());

        JScrollPane scrollPane = new JScrollPane(textPane);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        cursorLabel = new JLabel("Cursor: 1:1");
        charCountLabel = new JLabel("Characters: 0");

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem newMenuItem = new JMenuItem("New");
        newMenuItem.addActionListener(e -> newFile());
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        fileMenu.add(newMenuItem);

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(e -> openFile());
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileMenu.add(openMenuItem);

        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(e -> saveFile());
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        fileMenu.add(saveMenuItem);

        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        saveAsMenuItem.addActionListener(e -> saveAsFile());
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK));
        fileMenu.add(saveAsMenuItem);

        fileMenu.addSeparator();

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitMenuItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        JMenuItem undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.addActionListener(e -> undo());
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
        editMenu.add(undoMenuItem);

        JMenuItem redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.addActionListener(e -> redo());
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
        editMenu.add(redoMenuItem);

        editMenu.addSeparator();

        JMenuItem cutMenuItem = new JMenuItem("Cut");
        cutMenuItem.addActionListener(e -> cutText());
        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
        editMenu.add(cutMenuItem);

        JMenuItem copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.addActionListener(e -> copyText());
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
        editMenu.add(copyMenuItem);

        JMenuItem pasteMenuItem = new JMenuItem("Paste");
        pasteMenuItem.addActionListener(e -> pasteText());
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
        editMenu.add(pasteMenuItem);

        editMenu.addSeparator();

        JMenuItem selectAllMenuItem = new JMenuItem("Select All");
        selectAllMenuItem.addActionListener(e -> selectAll());
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        editMenu.add(selectAllMenuItem);

        editMenu.addSeparator();

        JMenuItem findMenuItem = new JMenuItem("Find");
        findMenuItem.addActionListener(e -> findText());
        findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
        editMenu.add(findMenuItem);

        JMenuItem replaceMenuItem = new JMenuItem("Replace");
        replaceMenuItem.addActionListener(e -> replaceText());
        replaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
        editMenu.add(replaceMenuItem);

        // Format menu
        JMenu formatMenu = new JMenu("Format");
        menuBar.add(formatMenu);

        boldMenuItem = new JCheckBoxMenuItem("Bold");
        boldMenuItem.addActionListener(e -> boldText());
        formatMenu.add(boldMenuItem);

        italicMenuItem = new JCheckBoxMenuItem("Italic");
        italicMenuItem.addActionListener(e -> italicText());
        formatMenu.add(italicMenuItem);

        underlineMenuItem = new JCheckBoxMenuItem("Underline");
        underlineMenuItem.addActionListener(e -> underlineText());
        formatMenu.add(underlineMenuItem);

        resizeMenuItem = new JCheckBoxMenuItem("Resize Text");
        resizeMenuItem.addActionListener(e -> resizeText());
        formatMenu.add(resizeMenuItem);

        // Toolbar
        JToolBar toolbar = new JToolBar();
        getContentPane().add(toolbar, BorderLayout.NORTH);

        JButton boldButton = new JButton("Bold");
        boldButton.addActionListener(e -> boldText());
        toolbar.add(boldButton);

        JButton italicButton = new JButton("Italic");
        italicButton.addActionListener(e -> italicText());
        toolbar.add(italicButton);

        JButton underlineButton = new JButton("Underline");
        underlineButton.addActionListener(e -> underlineText());
        toolbar.add(underlineButton);

        JButton resizeButton = new JButton("Resize Text");
        resizeButton.addActionListener(e -> resizeText());
        toolbar.add(resizeButton);

        // Status Bar
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        statusBar.add(cursorLabel);
        statusBar.add(charCountLabel);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStatus();
            }
        });

        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateStatus();
            }
        });

        menuBar.add(Box.createHorizontalGlue());
        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(e -> showAbout());
        menuBar.add(aboutMenuItem);
    }

    private void updateStatus() {
        try {
            int offset = textPane.getCaretPosition();
            int caretpos = textPane.getCaretPosition();
            Element map = textPane.getDocument().getDefaultRootElement();
            int row = map.getElementIndex(caretpos) + 1;
            int col = offset - Utilities.getRowStart(textPane, offset) + 1;

            cursorLabel.setText("Cursor: " + row + ":" + col);

            String text = textPane.getText();
            charCountLabel.setText("Characters: " + text.length());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void newFile() {
        textPane.setText("");
        setTitle("Text Editor");
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                textPane.read(reader, null);
                reader.close();
                setTitle("Text Editor - " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile() {
        String title = getTitle();
        if (title.equals("Text Editor")) {
            saveAsFile();
        } else {
            try {
                File file = new File(title.substring("Text Editor - ".length()));
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                textPane.write(writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveAsFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                textPane.write(writer);
                writer.close();
                setTitle("Text Editor - " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void undo() {
        if (textPane.isEditable() && ((AbstractDocument) textPane.getDocument()).getUndoableEditListeners().length > 0) {
            ((UndoHandler) ((AbstractDocument) textPane.getDocument()).getUndoableEditListeners()[0]).undo();
        }
    }

    private void redo() {
        if (textPane.isEditable() && ((AbstractDocument) textPane.getDocument()).getUndoableEditListeners().length > 0) {
            ((UndoHandler) ((AbstractDocument) textPane.getDocument()).getUndoableEditListeners()[0]).redo();
        }
    }

    private void cutText() {
        textPane.cut();
    }

    private void copyText() {
        textPane.copy();
    }

    private void pasteText() {
        textPane.paste();
    }

    private void selectAll() {
        textPane.selectAll();
    }

    private void findText() {
        String findStr = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (findStr != null) {
            String text = textPane.getText();
            int start = text.indexOf(findStr);
            if (start != -1) {
                textPane.setSelectionStart(start);
                textPane.setSelectionEnd(start + findStr.length());
            }
        }
    }

    private void replaceText() {
        String findStr = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (findStr != null) {
            String replaceStr = JOptionPane.showInputDialog(this, "Enter replacement text:");
            if (replaceStr != null) {
                String text = textPane.getText();

                // Використовуємо регулярний вираз для заміни всіх входжень findStr на replaceStr
                text = text.replaceAll(Pattern.quote(findStr), Matcher.quoteReplacement(replaceStr));

                textPane.setText(text);
            }
        }
    }

    private void boldText() {
        MutableAttributeSet attributes = textPane.getInputAttributes();
        StyleConstants.setBold(attributes, !StyleConstants.isBold(attributes));
        textPane.setCharacterAttributes(attributes, false);
    }

    private void italicText() {
        MutableAttributeSet attributes = textPane.getInputAttributes();
        StyleConstants.setItalic(attributes, !StyleConstants.isItalic(attributes));
        textPane.setCharacterAttributes(attributes, false);
    }

    private void underlineText() {
        MutableAttributeSet attributes = textPane.getInputAttributes();
        StyleConstants.setUnderline(attributes, !StyleConstants.isUnderline(attributes));
        textPane.setCharacterAttributes(attributes, false);
    }

    private void resizeText() {
        String sizeStr = JOptionPane.showInputDialog(this, "Enter size (e.g., 12):");
        if (sizeStr != null) {
            try {
                int size = Integer.parseInt(sizeStr);
                if (size > 0) {
                    MutableAttributeSet attributes = textPane.getInputAttributes();
                    StyleConstants.setFontSize(attributes, size);
                    textPane.setCharacterAttributes(attributes, false);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAbout() {
        String aboutMessage = "Developed by Katia Lobachova.\nVersion 2.0\nDeveloped in 2023.";
        JOptionPane.showMessageDialog(this, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class WrapEditorKit extends StyledEditorKit {
        private ViewFactory defaultFactory = new WrapColumnFactory();

        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private static class WrapColumnFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    private static class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return super.getPreferredSpan(axis);
        }

        @Override
        public float getPreferredSpan(int axis) {
            return super.getPreferredSpan(axis);
        }
    }

    private static class UndoHandler implements UndoableEditListener {
        private UndoManager undoManager;

        public UndoHandler() {
            undoManager = new UndoManager();
        }

        public void undo() {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        }

        public void redo() {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        }

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undoManager.addEdit(e.getEdit());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TextEditor editor = new TextEditor();
            editor.setVisible(true);
        });
    }
}