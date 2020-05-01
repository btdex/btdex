package btdex.ui.orderbook;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
    private static final long serialVersionUID = 1L;

    private Component but;

    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        return but = (Component) value;
    }

    public Object getCellEditorValue() {
        return but;
    }

    // validate the input
    public boolean stopCellEditing() {
        return super.stopCellEditing();
    }
}
