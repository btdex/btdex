package btdex.ui.orderbook;

import btdex.core.ContractState;
import btdex.core.Market;
import btdex.ui.CancelOrderDialog;
import btdex.ui.PlaceOrderDialog;
import btdex.ui.PlaceTokenOrderDialog;
import burst.kit.entity.response.AssetOrder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static btdex.locale.Translation.tr;

public class ActionButton extends JButton {

    private static final long serialVersionUID = 1L;
    private static Logger logger = LogManager.getLogger();

    public ActionButton(JPanel panel, Market market, String text, ContractState contract, boolean cancel) {
        this(panel, market, text, null, contract, false, cancel, false);
    }

    public ActionButton(JPanel panel, Market market, String text, AssetOrder order, boolean cancel) {
        this(panel, market, text, order, null, false, cancel, true);
    }
    
    public ActionButton(JPanel panel, Market market, String text, AssetOrder order, boolean ask, boolean cancel) {
        this(panel, market, text, order, null, ask, cancel, true);
    }
    
    public ActionButton(JPanel panel, Market market, String text, AssetOrder order, ContractState contract, boolean ask, boolean cancel, boolean isToken) {
        super(text);

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame f = (JFrame) SwingUtilities.getRoot(panel);
                BookTable.BUTTON_EDITOR.stopCellEditing();
                
                if(!isToken && contract == null) {
                	// new smart contract offer offer
    				JDialog dlg = new PlaceOrderDialog(f, market, null, !ask);
    				dlg.setLocationRelativeTo(panel);
    				dlg.setVisible(true);
    				return;
                }

                if((isToken && order.getAssetId() == null) ||
                        (!isToken && contract.hasPending())) {
                    JOptionPane.showMessageDialog(getParent(), tr("offer_wait_confirm"),
                            tr("offer_processing"), JOptionPane.WARNING_MESSAGE);
					logger.debug("Showing WARNING_MESSAGE");
                    return;
                }

                JDialog dlg = null;
                if(cancel) {
                    dlg = new CancelOrderDialog(f, market, order, contract);
                }
                else {
                    if(isToken)
                        dlg = new PlaceTokenOrderDialog(f, market, order, ask);
                    else
                        dlg = new PlaceOrderDialog(f, market, contract, false);
                }
                dlg.setLocationRelativeTo(panel);
                dlg.setVisible(true);
            }
        });
    }

}
