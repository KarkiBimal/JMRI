package jmri.jmrix.cmri.serial.serialmon;

import jmri.jmrix.cmri.serial.SerialListener;
import jmri.jmrix.cmri.serial.SerialMessage;
import jmri.jmrix.cmri.serial.SerialReply;
import jmri.jmrix.cmri.CMRISystemConnectionMemo;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;

import java.io.File;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import jmri.util.FileUtil;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

/**
 * Frame displaying (and logging) CMRI serial command messages
 *
 * @author Bob Jacobsen Copyright (C) 2001
 * @author Chuck Catania  Copyright (C) 2014, 2016, 2017
 */
public class SerialMonFrame extends jmri.jmrix.AbstractMonFrame implements SerialListener {
    // member declarations
    public boolean loggingEnabled = false;  // true if message logging to a file
    public boolean freezeDisplay  = false;  // true if display is halted

    protected JButton doneButton = new JButton();
    protected JButton logMsgButton = new JButton();
    protected JCheckBox deltaTBox = new JCheckBox();
    protected JButton packetfilterButton = new JButton();  //c2
    String rawDataCheck = this.getClass().getName()+".RawData";
    String timeStampCheck = this.getClass().getName()+".TimeStamp";
    String deltaTCheck = this.getClass().getName()+".DeltaT";
    String alwaysOnTopCheck = this.getClass().getName()+".alwaysOnTop";
    String autoScrollCheck = this.getClass().getName()+".AutoScroll";
    jmri.UserPreferencesManager p;
    
    protected Border packetDisplayBorder = BorderFactory.createEtchedBorder();
    protected Border packetDisplayBorderTitled = BorderFactory.createTitledBorder(packetDisplayBorder,
                                              " ",TitledBorder.LEFT,TitledBorder.ABOVE_TOP);            
    protected long lastTicks = 0L;

    final javax.swing.JFileChooser logFileChooser = new JFileChooser(FileUtil.getUserFilesPath()); //jmri.jmrit.XmlFile.userFileLocationDefault());

    private CMRISystemConnectionMemo _memo = null;

    public SerialMonFrame(CMRISystemConnectionMemo memo) {
        super();
        _memo = memo;
    }
    
    @Override
    public void dispose() { 
        p.setSimplePreferenceState(timeStampCheck, timeCheckBox.isSelected());
        p.setSimplePreferenceState(rawDataCheck, rawCheckBox.isSelected());
        p.setSimplePreferenceState(alwaysOnTopCheck, alwaysOnTopCheckBox.isSelected());
        p.setSimplePreferenceState(autoScrollCheck, !autoScrollCheckBox.isSelected());
        _memo.getTrafficController().removeSerialListener(this);
    }

    @Override
    protected String title() {
        return Bundle.getMessage("SerialCommandMonTitle");
    }

    @Override
    protected void init() {
        // connect to TrafficController
        _memo.getTrafficController().addSerialListener(this);
    }

