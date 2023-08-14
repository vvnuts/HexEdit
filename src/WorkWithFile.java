import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WorkWithFile {
    private final String path;
    private int quaNumberB = 0;
    public WorkWithFile(String path) {
        this.path = path;
    }
    public int getQuaNumberB() {
        return quaNumberB;
    }
    public void setQuaNumberB(int quaNumberB) {
        this.quaNumberB = quaNumberB;
    }
    public void readFile(DefaultTableModel model, int columnLength) {
        try (InputStream input = Files.newInputStream(Paths.get(path)); BufferedInputStream bis = new BufferedInputStream(input)) {
            int symbol;
            String[] row = new String[columnLength + 1];

            while ((symbol = bis.read()) != -1) { // считываем побайтово в строку, при заполнении её - добаляем таблицу
                int temp = quaNumberB % columnLength;
                if (temp == 0) row[temp] = String.format("%2X", quaNumberB);
                row[temp + 1] = String.format("%02X", symbol);
                quaNumberB += 1;
                if (quaNumberB % columnLength == 0) {
                    model.addRow(row);
                }
            }

            String[] lastRow = new String[quaNumberB % columnLength + 1];
            System.arraycopy(row, 0, lastRow, 0, lastRow.length);
            model.addRow(lastRow);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write(DefaultTableModel tableModel, int cols) { // Записываем побайтово файл данные из ячеек
        try (OutputStream output = Files.newOutputStream(Paths.get(path)); BufferedOutputStream bos = new BufferedOutputStream(output)) {
            for (int i = 1, counter = 0; counter < quaNumberB; counter++, i++) {
                if (i % (cols + 1) == 0) i++;
                String tableValue = tableModel.getValueAt(i / (cols + 1), i % (cols + 1)).toString();
                bos.write(convertHexToBytes(tableValue));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Что-то не так. Файл не сохранен");
        }
    }
    public byte[] convertHexToBytes(String hex) {
        if (hex.length() % 2 == 1) hex = "0" + hex;
        return javax.xml.bind.DatatypeConverter.parseHexBinary(hex);
    }
}
