import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class WorkWithTable {
    private WorkWithFile wwf;
    private JTable myTable;
    private boolean canChanged = false;
    private DefaultTableModel tableModel;
    private final ArrayList<Integer> highlightCells = new ArrayList<>();
    private int indexHighlite = 0;
    private Container myApp;
    private boolean isTableEmpty = true;
    private boolean isByteInfoCreate = false;
    private final Clipboard cp = Toolkit.getDefaultToolkit().getSystemClipboard();
    private int cols = 8;
    private int sumCells;
    private int address = 1;
    private final JLabel byteInfo = new JLabel();
    private JPanel button;

    public WorkWithTable(Container myApp) { // Получаем контейнер для добаления в него информации о байтах и прокрутки при поиске
        this.myApp = myApp;
    }

    public JTable getJTable () { // Выдаем MyApp таблицу для обновления в контейнере
        tableModel = setOptionModel(cols);
        myTable = setOptionTable(tableModel);
        return myTable;
    }
    private DefaultTableModel setOptionModel(int columns) { // Задаём параметры модели
        DefaultTableModel model = new DefaultTableModel(getColumnsName(columns), 1) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return !(column == 0);
            }
        };
        model.setValueAt(" 0", 0, 0);

        model.addTableModelListener(e -> { // Слушатель изменений таблицы, чтобы исправить случаи, когда пользователь ввёл недопустимое
            if (canChanged) {
                canChanged = false; // Запрещаем слушать изменения
                String tableValue = null;
                int startIndex = e.getLastRow() * cols + e.getColumn();
                if (model.getValueAt(e.getLastRow(), e.getColumn()) == "") { // Проверка на удаление значения
                    model.setValueAt("00", e.getLastRow(), e.getColumn());
                } else { // Иначе беерм то, что ввёл пользователь
                    tableValue = model.getValueAt(e.getLastRow(), e.getColumn()).toString();
                }
                if (tableValue != null && (!tableValue.replaceAll("[^A-F0-9]", "").equals(tableValue)
                        || tableValue.length() != 2)) { // Проверяем то, что ввёл пользователь на регистр, допустимые буквы и количество
                    tableValue = tableValue.replaceAll("[^A-Fa-f0-9]", "").toUpperCase();
                    if (tableValue.length() % 2 == 1) tableValue = "0" + tableValue;
                    if (tableValue.length() != 0) {
                        model.setValueAt(tableValue.substring(0, 2), e.getLastRow(), e.getColumn());
                    } else {
                        model.setValueAt("00", e.getLastRow(), e.getColumn());
                    }
                }
                if (startIndex > sumCells) { // Проверяем нужно ли заполнять пустые ячейки
                    sumCells += 1;
                    if (model.getValueAt(e.getLastRow(), e.getColumn() - 1) == null) {
                        int addNulls = fillNullCells(model, startIndex + model.getRowCount() - 1, cols);
                        sumCells += addNulls;
                    }
                }
                checkAndAddRow(startIndex + model.getRowCount() - 1, myTable.getRowCount()); // Проверяем нужен ли дополнительная строка, если последняя полная
                canChanged = true; // Разрешаем слушать изменения
            }
        });
        return model;
    }
    private JTable setOptionTable (DefaultTableModel model) { // Задаем параметры таблицы
        JTable table = new JTable(model);
        table.getColumnModel().setColumnSelectionAllowed( true );
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumnModel columnModel = table.getColumnModel(); // Задаём размеры колонок
        columnModel.getColumn(0).setPreferredWidth(80);
        for (int i = 1; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(25);
        }

        if (!isByteInfoCreate) { // Проверка создавался ли блок информации о байтах
            byteInfo.setPreferredSize(new Dimension(140, 100));
            myApp.add(byteInfo, BorderLayout.WEST);
            isByteInfoCreate = true;
            canChanged = true;
        }

        if (isTableEmpty) { // Если таблица пстая, то делаем выделение первой ячейки
            selectLastByte(model, table, 1);
            isTableEmpty = false;
        }

        JPopupMenu popupMenu = createPopupMenu(); // Создаём всплывающее меню
        table.setComponentPopupMenu(popupMenu);

        table.addMouseListener(new MouseAdapter() { // Чтение информации выделенных мышкой байт
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    getByteInfo(model, table);
                }
            }
        });

        return table;
    }
    public static long parseUnsignedHex(String text) {
        if (text.length() == 16) {
            return (parseUnsignedHex(text.substring(0, 1)) << 60)
                    | parseUnsignedHex(text.substring(1));
        }
        return Long.parseLong(text, 16);
    }
    private String[] getColumnsName(int columns) {
        String[] columnsName = new String[columns + 1];
        columnsName[0] = " ";
        for (int i = 1; i < columnsName.length; i++) {
            columnsName[i] = String.format("%02X", i - 1);
        }

        return columnsName;
    }
    public void putFile(String path, Container container) { // Помещаем файл в таблицу
        canChanged = false;
        myApp = container;
        wwf = new WorkWithFile(path);
        if (sumCells > 0) {
            for (int i = sumCells + myTable.getRowCount() - 1, counter = 0; counter < sumCells; counter++, i--) { // Если таблица непустая удаляем занечния из неё
                if (i % (cols + 1) == 0) i--;
                tableModel.setValueAt(null, i / (cols + 1), i % (cols + 1));
            }
        }
        deleteNullRow(tableModel); // Удаляет все стрки, кроме первой
        tableModel.removeRow(0); // В любом случае убираем первую строку, т.к. он мешает алгоритму
        wwf.readFile(tableModel, cols); // считываем
        selectLastByte(tableModel, myTable, address);
        isTableEmpty = false;
        sumCells = wwf.getQuaNumberB(); // Записываем кол-во байт

        canChanged = true;
    }
    public void saveFile(String path) { // Сохранение в файл
        canChanged = false;

        wwf = new WorkWithFile(path);
        wwf.setQuaNumberB(sumCells);
        wwf.write(tableModel, cols);

        canChanged = true;
    }
    public void KMP(ArrayList<String> pattern){ // Алгоритм кнутта-морриса пратта
        canChanged = false;
        // базовый случай 1: шаблон нулевой или пустой
        highlightCells.clear();
        if (pattern == null || pattern.size() == 0)
        {
            JOptionPane.showMessageDialog(null, "Шаблон пустой");
            return;
        }

        // базовый случай 2: текст равен NULL или длина текста меньше длины шаблона
        if (pattern.size() > sumCells || tableModel.getValueAt(0, 1) == null)
        {
            JOptionPane.showMessageDialog(null, "Текст равен NULL или длина текста меньше длины шаблона");
            return;
        }

        // next[i] сохраняет индекс следующего лучшего частичного совпадения
        int[] next = new int[pattern.size() + 1];
        for (int i = 1; i < pattern.size(); i++)
        {
            int j = next[i];

            while (j > 0 && pattern.get(j).equals(pattern.get(i))) {
                j = next[j];
            }

            if (j > 0 || pattern.get(j).equals(pattern.get(i))) {
                next[i + 1] = j + 1;
            }
        }

        for (int i = 1, j = 0, counter = 0; counter < sumCells; counter++, i++) {
            if (i % (cols + 1) == 0) i++;
            String tableValue = tableModel.getValueAt(i / (cols + 1), i % (cols + 1)).toString();
            if (j < pattern.size() && (pattern.get(j).equals("?") || tableValue.equals(pattern.get(j))))
            {
                if (++j == pattern.size()) {
                    highlightCells.add(counter - j + 2);
                }
            }
            else if (j > 0)
            {
                j = next[j];
                counter--;
                i--;    // так как `i` будет увеличен на следующей итерации
                if (i % (cols + 1) == 0) i--;
            }
        }
        canChanged = true;
        scrollSearch();
    }
    private void scrollSearch() { // Функция для скролла найденных строк
        selectLastByte(tableModel, myTable, highlightCells.get(0));
        JButton up = new JButton("Up");
        up.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indexHighlite = (indexHighlite - 1) % (highlightCells.size());
                if (indexHighlite == - 1) indexHighlite = highlightCells.size() - 1;
                selectLastByte(tableModel, myTable, highlightCells.get(indexHighlite));
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeButton();
            }
        });
        JButton down = new JButton("Down");
        down.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indexHighlite = (indexHighlite + 1) % (highlightCells.size());
                selectLastByte(tableModel, myTable, highlightCells.get(indexHighlite));
            }
        });
        button = new JPanel(new GridLayout(3, 1));
        button.add(up);
        button.add(down);
        button.add(close);
        myApp.add(button, BorderLayout.EAST);
        myApp.revalidate();
    }
    private void removeButton() {
        myApp.remove(button);
        myApp.revalidate();
        highlightCells.clear();
    }
    private void setAddress(){
        address = myTable.getSelectedRow() * cols + myTable.getSelectedColumn();
        if (address > sumCells) address = 1;
        if (address < 0) address = 1;
    }

    private void selectLastByte(DefaultTableModel model, JTable table,int address) {
        if (address % cols != 0) { // Условие нужно чтобы пропустить первый столбец с названиями строк
            table.changeSelection(address / cols, address % cols, false, false);
        } else {
            table.changeSelection(address / cols - 1, address % cols + cols, false, false);
        }
        getByteInfo(model, table);
    }
    public void getByteInfo(DefaultTableModel model, JTable table){
        int rowSelected = table.getSelectedRow();
        int colSelected = table.getSelectedColumn();
        if (colSelected == 0){
            byteInfo.setText("<html>  int: null" + "<br>  float: null" + "</html>");
        } else {
            try {
                String value = model.getValueAt(rowSelected, colSelected).toString();

                int intInfo = Integer.valueOf(value, 16);
                float doubleInfo = Float.intBitsToFloat(intInfo);

                byteInfo.setText("<html>  int: " + intInfo + "<br>  float: " + doubleInfo + "</html>");
            } catch (NullPointerException npe) {
                byteInfo.setText("<html>  int: null" + "<br>  float: null" + "</html>");
            }

        }
    }
    public JTable changeTable(int numColumns) { // Функция смены таблицы при изменении кол-ва столбцов
        canChanged = false;
        setAddress(); // Сохраняем последний выделенный адрес

        DefaultTableModel tempTableModel = setOptionModel(numColumns); //Создаём новую модель
        tempTableModel.removeRow(0); // УДаляем первую строку, т.к. она для ввода без файла
        JTable tempTable = setOptionTable(tempTableModel);
        int counter = 0; //Вводим счётчик байт
        String[] row = new String[numColumns + 1]; // Строка, которую мы будем заполнять и всталять в таблицу

        if (tableModel.getValueAt(0, 1) != null) { // Если таблица непустая
            isTableEmpty = false;
            for (int i = 0; i < myTable.getRowCount(); i++) { //Цикл по строкам
                for (int j = 1; j < myTable.getColumnCount(); j++) { // Цикл по столбцам
                    try {
                        int temp = counter % numColumns; // Счётчик по новой строке
                        if (temp == 0) row[temp] = String.format("%2X", counter); // 0-ой индекс заполняем назанием
                        row[counter % numColumns + 1] = tableModel.getValueAt(i, j).toString();
                        counter += 1;
                        if (counter % (numColumns) == 0) {
                            tempTableModel.addRow(row);
                        }
                    } catch (NullPointerException e) { // Если считали нуль копируем неполную строку и вставляем в таблицу
                        String[] lastRow = new String[counter % numColumns + 1];
                        System.arraycopy(row, 0, lastRow, 0, lastRow.length);
                        tempTableModel.addRow(lastRow);
                        break;
                    }
                }
            }
        } else { // Если таблицу пустая добавляем пустую строку
            row[0] = " 0";
            tempTableModel.addRow(row);
        }

        sumCells = counter; // Записыаем кол-во байт
        cols = numColumns; // и столбцовв

        tableModel = tempTableModel;
        myTable = tempTable;
        if (!isTableEmpty) selectLastByte(tableModel, myTable, address);

        canChanged = true;
        return tempTable;
    }
    private JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBounds(200, 100, 150, 200);
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canChanged = false;
                int result = JOptionPane.showConfirmDialog(popup, "Удалить со смещением?");
                if (result == JOptionPane.NO_OPTION) { // Если без смещения

                    int[] indexSelectedRows = myTable.getSelectedRows();
                    int[] indexSelectedCols = myTable.getSelectedColumns();

                    for (int row: indexSelectedRows) {
                        for (int col: indexSelectedCols) {
                            if (tableModel.getValueAt(row, col) != null) // Если знаечние ячейки ненулевое заполняем 00 иначе не трогаем
                                tableModel.setValueAt("00", row, col);
                        }
                    }
                } else if (result == JOptionPane.YES_OPTION) { // Если удаление со смещением
                    int[] selectedRows = myTable.getSelectedRows();
                    int[] selectedCols = myTable.getSelectedColumns();

                    for (int row : selectedRows) { // Задаём выделенным строчкам значение "удаления"
                        for (int col : selectedCols) {
                            if (tableModel.getValueAt(row, col) != null) {
                                tableModel.setValueAt("DEL", row, col);
                            }
                        }
                    }

                    int step = runOfTable(myTable); // Переписываем таблицу

                    for (int i = sumCells + myTable.getRowCount() - 1, counter = 0; counter < step; counter++, i--) { //Удаляем лишние значения после перезаписи
                        if (i % (cols + 1) == 0) i--;
                        tableModel.setValueAt(null, i / (cols + 1), i % (cols + 1));
                    }

                    deleteNullRow(tableModel); // Удаляем лишние строчки
                    sumCells -= step;
                }
                canChanged = true;
            }
        });
        JMenuItem add = new JMenuItem("Add");
        add.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(popup, "Добавить со смещением?");

                if (result == JOptionPane.CANCEL_OPTION) return;
                canChanged = false;

                if (result == JOptionPane.NO_OPTION) { // Без смещения
                    String getMessage = JOptionPane.showInputDialog(popup, "Введите что необходимо вставить:");

                    if (getMessage != null && getMessage.replaceAll("[^A-Fa-f0-9]", "").equals(getMessage)) { // Проверяем сообщение на корректность
                        getMessage = getMessage.toUpperCase();
                        if (getMessage.length() % 2 == 1) getMessage = "0" + getMessage;
                        String[] res = getMessage.split("(?<=\\G..)");

                        int indexSelectedRow = myTable.getSelectedRow();
                        int indexSelectedCols = myTable.getSelectedColumn();
                        int startIndex = indexSelectedRow * (cols + 1) + indexSelectedCols;

                        int addNulls = fillNullCells(tableModel, startIndex, cols); // если есть нулевые ячейки раньше - заполняем

                        int addNewValue = fillWithoutOffset(res, indexSelectedRow, indexSelectedCols); // записываем значение
                        sumCells += addNulls + addNewValue;
                    } else enterMessageWarn(popup);
                } else if (result == JOptionPane.YES_OPTION) { // Со смещением
                    String getMessage = JOptionPane.showInputDialog(popup, "Введите что необходимо вставить:");

                    if (getMessage != null && getMessage.replaceAll("[^A-Fa-f0-9]", "").equals(getMessage)) {
                        getMessage = getMessage.toUpperCase();
                        if (getMessage.length() % 2 == 1) getMessage = "0" + getMessage;
                        String[] res = getMessage.split("(?<=\\G..)");

                        int startIndex = myTable.getSelectedRow() * (cols + 1) + myTable.getSelectedColumn();
                        int counterRes = 0;

                        if (tableModel.getValueAt(startIndex / (cols + 1), startIndex % (cols + 1)) != null) { // Если выделенная ячейка ненулевая - делаем смещение - иначе просто вставляем значения
                            for (int i = startIndex, counter = startIndex - myTable.getSelectedRow(); counter <= sumCells; counter++, i++) { // counter - количество байт без первого столбца, i - с первым
                                if (i % (cols + 1) == 0) {
                                    i++;
                                }
                                checkAndAddRow(i, myTable.getRowCount()); // Проверяем нужно ли добавить строку

                                String tableValue = tableModel.getValueAt(i / (cols + 1), i % (cols + 1)).toString();

                                if (counter % cols != 0) {
                                    myTable.setValueAt(res[counterRes], counter / cols,
                                            counter % cols);
                                } else {
                                    myTable.setValueAt(res[counterRes], counter / cols - 1,
                                            counter % cols + cols);
                                }
                                res[counterRes] = tableValue;
                                counterRes = (counterRes + 1) % res.length;

                            }
                            for (int i = sumCells + myTable.getRowCount(), counter = 0; counter < res.length; counter++, i++) { //Записываем "остатки"
                                if (i % (cols + 1) == 0) i++;

                                checkAndAddRow(i, myTable.getRowCount());

                                tableModel.setValueAt(res[counterRes], i / (cols + 1), i % (cols + 1));
                                counterRes = (counterRes + 1) % res.length;
                            }

                            sumCells += res.length;
                        } else {
                            int addNulls = fillNullCells(tableModel, startIndex, cols);
                            int addNewValues = fillWithoutOffset(res, myTable.getSelectedRow(), myTable.getSelectedColumn());
                            sumCells += addNulls + addNewValues;
                        }
                    } else enterMessageWarn(popup);
                }
                canChanged = true;
            }
        });
        JMenuItem cut = new JMenuItem("Cut");
        cut.addActionListener(new AbstractAction() { // Работает также, как удаление со смещением, только сохраняет удалённое в буфер обмена
            @Override
            public void actionPerformed(ActionEvent e) {
                canChanged = false;

                int[] selectedRows = myTable.getSelectedRows();
                int[] selectedCols = myTable.getSelectedColumns();
                StringBuilder cutHex = new StringBuilder();

                for (int row : selectedRows) {
                    for (int col : selectedCols) {
                        if (tableModel.getValueAt(row, col) != null) {
                            cutHex.append(tableModel.getValueAt(row, col));
                            tableModel.setValueAt("DEL", row, col);
                        }
                    }
                }
                StringSelection s = new StringSelection(cutHex.toString());
                cp.setContents(s, s);

                int step = runOfTable(myTable);

                for (int i = sumCells + myTable.getRowCount() - 1, counter = 0; counter < step; counter++, i--) {
                    if (i % (cols + 1) == 0) i--;
                    tableModel.setValueAt(null, i / (cols + 1), i % (cols + 1));
                }

                deleteNullRow(tableModel);
                sumCells -= step;
                canChanged = true;
            }
        });
        JMenuItem geInfo = new JMenuItem("Get bytes info");
        geInfo.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] values = {"2", "4", "8"};

                Object selected = JOptionPane.showInputDialog(null, "What is the target Nicotine level?", "Selection", JOptionPane.PLAIN_MESSAGE, null, values, "0");
                if ( selected != null ) {//null if the user cancels.
                    String selectedString = selected.toString();

                    int startIndex = myTable.getSelectedRow() * (cols + 1) + myTable.getSelectedColumn();
                    StringBuilder bytes = new StringBuilder();

                    for (int i = startIndex, counter = 0; counter < Integer.parseInt(selectedString); counter++, i++) {
                        if (i % (cols + 1) == 0) i++;
                        if (tableModel.getValueAt(i / (cols + 1), i % (cols + 1)) != null) {
                            String tableValue = tableModel.getValueAt(i / (cols + 1), i % (cols + 1)).toString();
                            bytes.append(tableValue);
                        } else {
                            JOptionPane.showMessageDialog(null, "Не получается считать все байты");
                            return;
                        }
                    }

                    long bigIntInfo = parseUnsignedHex(bytes.toString());
                    double bigDecInfo = Double.longBitsToDouble(bigIntInfo);


                    byteInfo.setText("<html>Info about some bytes:" + "<br>   int:" + bigIntInfo + "<br>  double: " + bigDecInfo + "</html>");

                } else {
                    System.out.println("User cancelled");
                }
            }
        });

        popup.add(delete);
        popup.add(add);
        popup.add(cut);
        popup.add(geInfo);
        // Добавить сепаратор

        return popup;
    }
    private int runOfTable (JTable table){ // Перезапись таблицы при удалении со смещением
        int startIndex = table.getSelectedRow() * (cols + 1) + table.getSelectedColumn();
        int step = 0;

        for (int i = startIndex, counter = startIndex - table.getSelectedRow(); counter <= sumCells; counter++, i++) {
            if (i % (cols + 1) == 0) {
                i++;
            }

            String tableValue = tableModel.getValueAt(i / (cols + 1), i % (cols + 1)).toString();

            if (tableValue.equals("DEL")) {
                step += 1;
            } else {
                if ((counter - step) % cols != 0) {
                    myTable.setValueAt(tableValue, (counter - step) / cols, (counter - step) % cols);
                } else {
                    myTable.setValueAt(tableValue, (counter - step) / cols - 1, (counter - step) % cols + cols);
                }
            }
        }

        return step;
    }
    private int fillWithoutOffset (String[] res, int indexSelectedRow, int indexSelectedCol) {
        int addNewValue = 0;
        int numRows = tableModel.getRowCount();

        for (String bytes : res) {
            if (numRows <= indexSelectedRow + 1 && indexSelectedCol == cols) {
                String[] temp = new String[cols];
                temp[0] = String.format("%2X", numRows * cols);
                tableModel.addRow(temp);
                numRows++;
            }

            if (tableModel.getValueAt(indexSelectedRow, indexSelectedCol) == null) addNewValue += 1;
            tableModel.setValueAt(bytes, indexSelectedRow, indexSelectedCol);

            indexSelectedCol += 1;
            if (indexSelectedCol > cols) {
                indexSelectedCol = 1;
                indexSelectedRow += 1;
            }
        }
        return addNewValue;
    }
    private int fillNullCells(DefaultTableModel model, int startIndex, int columns) {
        int counter = 0;
        for (int i = startIndex - 1; counter < columns && i != 0; counter++, i--) {
            if (i % (columns + 1) == 0) i--;
            if (model.getValueAt(i / (columns + 1), i % (columns + 1)) == null) {
                model.setValueAt("00", i / (columns + 1), i % (columns + 1));
            } else return counter;
        }
        return counter;
    }
    private void checkAndAddRow (int index, int numRows) {
        if (index / (cols + 1) + 1 >= myTable.getRowCount() && index % (cols + 1) == cols) {
            String[] temp = new String[cols];
            temp[0] = String.format("%2X", numRows * cols);
            tableModel.addRow(temp);
        }
    }
    private void deleteNullRow(DefaultTableModel model) {
        for (int i = model.getRowCount() - 1; i > 0; i--)
            if (model.getValueAt(i - 1, cols) == null) // мб первое условие и ненужно
                model.removeRow(i);
        if (model.getValueAt(0, 1) == null) isTableEmpty = true;
    }
    private void enterMessageWarn(JPopupMenu popup) {
        JOptionPane.showMessageDialog(popup,
                "Ваше сообщение не может быть вставлено.");
    }
}