    @Override
    public void initComponents() throws Exception {

        p = jmri.InstanceManager.getDefault(jmri.UserPreferencesManager.class);
        // the following code sets the frame's initial state

        clearButton.setText("Clear screen");
        clearButton.setVisible(true);
        clearButton.setToolTipText("Clear monitoring history");

        freezeButton.setText("Freeze Display");
        freezeButton.setVisible(true);
        freezeButton.setToolTipText("Start/Stop display scrolling");

        enterButton.setText("Add Message");
        enterButton.setVisible(true);
        enterButton.setToolTipText("Add a text message to the log");

        monTextPane.setVisible(true);
        monTextPane.setToolTipText("Command and reply monitoring information appears here");
        monTextPane.setEditable(false);

       // Add document listener to scroll to end when modified if required
        monTextPane.getDocument().addDocumentListener(new DocumentListener() {

            // References to the JTextArea and JCheckBox
            // of this instantiation
            JTextArea ta = monTextPane;
            JCheckBox chk = autoScrollCheckBox;

            @Override
            public void insertUpdate(DocumentEvent e) {
                doAutoScroll(ta, chk.isSelected());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                doAutoScroll(ta, chk.isSelected());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                doAutoScroll(ta, chk.isSelected());
            }
        });

        entryField.setToolTipText("Enter text here, then click button to include it in log");

        // fix a width for current character set
        JTextField t = new JTextField(80);
        int x = jScrollPane1.getPreferredSize().width+t.getPreferredSize().width;
        int y = jScrollPane1.getPreferredSize().height+10*t.getPreferredSize().height;

        jScrollPane1.getViewport().add(monTextPane);
        jScrollPane1.setPreferredSize(new Dimension(x, y));
        jScrollPane1.setVisible(true);
        jScrollPane1.setBorder(packetDisplayBorderTitled); 
                
        logMsgButton.setText("Start Logging");
        logMsgButton.setVisible(true);
        logMsgButton.setToolTipText("Start/Stop message logging to file");

        rawCheckBox.setText("Show raw data");
        rawCheckBox.setVisible(true);
        rawCheckBox.setToolTipText("If checked, show the raw traffic in hex");
        rawCheckBox.setSelected(p.getSimplePreferenceState(rawDataCheck));

        timeCheckBox.setText("Show timestamps");
        timeCheckBox.setVisible(true);
        timeCheckBox.setToolTipText("If checked, show timestamps before each message");
        timeCheckBox.setSelected(p.getSimplePreferenceState(timeStampCheck));
        
        deltaTBox.setText("w/Time Diff");
        deltaTBox.setVisible(true);
        deltaTBox.setToolTipText("If checked, show time difference in mS");
        deltaTBox.setSelected(p.getSimplePreferenceState(deltaTCheck));
        
        alwaysOnTopCheckBox.setText("Window always on Top");
        alwaysOnTopCheckBox.setVisible(true);
        alwaysOnTopCheckBox.setToolTipText("If checked, this window be always be displayed in front of any other window");
        alwaysOnTopCheckBox.setSelected(p.getSimplePreferenceState(alwaysOnTopCheck));
        setAlwaysOnTop(alwaysOnTopCheckBox.isSelected());

        autoScrollCheckBox.setText("Auto scroll");
        autoScrollCheckBox.setVisible(true);
        autoScrollCheckBox.setToolTipText("If checked, always scroll to the latest log entry");
        autoScrollCheckBox.setSelected(!p.getSimplePreferenceState(autoScrollCheck));

        openFileChooserButton.setText("Choose log file");
        openFileChooserButton.setVisible(true);
        openFileChooserButton.setToolTipText("Click here to select a new output log file");

        packetfilterButton.setText("Filter Packets");
        packetfilterButton.setVisible(true);
        packetfilterButton.setToolTipText("Opens CMRInet Packet Filter");

        doneButton.setText("Done");
        doneButton.setVisible(true);
        doneButton.setToolTipText("Exit Serial Monitor");

        setTitle(title());
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // add items to GUI
        getContentPane().add(jScrollPane1);

        JPanel paneA = new JPanel();
        paneA.setLayout(new BoxLayout(paneA, BoxLayout.Y_AXIS));
        
        JPanel pane3 = new JPanel();
        pane3.setLayout(new BoxLayout(pane3, BoxLayout.X_AXIS));
        pane3.add(openFileChooserButton);

        pane3.add(logMsgButton);
        paneA.add(pane3);

        JPanel pane2 = new JPanel();
        pane2.setLayout(new BoxLayout(pane2, BoxLayout.X_AXIS));
        pane2.add(rawCheckBox);
        pane2.add(timeCheckBox);
        pane2.add(deltaTBox);
        pane2.add(alwaysOnTopCheckBox);
        pane2.add(packetfilterButton);
        paneA.add(pane2);
        
        JPanel pane1 = new JPanel();
        pane1.setLayout(new BoxLayout(pane1, BoxLayout.X_AXIS));
        pane1.add(clearButton);
        pane1.add(freezeButton);
        
        pane1.add(Box.createRigidArea(new Dimension(50,0)));
        pane1.add(doneButton);
        paneA.add(pane1);
        
        JPanel pane4 = new JPanel();
        pane4.setLayout(new BoxLayout(pane4, BoxLayout.X_AXIS));
        pane4.add(enterButton);
        pane4.add(entryField);
        paneA.add(pane4);

        getContentPane().add(paneA);

        // connect actions to buttons
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearButtonActionPerformed(e);
            }
        });

        freezeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                freezeButtonActionPerformed(e);
            }
        });
         
       logMsgButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logButtonActionPerformed(e);
            }
        });
       
        openFileChooserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooserButtonActionPerformed(e);
            }
        });

        enterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterButtonActionPerformed(e);
            }
        });

        alwaysOnTopCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAlwaysOnTop(alwaysOnTopCheckBox.isSelected());
            }
        });

        autoScrollCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAutoScroll(monTextPane, autoScrollCheckBox.isSelected());
            }
        });

         packetfilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPacketFilterPerformed(e);
            }
        });
         
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }

        });

       // set file chooser to a default
        logFileChooser.setSelectedFile(new File("monitorLog.txt"));

        // connect to data source
        init();

        // add help menu to window
        addHelpMenu();

        // prevent button areas from expanding
        pack();
        paneA.setMaximumSize(paneA.getSize());
        pack();
    }
        @Override
        public void nextLine(String line, String raw) {
        // handle display of traffic
        // line is the traffic in 'normal form', raw is the "raw form"
        // Both should be one or more well-formed lines, e.g. end with \n
        StringBuffer sb = new StringBuffer(120);

        // display the timestamp if requested
        if ( timeCheckBox.isSelected() ) {
            long curTicks = System.currentTimeMillis();  //c2
            sb.append(df.format(curTicks));
            
            if (deltaTBox.isSelected())   //c2
            {
              if (lastTicks == 0) lastTicks = curTicks;
              sb.append(" [").append(Long.toString(curTicks-lastTicks)).append("]");
              lastTicks = curTicks;
            }
            else
              lastTicks = 0L;
            
            sb.append(": ");
        }

//         if ( timeCheckBox.isSelected() ) {
//             sb.append(df.format(new Date())).append( ": " ) ;
//        }

        // display the raw data if requested
        if ( rawCheckBox.isSelected() ) {
            sb.append( '[' ).append(raw).append( "]  " );
        }

        // display decoded data
        sb.append(line);
//        synchronized( self )
        {
            linesBuffer.append( sb.toString() );
        }

        // if not frozen, display it in the Swing thread
//        if (!freezeButton.isSelected()) {
        if (!freezeDisplay) {
            Runnable r = new Runnable() {
                public void run() {
//                    synchronized( self )
                    {
                        monTextPane.append( linesBuffer.toString() );
                        int LineCount = monTextPane.getLineCount() ;
                        if( LineCount > MAX_LINES )
                        {
                            LineCount -= MAX_LINES ;
                            try {
                                int offset = monTextPane.getLineStartOffset(LineCount);
                                monTextPane.getDocument().remove(0, offset ) ;
                            }
                            catch (BadLocationException ex) {
                            }
                        }
                        linesBuffer.setLength(0) ;
                    }
                }
            };
            javax.swing.SwingUtilities.invokeLater(r);
        }

        // if requested, log to a file.
        if (logStream != null) {
            synchronized (logStream) {
                String logLine = sb.toString();
                if (!newline.equals("\n")) {
                    // have to massage the line-ends
                    int i = 0;
                    int lim = sb.length();
                    StringBuffer out = new StringBuffer(sb.length()+10);  // arbitrary guess at space
                    for ( i = 0; i<lim; i++) {
                        if (sb.charAt(i) == '\n')
                            out.append(newline);
                        else
                            out.append(sb.charAt(i));
                    }
                    logLine = out.toString();
                }
                logStream.print(logLine);
            }
        }
    }

    String newline = System.getProperty("line.separator");

    /**
     * Method to position caret at end of JTextArea ta when
     * scroll true.
     * @param ta Reference to JTextArea
     * @param scroll True to move to end
     */
    private void doAutoScroll(final JTextArea ta, final boolean scroll) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int len = ta.getText().length();
                if (scroll) {
                    ta.setCaretPosition(len);
                } else if (ta.getCaretPosition()==len && len>0) {
                    ta.setCaretPosition(len-1);
                }        
            }
        });
    }
    /**
     * Method to test for packet logging
     * @param OnOff 
     */
    public void setMsgLogging( boolean OnOff )
    {
        loggingEnabled = OnOff;
    }    
    public boolean getMsgLogging()
    {
        return loggingEnabled;       
    }
    /**
     * Toggle the packet logging function with one button
     * @param e 
     */
    public synchronized void logButtonActionPerformed(java.awt.event.ActionEvent e) {
        // start logging by creating the stream
        if (getMsgLogging()) // Logging enabled, stop logging
        {
            if (logStream!=null)
            {
                synchronized (logStream)
                {
                 logStream.flush();
                 logStream.close();
                }   
           }
           logStream = null;
           logMsgButton.setText("Start Logging");
           setMsgLogging( false );
        }
        else
        if ( logStream==null)
        {  // successive clicks don't restart the file
            // start logging
            try {
                logStream = new PrintStream (new FileOutputStream(logFileChooser.getSelectedFile()));
                logMsgButton.setText("Stop Logging");
                setMsgLogging( true );

            } catch (Exception ex) {
                log.error("exception "+ex);
            }
        }
    }
    
    public synchronized void freezeButtonActionPerformed(java.awt.event.ActionEvent e)
    {
        // freeze/resume the monitor output
        if (freezeDisplay) 
        {
           freezeButton.setText("Freeze Display");
           freezeDisplay = false;
        }
        else
        {  
           freezeButton.setText("Resume Display");
           freezeDisplay = true;
        }
    }
    
    @Override
    public void openFileChooserButtonActionPerformed(java.awt.event.ActionEvent e) {
        // start at current file, show dialog
        int retVal = logFileChooser.showSaveDialog(this);

        // handle selection or cancel
        if (retVal == JFileChooser.APPROVE_OPTION) {
            boolean loggingNow = (logStream != null);
            //stopLogButtonActionPerformed(e);  // stop before changing file
            logButtonActionPerformed(e);  // stop before changing file
            //File file = logFileChooser.getSelectedFile();
            // if we were currently logging, start the new file
//            if (loggingNow) startLogButtonActionPerformed(e);
            if (loggingNow) logButtonActionPerformed(e);
        }
    }

    public void openPacketFilterPerformed(ActionEvent e) {
		// create a SerialFilterFrame
		SerialFilterFrame f = new SerialFilterFrame(_memo);
		try {
			f.initComponents();
			}
		catch (Exception ex) {
			log.warn("SerialFilterAction starting SerialFilterFrame: Exception: "+ex.toString());
			}
		f.setVisible(true);
	}
    

    @Override 
    public synchronized void message(SerialMessage l) {  // receive a message and log it
        // check for valid length
        if (l.getNumDataElements() < 2) {
            nextLine("Truncated message of length " + l.getNumDataElements() + "\n",
                    l.toString());
            return;
        } else if (l.isPoll()) {
            nextLine("Poll ua=" + l.getUA() + "\n", l.toString());
        } else if (l.isXmt()) {
            StringBuilder sb = new StringBuilder("Transmit ua=");
            sb.append(l.getUA());
            sb.append(" OB=");
            for (int i = 2; i < l.getNumDataElements(); i++) {
                sb.append(Integer.toHexString(l.getElement(i) & 0x000000ff));
                sb.append(" ");
            }
            sb.append("\n");
            nextLine(new String(sb), l.toString());
        } else if (l.isInit()) {
            StringBuilder sb = new StringBuilder("Init ua=");
            sb.append(l.getUA());
            sb.append(" type=");
            sb.append((char) l.getElement(2));
            int len = l.getNumDataElements();
            if (len >= 5) {
                sb.append(" DL=");
                sb.append(l.getElement(3) * 256 + l.getElement(4));
            }
            if (len >= 6) {
                sb.append(" NS=");
                sb.append(l.getElement(5));
                sb.append(" CT: ");
                for (int i = 6; i < l.getNumDataElements(); i++) {
                    sb.append(Integer.toHexString(l.getElement(i) & 0x000000ff));
                    sb.append(" ");
                }
            }
            sb.append("\n");
            nextLine(new String(sb), l.toString());
        } else {
            nextLine("unrecognized cmd: \"" + l.toString() + "\"\n", "");
        }
    }

