package btdex.ui.orderbook;

import btdex.core.ContractState;
import btdex.ui.CancelOrderDialog;
import btdex.ui.PlaceOrderDialog;
import btdex.ui.PlaceTokenOrderDialog;
import burst.kit.entity.response.AssetOrder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static btdex.locale.Translation.tr;

public class ActionButton extends JButton {

    private static final long serialVersionUID = 1L;

    public ActionButton(OrderBook orderBook, String text, ContractState contract, boolean cancel) {
        this(orderBook, text, null, contract, cancel, false);
    }

    public ActionButton(OrderBook orderBook, String text, AssetOrder order, boolean cancel) {
        this(orderBook, text, order, null, cancel, true);
    }
    public ActionButton(OrderBook orderBook, String text, AssetOrder order, ContractState contract, boolean cancel, boolean isToken) {
        super(text);

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = (JFrame) SwingUtilities.getRoot(orderBook);

                if((isToken && order.getAssetId() == null) ||
                        (!isToken && contract.hasPending())) {
                    JOptionPane.showMessageDialog(getParent(), tr("offer_wait_confirm"),
                            tr("offer_processing"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JDialog dlg = null;
                if(cancel) {
                    dlg = new CancelOrderDialog(f, orderBook.getMarket(), order, contract);
                }
                else {
                    if(isToken)
                        dlg = new PlaceTokenOrderDialog(f, orderBook.getMarket(), order);
                    else
                        dlg = new PlaceOrderDialog(f, orderBook.getMarket(), contract, false);
                }
                dlg.setLocationRelativeTo(orderBook);
                dlg.setVisible(true);

                OrderBook.BUTTON_EDITOR.stopCellEditing();
            }
        });
    }

}
