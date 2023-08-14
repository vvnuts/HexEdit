import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class MyApp extends JFrame {
    private final WorkWithTable wwt;
    private JScrollPane jTable;
    private final Container container;
    public MyApp()
    {
        super("Hex-editor");
        setDefaultCloseOperation( EXIT_ON_CLOSE );
        setSize(600, 400);

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        setJMenuBar(menuBar);

        container = getContentPane();

        wwt = new WorkWithTable(container);

        jTable = new JScrollPane(wwt.getJTable());
        container.add(jTable, BorderLayout.CENTER);
        container.add(new JPanel(), BorderLayout.SOUTH);
        // Открываем окно
        setVisible(true);
    }

    private JMenu createFileMenu() {
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open");
        open.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose file");

                if ( fileChooser.showOpenDialog(MyApp.this) == JFileChooser.APPROVE_OPTION ) {

                    String path = fileChooser.getSelectedFile().getPath();
                    wwt.putFile(path, container);
                }
            }
        });
        JMenuItem save = new JMenuItem("Save as");
        save.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save file");

                if ( fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION ) {
                    File save = fileChooser.getSelectedFile();

                    wwt.saveFile(save.getPath());
                }
            }
        });

        file.add(open);
        file.add(save);

        return file;
    }
    private JMenu createEditMenu() {
        JMenu edit = new JMenu("Edit");
        JMenuItem changeNumColumns = new JMenuItem("Сменить кол-во столбцов");
        changeNumColumns.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String getMessage = JOptionPane.showInputDialog(MyApp.this, "Enter your num");
                if (getMessage == null) return;

                if (getMessage.replaceAll("[^0-9]", "").equals(getMessage) && Integer.parseInt(getMessage) != 0) {
                    remove(jTable);
                    jTable = new JScrollPane(wwt.changeTable(Integer.parseInt(getMessage)));
                    container.add(jTable, BorderLayout.CENTER);
                    repaint();
                    setVisible(true);
                }
            }
        });
        JMenuItem search = new JMenuItem("Search");
        search.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String getMessage = JOptionPane.showInputDialog(MyApp.this, "Введите что необходимо найти:");

                if (getMessage.replaceAll("[^ABCDEFabcdef0-9?]", "").equals(getMessage)) {
                    getMessage = getMessage.toUpperCase();
                    String[] beforePattern = getMessage.split("\\?");
                    ArrayList<String> pattern = new ArrayList<>();

                    for (String symbol: beforePattern
                    ) {
                        if (symbol.isEmpty()) {
                            pattern.add("?");
                        } else {
                            if (symbol.length() % 2 == 1) symbol = "0" + symbol;
                            String[] temp = symbol.split("(?<=\\G..)");

                            pattern.addAll(Arrays.asList(temp));
                            pattern.add("?");
                        }
                    }
                    pattern.remove(pattern.size() - 1);


                    wwt.KMP(pattern);
                }
            }
        });

        edit.add(changeNumColumns);
        edit.add(search);
        return edit;
    }

    public static void main(String[] args)
    {
        new MyApp();
    }
}