/*
  /********************
     Transmit Packets
  *********************
    public synchronized void message(SerialMessage l) 
    { 
       int aPacketTypeID = 0;
     // Test if message is for a monitored node
     //----------------------------------------
		
 //      SerialNode monitorNode = null;       
//       monitorNode = (SerialNode) _memo.l.getUA());
       SerialNode monitorNode = (SerialNode)_memo.getTrafficController().getNode(l.getUA());
		
       if (monitorNode == null) return;       
       if (!monitorNode.getMonitorNodePackets()) return;

       aPacketTypeID = l.getElement(1);
//       if (aPacketTypeID == monitorNode.monPktTypeID[SerialNode.monPktTransmit])
//        System.out.println("Saw "+l.getElement(1)+":"+aPacketTypeID);
        
	 // check for valid length
        if (l.getNumDataElements() < 2)
	{
            nextLine("Truncated message of length "+l.getNumDataElements()+"\n",l.toString());
            return;
        }
        		
	switch(aPacketTypeID)
	{
	case 0x50:        // (P) Poll
            if(monitorNode.getMonitorPacketBit(SerialFilterFrame.monPktPoll))
            nextLine("Poll ua="+l.getUA()+"\n", l.toString());
	break;

	case 0x54:        // (T) Transmit
            if (monitorNode.getMonitorPacketBit(SerialFilterFrame.monPktTransmit))
            {
                StringBuilder sb = new StringBuilder("Transmit ua=");
                sb.append(l.getUA());
                sb.append(" OB=");
                for (int i=2; i<l.getNumDataElements(); i++)
                {
                    sb.append(Integer.toHexString(l.getElement(i)&0x000000ff).toUpperCase());  //c2
                    sb.append(" ");
                }   
                sb.append("\n");
                nextLine(new String(sb), l.toString());
            }
	break;

	case 0x49:        // (I) Initialize 
            if(monitorNode.getMonitorPacketBit(SerialFilterFrame.monPktInit))
            {
                StringBuilder sb = new StringBuilder("Init ua=");
                sb.append(l.getUA());
                sb.append(" type=");
                int ndp=l.getElement(2); // ndp node type
                sb.append((char)ndp);
                int len = l.getNumDataElements();

		switch (ndp)
		{
                // SMINI/SUSIC/USIC
                    case SerialNode.NDP_USICSUSIC24:
                    case SerialNode.NDP_USICSUSIC32:
                    case SerialNode.NDP_SMINI:
					
                    if (len>=5) 
                    {
                        sb.append(" DL=");
                        sb.append(l.getElement(3)*256+l.getElement(4));
                    }
                
                    if (len>=6) 
                    {
                        sb.append(" NS=");
                        sb.append(l.getElement(5));
                        sb.append(" CT: ");
                        for (int i=6; i<l.getNumDataElements(); i++)
                        {
                            sb.append(Integer.toHexString(l.getElement(i)&0x000000ff).toUpperCase()); //c2
                            sb.append(" ");
                        }
                    }
                    break;
					
		// CPNODE
                    case SerialNode.NDP_CPNODE:
                    if (len>=5) 
                    {                    
                        sb.append(" DL=");
                        sb.append(l.getElement(3)*256+l.getElement(4));
                    }
                    sb.append(" Opts=");
                    int i=5;
                    while (i<l.getNumDataElements())
                    {
                        if (l.getElement(i) != 16) // skip DLE
                        {    
                            sb.append(Integer.toHexString(l.getElement(i)&0x000000ff).toUpperCase()); //c2
                            sb.append(" ");
                        }
                    i++;
                    }
                    break;
					
                // CPMEGA
                    case SerialNode.NDP_CPMEGA:
                    if (len>=5) 
                    {                    
                        sb.append(" DL=");
                        sb.append(l.getElement(3)*256+l.getElement(4));
                    }
                    sb.append(" Opts=");
                    i=5;
                    while (i<l.getNumDataElements())
                    {
			if (l.getElement(i) != 16) // skip DLE
			{    
                            sb.append(Integer.toHexString(l.getElement(i)&0x000000ff).toUpperCase()); //c2
                            sb.append(" ");
			}
                    i++;
                    }
                    break;
					
                    default:
			sb.append("Unrecognized node type NDP: ["+ndp+"] ");
                    break;

                } //ndp case
				
            sb.append("\n");
            nextLine(new String(sb), l.toString());
            }
        break;
		
	default: 
            nextLine("Unrecognized cmd: \""+l.toString()+"\"\n", "");
          
        }  // end packet ID case
    }

 */   

    @Override

    public synchronized void reply(SerialReply l) {  // receive a reply message and log it
        // check for valid length
        if (l.getNumDataElements() < 2) {
            nextLine("Truncated reply of length " + l.getNumDataElements() + "\n",
                    l.toString());
            return;
        } else if (l.isRcv()) {
            StringBuilder sb = new StringBuilder("Receive ua=");
            sb.append(l.getUA());
            sb.append(" IB=");
            for (int i = 2; i < l.getNumDataElements(); i++) {
                sb.append(Integer.toHexString(l.getElement(i) & 0x000000ff));
                sb.append(" ");
            }
            sb.append("\n");
            nextLine(new String(sb), l.toString());
        } else {
            nextLine("unrecognized rep: \"" + l.toString() + "\"\n", "");
        }
    }

