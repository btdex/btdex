package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

import org.apache.commons.text.StringEscapeUtils;

import btdex.core.Globals;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class ChatPanel extends JPanel implements ActionListener, ListSelectionListener {
	private static final long serialVersionUID = 144264934272036916L;
	
	private static final int MAX_LENGTH = 280;

	private static final String STYLE_SHEET =
            ".container-friend {\n" + 
            "  border: 2px solid #bbbbbb;\n" + 
			"  font-family: Dialog;\n" + 
            "  padding: 4px;\n" + 
            "  margin: 4px 0;\n" + 
            "}\n" + 
            ".container-you {\n" + 
			"  font-family: Dialog;\n" + 
            "  padding: 4px;\n" + 
            "  margin: 4px 0;\n" + 
            "  text-align: left;\n" + 
            "}\n" + 
            ".time {\n" + 
			"  font-family: Dialog;\n" + 
            "  font-size: small;\n" + 
            "}\n"
            ;
    private static final String HTML_FORMAT
            = "<style>" + STYLE_SHEET + "</style>"
               + "<div id=content> </div>"
            ;

    private static final String FRIEND_CHAT_FORMAT = "<div class=\"container-friend\">\n" + 
    		"  <div>%s</div>\n" + 
    		"  <div class=\"time\">BURST-XXXX-X 11:00</div>\n" + 
    		"</div>";
    private static final String YOU_CHAT_FORMAT = "<div class=\"container-you\">\n" + 
    		"  <div align=\"right\">%s</div>\n" + 
    		"  <div align=\"right\" class=\"time\">You 11:01</div>\n" + 
    		"</div>";

    private JTextField inputField;
    private JTextPane displayField;
    
    private JPasswordField pinField;
    private char []pin;

	private JList<String> addressList;

	private JButton btnSend;

	private JScrollPane scrollPane;

    public ChatPanel() {
    	super(new BorderLayout());

		JPanel addressPanel = new JPanel(new BorderLayout());
		addressPanel.setBorder(BorderFactory.createTitledBorder("Your contacts"));
		addressList = new JList<>();
		addressList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		addressList.setPreferredSize(new Dimension(300, 300));
		addressPanel.add(addressList, BorderLayout.CENTER);		
		add(addressPanel, BorderLayout.LINE_START);
		addressList.addListSelectionListener(this);
		
		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

    	JPanel panelSendMessage = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setToolTipText("Enter your message");
        panelSendMessage.add(new Desc("Enter your message", inputField), BorderLayout.CENTER);
        inputField.addActionListener(this);
        inputField.setEnabled(false);

        btnSend = new JButton("");
		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.PAPER_PLANE_O, 24, btnSend.getForeground());
        btnSend.setIcon(sendIcon);
        btnSend.setToolTipText("Send your message");
        btnSend.addActionListener(this);
        btnSend.setEnabled(false);
        panelSendMessage.add(new Desc(" ", btnSend), BorderLayout.EAST);

        displayField = new JTextPane();
        displayField.setContentType("text/html");
        displayField.setEditable(false);
        displayField.setText(HTML_FORMAT);

        scrollPane = new JScrollPane(displayField);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel panelCenter = new JPanel(new BorderLayout());
        panelCenter.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(panelCenter, BorderLayout.CENTER);
        
        panelCenter.add(scrollPane, BorderLayout.CENTER);
        panelCenter.add(panelSendMessage, BorderLayout.SOUTH);
        
        setSize(280, 400);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	if(e.getSource() == btnSend || e.getSource() == inputField) {
    		if(inputField.getText().length() == 0)
    			return;
    		if(inputField.getText().length() > MAX_LENGTH) {
				Toast.makeText(Main.getInstance(), "Maximum length of a message is " + MAX_LENGTH +
						" characters.", Toast.Style.ERROR).display(inputField);
				return;
    		}
    		appendMeMyMessage(inputField.getText());
    		inputField.setText("");
    		inputField.requestFocus();
    	}
    	if(e.getSource() == pinField) {
    		pin = pinField.getPassword();
    		if(!Globals.getInstance().checkPIN(pin)) {
				Toast.makeText(Main.getInstance(), "Invalid PIN", Toast.Style.ERROR).display(pinField);
				return;
    		}
    	}
    }

    private String prepareHtmlString(String rawContent) {
        return StringEscapeUtils.escapeXml10(rawContent);
    }

    private void appendRawHtmlChatContent(String html) {
        HTMLDocument document = (HTMLDocument) displayField.getDocument();
        Element contentElement = document.getElement("content");
        try {
            if (contentElement.getElementCount() > 0) {
                Element lastElement = contentElement.getElement(contentElement.getElementCount() - 1);
                document = (HTMLDocument) contentElement.getDocument();
                document.insertAfterEnd(lastElement, html);
            } else {
                document.insertAfterStart(contentElement, html);
            }
        } catch (BadLocationException | IOException ignored) {
        }
        
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            if(bar!=null)
            	bar.setValue(bar.getMaximum());
        });
    }
    
    public void clearMessages() {
        displayField.setText(HTML_FORMAT);
    }

    public void appendFriendMessage(String message) {
        String htmlMessage = prepareHtmlString(message);
        appendRawHtmlChatContent(String.format(FRIEND_CHAT_FORMAT, htmlMessage));
    }

    public void appendMeMyMessage(String message) {
        String htmlMessage = prepareHtmlString(message);
        appendRawHtmlChatContent(String.format(YOU_CHAT_FORMAT, htmlMessage));
    }

    public static void main(String[] args) {
		IconFontSwing.register(FontAwesome.getIconFont());

		JFrame f = new JFrame("Chat test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ChatPanel chat = new ChatPanel();
		f.getContentPane().add(chat);
		
		chat.appendFriendMessage("Message from friend");
		chat.appendMeMyMessage("Message from me");
		chat.appendFriendMessage("Another from friend");
		chat.appendMeMyMessage("Another from me, but this is a very long message, very very very long message");
		
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		
	}
}