/*
  /********************
     Receive Packets
  *********************
    public synchronized void reply(SerialReply l) 
    { 
       int aPacketTypeID = 0;

//       SerialNode monitorNode = null;       
//       monitorNode = (SerialNode) _memo.getNodeFromAddress(l.getUA());
       SerialNode monitorNode = (SerialNode)_memo.getTrafficController().getNode(l.getUA());		
       if (monitorNode == null) return;
       if (!monitorNode.getMonitorNodePackets()) return; 
		
	   aPacketTypeID = l.getElement(1);

        // check for valid length
        if (l.getNumDataElements() < 2) 
        {
            nextLine("Truncated reply of length "+l.getNumDataElements()+"\n",l.toString());
//       CMRInetMetricsData.incMetricErrValue( CMRInetMetricsData.CMRInetMetricTruncReply );
		return;
        }		
	switch(aPacketTypeID)
	{
            case 0x52:  // (R) Receive (poll reply)
                if(monitorNode.getMonitorPacketBit(SerialFilterFrame.monPktTransmit))
                {
                    StringBuilder sb = new StringBuilder("Receive ua=");
                    sb.append(l.getUA());
                    sb.append(" IB=");
                    for (int i=2; i<l.getNumDataElements(); i++)
                    {
                        sb.append(Integer.toHexString(l.getElement(i)&0x000000ff).toUpperCase());  //c2
                        sb.append(" ");
                    }
                sb.append("\n");
                nextLine(new String(sb), l.toString());
                }
            break; 
				
            case 0x45:  // (E) EOT c2
                if(monitorNode.getMonitorPacketBit(SerialFilterFrame.monPktTransmit))
                {
                StringBuilder sb = new StringBuilder("Receive ua=");
                sb.append(l.getUA());
                sb.append(" eot");            
                sb.append("\n");
                nextLine(new String(sb), l.toString());
                }
            break; 
				
            default:
//                CMRInetMetricsData.incMetricErrValue( CMRInetMetricsData.CMRInetMetricUnrecResponse );
                nextLine("Unrecognized response: \""+l.toString()+"\"\n", "");
            break;
        }
    }
 */ 
    volatile PrintStream logStream = null;

    // to get a time string
    DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

    StringBuffer linesBuffer = new StringBuffer();
    static private int MAX_LINES = 500 ;
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SerialMonFrame.class.getName());

}

