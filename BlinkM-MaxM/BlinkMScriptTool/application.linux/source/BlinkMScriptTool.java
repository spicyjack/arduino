import processing.core.*; import javax.swing.*; import java.util.regex.*; import javax.swing.border.*; import javax.swing.plaf.metal.*; import processing.serial.*; import javax.swing.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class BlinkMScriptTool extends PApplet {//
// BlinkMScriptTool.pde --  Load/Save BlinkM light scripts in text format
//
//   This Processing sketch assumes it is communicating to a BlinkM 
//   via an Arduino "BlinkMCommunicator" sketch.
//   
//   You can use this download the BlinkMSequencer-creatd light scripts
//   from a BlinkM.  Or to reset a BlinkM to its default light script.
//
//   Note: it only loads files with .txt extensions, so be sure to save your
//         files as that.
//
// 2008, Tod E. Kurt, ThingM, http://thingm.com/
//
//

  // even tho most of the stuff we're doing is AWT

      // for silly borders on buttons
  // for look-n-feel stuff

boolean debug = true;

String strToParse =  
  "// Edit your BlinkM light script here. \n"+
  "// Or load one up from a text file.  \n"+
  "// Or read the one stored on a BlinkM.\n"+
  "// Then save your favorite scripts to a text files\n"+
  "// Several example scripts are stored in this sketch's 'data' directory.\n"+
  "// Make sure you have BlinkMCommunicator installed on your Arduino.\n"+
  "//\n"+
  "// Here's an example light script. It's the default BlinkM script.\n\n"+
  "{  // dur, cmd,  arg1,arg2,arg3\n"+
  "    {  1, {'f',   10,0x00,0x00}},  // set color_step (fade speed) to 10\n"+
  "    {100, {'c', 0xff,0xff,0xff}},  // bright white\n"+
  "    { 50, {'c', 0xff,0x00,0x00}},  // red \n"+
  "    { 50, {'c', 0x00,0xff,0x00}},  // green\n"+
  "    { 50, {'c', 0x00,0x00,0xff}},  // blue \n"+
  "    { 50, {'c', 0x00,0x00,0x00}},  // black (off)\n"+
  "}\n\n";

ArrayList scriptLines;     // contains a list of BlinkMScriptLine objects
int maxScriptLength = 49;  // max the EEPROM on BlinkM can hold
BlinkMScriptLine nullScriptLine = new BlinkMScriptLine( 0,(char)0x00,0,0,0);

BlinkMComm blinkmComm;

ScriptToolFrame stf;
JFileChooser fc;
JButton disconnectButton;
JTextArea editArea;  // contains the raw text of the script
JTextField posText;

int mainWidth = 740;
int mainHeight = 480;
Font monoFont = new Font("Monospaced", Font.PLAIN, 14); // all hail fixed width
Font monoFontSm = new Font("Monospaced", Font.PLAIN, 9); 
Color backColor = new Color(150,150,150);


//
// Processing's setup()
//
public void setup() {
  size(100, 100);   // Processing's frame, we'll turn this off in a bit
  blinkmComm = new BlinkMComm(this);
  setupGUI();
}

//
// Processing's draw()
// Here we're using it as a cheap way to finish setting up our other window
// and as a simple periodic loop to deal with disconnectButton state
// (could write a handler for that, but i'm lazy)
//
public void draw() {
  // we can only do this after setup
  if( frameCount < 60 ) {
    super.frame.setVisible(false);  // turn off Processing's frame
    super.frame.toBack();
    stf.toFront();
  }
  // auto-toggle disconnect button's clickability based on connectedness
  disconnectButton.setEnabled( blinkmComm.isConnected() );
}

// debug!
//void serialEvent(Serial p) {
//  print( hex(p.read(),2)+"," );
//}

// this class is bound to the GUI buttons below
// it triggers the four main functions
class MyActionListener implements ActionListener{
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if( cmd == null ) return;
    if( cmd.equals("stopScript")) {
      if( !connectIfNeeded() ) return;
      blinkmComm.stopScript();
    }
    else if( cmd.equals("playScript")) {
      int pos = 0;
      String s = posText.getText().trim();
      try { pos = Integer.parseInt(s);} catch(Exception nfe){}
      if( pos < 0 ) pos = 0;
      println("playing at position "+pos);
      if( !connectIfNeeded() ) return;
      blinkmComm.playScript(0,0,pos);
    }
    else if( cmd.equals("saveFile") ) {
      saveFile();
    }
    else if( cmd.equals("loadFile") ) {
      loadFile();
    }
    else if( cmd.equals("sendBlinkM") ) {
      sendToBlinkM();
    }
    else if( cmd.equals("recvBlinkM") ) {
      receiveFromBlinkM();
    }
    else if( cmd.equals("disconnect") ) { 
      blinkmComm.disconnect();
    }
    else if( cmd.equals("inputs") ) {
      showInputs();
    }
  }
}

// pop up the connect dialog box if we need to
public boolean connectIfNeeded() {
  if( !blinkmComm.isConnected() ) {
    if( blinkmComm.doConnectDialog() == false ) 
      return false;
    blinkmComm.pause(2000);  // wait for things to settle after connect
  }
  return true; // connect successful?
}

// set a script to blinkm
public void sendToBlinkM() {
  String[] rawlines = editArea.getText().split("\n");
  scriptLines = parseScript(rawlines);
  if(debug) println( scriptLinesToString(scriptLines) );
    
  if( !connectIfNeeded() ) return;

  // update the text area with the parsed script
  String str = scriptLinesToString(scriptLines);
  str = "// Uploaded to BlinkM on "+(new Date())+"\n" + str;
  editArea.setText( str );
    
  println("sending!...");
  int len = scriptLines.size();
  for( int i=0; i< len; i++ ) {
    blinkmComm.writeScriptLine( i, (BlinkMScriptLine)scriptLines.get(i));
  }
  // hack to get around fact we can't read back length
  if( len < maxScriptLength ) {  
    blinkmComm.writeScriptLine( len, nullScriptLine );
  }
  blinkmComm.setScriptLengthRepeats( 0, len, 0 );
  blinkmComm.setStartupParamsDefault();
  blinkmComm.playScript();
}

// download a script from a blinkm
public void receiveFromBlinkM() {
  if( !connectIfNeeded() ) return;
  println("receiving!...");
  BlinkMScriptLine line;
  String str = "{\n";
  int i;
  for( i = 0; i< maxScriptLength; i++ ) {
    line = blinkmComm.readScriptLine( 0, i );
    if( (line.dur == 0xff && line.cmd == 0xff ) || 
        (line.dur == 0 && line.cmd == 0) ) {
      println("bad script line at pos "+i+", assuming end of script.");
      break;
    }
    println("script line #"+i+": "+line);
    str += "\t"+ line.toFormattedString() + "\n";
  }
  str += "}\n";
  str = "// Downloaded from BlinkM at "+(new Date())+"\n" + 
    "// script length: "+ i + "\n" + str;
  editArea.setText(str); // copy it all to the edit textarea
  editArea.setCaretPosition(0);
}

// take a String and turn it into a list of BlinkMScriptLine objects
public ArrayList parseScript( String[] lines ) {
  BlinkMScriptLine bsl;  // little holder
  ArrayList sl = new ArrayList();  // array of scriptlines
  String linepat = "\\{(.+?),\\{'(.+?)',(.+?),(.+?),(.+?)\\}\\}";
  Pattern p = Pattern.compile(linepat);

  for (int i = 0; i < lines.length; i++) {
    String l = lines[i];
    String[] lineparts = l.split("//");  // in case there's a comment
    l = l.replaceAll("\\s+","");  // squash all spaces to zero
    //if(debug) println("l:"+l); 
    Matcher m = p.matcher( l );
    while( m.find() ) {
      if( m.groupCount() == 5 ) { // matched everything
        int dur = parseHexDecInt( m.group(1) );
        char cmd = m.group(2).charAt(0);
        int a1 = parseHexDecInt( m.group(3) );
        int a2 = parseHexDecInt( m.group(4) );
        int a3 = parseHexDecInt( m.group(5) );
        if(debug)println("d:"+dur+",c:"+cmd+",a123:"+a1+","+a2+","+a3);
        bsl = new BlinkMScriptLine( dur, cmd, a1,a2,a3);
        if( lineparts.length > 1 ) 
          bsl.addComment( lineparts[1] );
        sl.add( bsl );
      }
    }
  }
  return sl;
}

// Load a text file containing a light script and turn it into BlinkMScriptLines
// Note: uses Procesing's "loadStrings()"
public void loadFile() {
  int returnVal = fc.showOpenDialog(stf);  // this does most of the work
  if (returnVal != JFileChooser.APPROVE_OPTION) {
    println("Open command cancelled by user.");
    return;
  }
  File file = fc.getSelectedFile();
  // see if it's a txt file
  // (better to write a function and check for all supported extensions)
  if( file!=null ) {
    String lines[] = loadStrings(file); // loadStrings can take File obj too
    StringBuffer sb = new StringBuffer();
    for( int i=0; i<lines.length; i++) {
      sb.append(lines[i]); 
      sb.append("\n");
    }
    editArea.setText(sb.toString()); // copy it all to the edit textarea
    editArea.setCaretPosition(0);
    scriptLines = parseScript( lines ); // and parse it
    // FIXME: should do error checking here
  }
}

// Save a text file of BlinkMScriptLines
// Note: uses Processing's "saveStrings()"
public void saveFile() {
  int returnVal = fc.showSaveDialog(stf);  // this does most of the work
  if( returnVal != JFileChooser.APPROVE_OPTION) {
    println("Save command cacelled by user.");
    return;
  }
  File file = fc.getSelectedFile();
  if (file.getName().endsWith("txt") ||
      file.getName().endsWith("TXT")) {
    String lines[] = editArea.getText().split("\n");
    saveStrings(file, lines);  // actually write the file
  }
}

// Utility: 'serialize' to String
// not strictly needed since we can just read/write the editArea
public String scriptLinesToString(ArrayList scriptlines) {
  String str = "{\n";
  BlinkMScriptLine line;
  for( int i=0; i< scriptLines.size(); i++ ) {
    line = (BlinkMScriptLine)scriptLines.get(i);
    str += "\t"+ line.toFormattedString() +"\n";
  }
  str += "}\n";
  return str;
}

// Utility: parse a hex or decimal integer
public int parseHexDecInt(String s) {
  int n=0;
  try { 
    if( s.indexOf("0x") != -1 ) // it's hex
      n = Integer.parseInt( s.replaceAll("0x",""), 16 ); // yuck
    else 
      n = Integer.parseInt( s, 10 );
  } catch( Exception e ) {}
  return n;
}

// -------------------------------------------------------------------------

//  The nuttiness below is to do the "Inputs" dialog. Jeez what a mess.
JTextField inputText;
JDialog inputDialog;
boolean watchInput;

class InputWatcher implements Runnable {
  public void run() {
    while( watchInput ) { 
      try { Thread.sleep(300); } catch(Exception e) {} 
      byte[] inputs = blinkmComm.readInputs();
      String s = "inputs: ";
      for( int i=0; i<inputs.length; i++) {
        s += "0x" + Integer.toHexString( inputs[i] & 0xff) + ", ";
      }
      inputText.setText(s);
      println(s);
    }
    inputDialog.hide();
  }
}

// man this seems messier than it should be
// all I want is a Dialog with a single line of text and an OK button
// where I can dynamically update the line of text
public void showInputs() {
  if( !connectIfNeeded() ) return;
  println("watching inputs!...");
  inputDialog = new JDialog(stf, "Inputs", false);
  inputDialog.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        watchInput = false;
      }});
  inputDialog.setLocationRelativeTo(stf);
  Container cp = inputDialog.getContentPane();
  //cp.setLayout(new BorderLayout());
  JPanel panel = new JPanel(new BorderLayout());
  panel.setBorder(new EmptyBorder(10,10,10,10));
  cp.add(panel);
  
  inputText = new JTextField("inputs",20);
  JButton btn = new JButton("Done");
  btn.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        watchInput = false;
      }});
  panel.add( inputText, BorderLayout.CENTER );
  panel.add( btn, BorderLayout.SOUTH );
  inputDialog.pack();
  inputDialog.show();

  watchInput = true;
  new Thread( new InputWatcher() ).start();
  // this exits, and thread shoud quit when Done is clikd or window closed
}

// ---------------------------------------------------------------------

//
// do all the nasty gui stuff that's all Java and not very Processingy
//
public void setupGUI() {
  try {  // use a Swing look-and-feel that's the same across all OSs
    MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
    UIManager.setLookAndFeel( new MetalLookAndFeel() );
  } catch(Exception e) { 
    println("drat: "+e);
  }

  fc = new JFileChooser( super.sketchPath ); 
  fc.setFileFilter( new javax.swing.filechooser.FileFilter() {
      public boolean accept(File f) {
        if (f.isDirectory()) 
          return true;
        if (f.getName().endsWith("txt") ||
            f.getName().endsWith("TXT")) 
          return true;
        return false;
      }
      public String getDescription() {
        return "TXT files";
      }
    }
    );
  
  stf = new ScriptToolFrame(mainWidth, mainHeight, this);
  stf.createGUI();

  super.frame.setVisible(false);
  stf.setVisible(true);
  stf.setResizable(false);

}

//
// A new window that holds all the Swing GUI goodness
//
public class ScriptToolFrame extends JFrame {

  public Frame f = new Frame();
  private int width, height;
  private PApplet appletRef;     

  //
  public ScriptToolFrame(int w, int h, PApplet appRef) {
    super("BlinkMScriptTool");
    this.setBackground( backColor );
    this.setFocusable(true);
    this.width = w;
    this.height = h;
    this.appletRef = appRef;

    // handle window close events
    this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          dispose();            // close mainframe
          appletRef.destroy();  // close processing window as well
          appletRef.frame.setVisible(false);
          System.exit(0);
        }
      }); 

    // center on the screen and show it
    this.setSize(this.width, this.height);
    Dimension scrnSize = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(scrnSize.width/2 - this.width/2, 
                     scrnSize.height/2 - this.height/2);
    this.setVisible(true);
  }

  //
  public void createGUI() {
    this.setLayout( new BorderLayout() );
    JPanel editPanel = new JPanel(new BorderLayout());
    JPanel ctrlPanel = new JPanel();    // contains all controls
    JPanel filePanel = new JPanel();    // contains load/save file
    JPanel blinkmPanel  = new JPanel(); // contains all blinkm ctrls
    ctrlPanel.setLayout( new BoxLayout(ctrlPanel,BoxLayout.X_AXIS) );
    filePanel.setLayout( new BoxLayout(filePanel,BoxLayout.X_AXIS) );
    blinkmPanel.setLayout( new BoxLayout(blinkmPanel,BoxLayout.X_AXIS) );

    ctrlPanel.add(filePanel);
    ctrlPanel.add(blinkmPanel);

    this.getContentPane().add( editPanel, BorderLayout.CENTER);
    this.getContentPane().add( ctrlPanel, BorderLayout.SOUTH);

    ctrlPanel.setBorder(new EmptyBorder(5,5,5,5));
    //ctrlPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

    filePanel.setBorder( new CompoundBorder
                         (BorderFactory.createTitledBorder("file"),
                          new EmptyBorder(5,5,5,5)));
    blinkmPanel.setBorder( new CompoundBorder
                           (BorderFactory.createTitledBorder("blinkm"),
                            new EmptyBorder(5,5,5,5)));

    editArea = new JTextArea(strToParse);
    editArea.setFont( monoFont );
    editArea.setLineWrap(false);
    JScrollPane scrollPane = new JScrollPane(editArea);
    editPanel.add( scrollPane, BorderLayout.CENTER);
  
    MyActionListener mal = new MyActionListener();

    JButton loadButton = addButton("Load", "loadFile", mal, filePanel);
    JButton saveButton = addButton("Save", "saveFile", mal, filePanel);

    JButton sendButton = addButton("Send",    "sendBlinkM", mal, blinkmPanel); 
    JButton recvButton = addButton("Receive", "recvBlinkM", mal, blinkmPanel); 

    blinkmPanel.add(Box.createRigidArea(new Dimension(5,5)));;
    disconnectButton   = addButton("disconnect","disconnect", mal,blinkmPanel);
    disconnectButton.setEnabled(false);
    blinkmPanel.add(Box.createRigidArea(new Dimension(5,5)));;

    JButton stopButton = addButton("Stop", "stopScript", mal, blinkmPanel);
    JButton playButton = addButton("Play", "playScript", mal, blinkmPanel);
    
    JLabel posLabel = new JLabel("<html>play <br>pos:</html>", JLabel.RIGHT);
    posText = new JTextField("0");
    posLabel.setFont(monoFontSm);
    posText.setFont(monoFontSm);
    blinkmPanel.add(posLabel);
    blinkmPanel.add(posText);

    JButton inputsButton = addButton("inputs", "inputs", mal, blinkmPanel);

  }

  //
  private JButton addButton( String text, String action, ActionListener al,
                            Container container ) {
    JButton button = new JButton(text);
    button.setActionCommand(action);
    button.addActionListener(al);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    container.add(Box.createRigidArea(new Dimension(5,5)));;
    container.add(button);
    return button;
  }

} // ScriptToolFrame

// Copyright (c) 2007-2008, ThingM Corporation

/**
 * BlinkMComm -- Simple Processing 'library' to talk to BlinkM 
 *               (via an Arduino programmed with BlinkMCommunicator)
 *
 * This is almost 100% Java with few Processing dependencies, 
 * so with a bit of work can be usable in other Java environments
 *
 * 2007-2008, Tod E. Kurt, ThingM, http://thingm.com/
 * 
 *
 * NOTE: you should NOT have a "serialEvent()" method in your sketch,
 *       else the BlinkM reading functions will not work.  For debugging
 *       a "serialEvent()" method is useful, but be sure to comment it out
 *       if you want to read anything back from a BlinkM.
 */


  // for connect dialog

public class BlinkMComm {
  public final boolean debug = true;
  public final boolean fakeIt = false;

  boolean isConnected = false;

  public byte blinkm_addr = 0x09;
  public String portName = null;
  public final int portSpeed = 19200;

  PApplet papplet; // our owner, owns the GUI window
  Serial port;
  
  // mapping of duration to ticks      (must be same length as 'durations')
  public byte[] durTicks   = { (byte)   1, (byte) 18, (byte) 72 };
  // mapping of duration to fadespeeds (must be same length as 'durations')
  public byte[] fadeSpeeds = { (byte) 100, (byte) 25, (byte)  5 };
    
  Color cBlk = new Color(0,0,0);
  Color lastColor;

  // Return a list of potential ports
  // they should be ordered by best to worst (but are not right now)
  // this can't be static as a .pde, sigh.
  public String[] listPorts() {
      String[] a = Serial.list();
      String osname = System.getProperty("os.name");
      if( osname.toLowerCase().startsWith("windows") ) {
          // reverse list because Arduino is almost always highest COM port
          for(int i=0;i<a.length/2;i++) {
              String t = a[i]; a[i] = a[a.length-(1+i)]; a[a.length-(1+i)] = t;
          }
      }
      return a;
  }

  public BlinkMComm(PApplet p) {
    papplet = p;
  }

  /**
   * Connect to the given port
   * Can optionally take a PApplet (the sketch) to get serialEvents()
   * but this is not recommended
   *
   */
  public void connect( String portname ) throws Exception {
      debug("BlinkMComm.connect: portname:"+portname);
      try {
          if(port != null)
              port.stop(); 
          port = new Serial(papplet, portname, portSpeed); // FIXME: callback 
          pause(100);

          // FIXME: check address, set it if needed

          isConnected = true;
          portName = portname;
      }
      catch (Exception e) {
          isConnected = false;
          portName = null;
          port = null;
          throw e;
      }
  }

  // disconnect but remember the name
  public void disconnect() {
      if( port!=null )
          port.stop();
      isConnected = false;
  }

  // verifies connection is good
  public boolean checkConnection() {
      return true;        // FIXME: add echo check
  }

  public boolean isConnected() {
      return isConnected; // FIXME: this is kinda lame
  }

  // uses global var 'durations'
  public byte getDurTicks(int[] durations, int loopduration) {
    for( int i=0; i<durations.length; i++ ) {
      if( durations[i] == loopduration )
        return durTicks[i];
    }
    return durTicks[0]; // failsafe
  }
  // this is so lame
  public byte getFadeSpeed(int[] durations, int loopduration) {
      for( int i=0; i<durations.length; i++ ) {
          if( durations[i] == loopduration )
              return fadeSpeeds[i];
      }
      return fadeSpeeds[0]; // failsafe
  }


  /**
   * Send an I2C command to addr, via the BlinkMCommunicator Arduino sketch
   * Byte array must be correct length
   */
  public synchronized void sendCommand( byte addr, byte[] cmd ) {
      if( fakeIt ) return;        // just pretend
      if( !isConnected ) return;
      debug("BlinkMComm.sendCommand(): "+(char)cmd[0]+", "+cmd.length);
      byte cmdfull[] = new byte[4+cmd.length];
      cmdfull[0] = 0x01;                    // sync byte
      cmdfull[1] = addr;                    // i2c addr
      cmdfull[2] = (byte)cmd.length;        // this many bytes to send
      cmdfull[3] = 0x00;                    // this many bytes to receive
      for( int i=0; i<cmd.length; i++) {    // actual command
          cmdfull[4+i] = cmd[i];
      }
      port.write(cmdfull);
      //port.clear();  // just in case
      //port.flush();  // maybe?
  }

  /**
   * Send an I2C command to addr, via the BlinkMCommunicator Arduino sketch
   * Byte array must be correct length
   * returns response (if any) 
   */
  public synchronized byte[] sendCommand( byte addr, byte[] cmd, int respcnt ) {
      if( fakeIt ) return null;        // just pretend
      if( !isConnected ) return null;
      debug("BlinkMComm.sendCommand(): "+(char)cmd[0]+", "+cmd.length);
      byte cmdfull[] = new byte[4+cmd.length];
      cmdfull[0] = 0x01;                    // sync byte
      cmdfull[1] = addr;                    // i2c addr
      cmdfull[2] = (byte)cmd.length;        // this many bytes to send
      cmdfull[3] = (byte)respcnt;           // this many bytes to receive
      for( int i=0; i<cmd.length; i++) {    // actual command
          cmdfull[4+i] = cmd[i];
      }
      port.clear(); // just in case
      port.write(cmdfull);
      pause(10); // wait for 

      // this bit needs to be cleaned up
      if( respcnt > 0 ) {
          byte[] resp = new byte[respcnt];
          int j = 100;
          while( port.available() < respcnt ) {
              pause(1); 
              if( j-- == 0 ) { 
                  debug("sendCommand couldn't receive");
                  return null; 
              }
          }
          for( int i=0; i<respcnt; i++ ) 
              resp[i] = (byte)port.read();
          return resp;
      }
      return null;
  }

  //
  public void writeScriptLine( int pos, BlinkMScriptLine line ) {
      // build up the byte array to send
      debug("BlinkMComm.writeScriptLine: pos:"+pos+" scriptline: "+line);
      byte[] cmd = new byte[8];    // 
      cmd[0] = (byte)'W';          // "Write Script Line" command
      cmd[1] = (byte)0;            // script id (0==eeprom)
      cmd[2] = (byte)pos;          // script line number
      cmd[3] = (byte)line.dur;     // duration in ticks
      cmd[4] = (byte)line.cmd;     // command
      cmd[5] = (byte)line.arg1;    // cmd arg1
      cmd[6] = (byte)line.arg2;    // cmd arg2
      cmd[7] = (byte)line.arg3;    // cmd arg3
      sendCommand( blinkm_addr, cmd );
      pause(20); // must have at least 4.5msec delay between EEPROM writes
    }

  //
  public BlinkMScriptLine readScriptLine(int script_id, int pos ) {
      debug("BlinkMComm.readScriptLine: pos:"+pos);
      //BlinkMScriptLine line = new BlinkMScriptLine();
      byte[] cmd = new byte[3];
      cmd[0] = (byte)'R';           // "Read Script Line" command
      cmd[1] = (byte)script_id;     // script id (0==eeprom)
      cmd[2] = (byte)pos;           // script line number
      byte[] resp = sendCommand( blinkm_addr, cmd, 5 ); // 5 bytes in response
      BlinkMScriptLine line = new BlinkMScriptLine();
      if( !line.fromByteArray(resp) ) return null;
      return line;  // we're bad
  }

  // set the length & repeats for a given script_id (always 0 for now)
  public void setScriptLengthRepeats(int script_id, int len, int reps) {
      //   set script length  cmd          id        length        reps
      byte[] cmdsetlength = { 'L', (byte)script_id, (byte)len, (byte)reps };
    sendCommand( blinkm_addr, cmdsetlength );
    pause(20);
  }

  // play a light script
  public void playScript(int script_id, int reps, int pos) {
      byte[] cmd = { 'p', (byte)script_id, (byte)reps, (byte)pos};
      sendCommand( blinkm_addr, cmd );
  }
  // plays the eeprom script (script id 0) from start, forever
  public void playScript() {
      playScript(0,0,0);
  }
  
  // stops any playing script
  public void stopScript() {
      debug("BlinkmComm.stopPlayingScript");
      byte[] cmd = {'o'};
      sendCommand( blinkm_addr, cmd );
  }

  // set boot params   cmd,mode,id,reps,fadespeed,timeadj
  public void setStartupParams( int mode, int script_id, int reps, 
                                int fadespeed, int timeadj ) {
      byte cmdsetboot[] = { 'B', 1, (byte)script_id, (byte)reps, 
                            (byte)fadespeed, (byte)timeadj };
      sendCommand( blinkm_addr, cmdsetboot );
      pause(20);
  }
  // default values for startup params
  public void setStartupParamsDefault() {
      setStartupParams( 1, 0, 0, 0x08, 0 );
  }
  
  // read the 4 8-bit analog inputs on a MaxM
  public byte[] readInputs() {
      debug("BlinkMComm.readInputs");
      byte[] cmd = new byte[1];
      cmd[0] = (byte)'i';           // "Read Inputs" command
      byte[] resp = sendCommand( blinkm_addr, cmd, 4 ); // 4 bytes in response
      return resp;
  }


  // ------------ old stuff for BlinkMSequencer -------------

  /**
   * Burn a list of colors to a BlinkM
   * @param colorlist an ArrayList of the Colors to burn (java Color objs)
   * @param nullColor a color in the list that should be treated as nothing
   * @param duration  how long the entire list should last for, in seconds
   * @param loop      should the list be looped or not
   * @param progressbar if not-null, will update a progress bar
   */
  public void burnColorList(ArrayList colorlist, Color nullColor, 
                            int[] durations, int duration, boolean loop, 
                            javax.swing.JProgressBar progressbar) {

    byte[] cmd = new byte[8];
    byte fadespeed = getFadeSpeed(durations, duration);
    byte durticks = getDurTicks(durations, duration);
    byte reps = (byte)((loop) ? 0 : 1);  // sigh, java

    Color c;

    debug("BlinkMComm.burn: durticks:"+durticks+" fadespeed:"+fadespeed);

    // build up the byte array to send
    Iterator iter = colorlist.iterator();
    int i=0;
    while( iter.hasNext() ) {
      debug("BlinkMComm.burn: writing script line "+i);
      c = (Color) iter.next();
      if( c == nullColor )
        c = cBlk;
      cmd[0] = (byte)'W';          // "Write Script Line" command
      cmd[1] = (byte)0;            // script id (0==eeprom)
      cmd[2] = (byte)i;            // script line number
      cmd[3] = (byte)durticks;     // duration in ticks
      cmd[4] = (byte)'c';          // fade to rgb color command
      cmd[5] = (byte)c.getRed();   // cmd arg1
      cmd[6] = (byte)c.getGreen(); // cmd arg2
      cmd[7] = (byte)c.getBlue();  // cmd arg3
      sendCommand( blinkm_addr, cmd );
      if( progressbar !=null) progressbar.setValue(i);  // hack
      i++;
      pause(50);
    }
    // set script length   cmd   id         length         reps
    byte[] cmdsetlength = { 'L', 0, (byte)colorlist.size(), reps };
    sendCommand( blinkm_addr, cmdsetlength );
    pause(50);

    // set boot params   cmd,mode,id,reps,fadespeed,timeadj
    byte cmdsetboot[] = { 'B', 1, 0, 0, fadespeed, reps };
    sendCommand( blinkm_addr, cmdsetboot );
    pause(50);

    // set fade speed
    byte[] cmdsetfade = { 'f', fadespeed };
    sendCommand( blinkm_addr, cmdsetfade );
    pause(30);

    // and cause the script to be played 
    //                 cmd,id,reps,pos
    byte[] cmdplay = { 'p', 0, reps, 0 };
    sendCommand( blinkm_addr, cmdplay );
    pause(30);
  }

  // prepare blinkm for playing preview scripts
  public void prepareForPreview(int[] durations, int loopduration) {
      byte fadespeed = getFadeSpeed(durations, loopduration);
      debug("BlinkmComm.prepareForPreview: fadespeed:"+fadespeed);
      byte[] cmdstop    = {'o'};
      byte[] cmdsetfade = {'f', fadespeed};
      if( isConnected() ) {
          sendCommand( blinkm_addr, cmdstop );
          pause(40);
          sendCommand( blinkm_addr, cmdsetfade );
          pause(40);
      }
  }
  
  /**
   *
   */
  public void sendColor( Color aColor, Color nullColor, int duration ) {
      if( aColor.equals( lastColor ) )   // don't clog the pipes!
          return;
      
      Color c = (aColor == nullColor) ? cBlk : aColor;
      byte[] cmd={'c',(byte)c.getRed(),(byte)c.getGreen(),(byte)c.getBlue()};
      sendCommand( blinkm_addr, cmd );
      pause(10); // FIXME: hack
      lastColor = aColor;
  }
  
  // ---------------------------

  //
  // open up a dialog box to select the serial port
  //
  public boolean doConnectDialog() {
      String[] ports = listPorts();
      //String buttons[] = {"Connect", "Cancel"};
      String s = (String)
          JOptionPane.showInputDialog( null,
                                       "Select a serial port:\n",
                                       "Connect to BlinkMCommunicator",
                                       JOptionPane.PLAIN_MESSAGE,
                                       null,
                                       ports,
                                       ports[0]
                                      );
    // if a string was returned, try to connect.
    if ((s != null) && (s.length() > 0)) {
        disconnect(); // just in case
        try { 
            connect( s );
        } catch( Exception e ) {
            JOptionPane.showMessageDialog( null,
                                          "Could not connect\n"+e,
                                          "Connect error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    // otherwise, we failed
    return false;
  }




  // ---------------------

  /**
   * A simple delay
   */
  public void pause( int millis ) {
      try { Thread.sleep(millis); } catch(Exception e) { }
  }

  public void debug( String s ) {
      if(debug) println(s);
  }
  
}




// Java data struct representation of a BlinkM script line
// also includes string rendering
public class BlinkMScriptLine {
    int dur;
    char cmd;
    int  arg1,arg2,arg3;
    String comment;

    public BlinkMScriptLine() {
    }
    public BlinkMScriptLine( int d, char c, int a1, int a2, int a3 ) {
        dur = d;
        cmd = c;
        arg1 = a1; 
        arg2 = a2;
        arg3 = a3;
    }

    // "construct" from a byte array.  could also do other error checking here
    public boolean fromByteArray(byte[] ba) {
        if( ba==null || ba.length != 5 ) return false;
        dur  = ba[0] & 0xff;
        cmd  = (char)(ba[1] & 0xff);
        arg1 = ba[2] & 0xff;
        arg2 = ba[3] & 0xff;
        arg3 = ba[4] & 0xff;  // because byte is stupidly signed
        return true;
    }

    public void addComment(String s) {
        comment = s;
    }

    public String toStringSimple() {
        return "{"+dur+", {'"+cmd+"',"+arg1+","+arg2+","+arg3+"}},";
    }
    public String toFormattedString() {
        return toString();
    }
    // this seems pretty inefficient with all the string cats
    public String toString() {
        String s="{"+dur+", {'"+cmd+"',";
        if( cmd=='n'||cmd=='c'||cmd=='C'||cmd=='h'||cmd=='H' ) {
            s += makeGoodHexString(arg1) +","+
                makeGoodHexString(arg2) +","+
                makeGoodHexString(arg3) +"}},";
        }
        else 
            s += arg1+","+arg2+","+arg3+"}},";
        if( comment!=null ) s += "\t// "+comment;
        return s;
    }
    // convert a byte properly to a hex string
    // why does Java number formatting still suck?
    public String makeGoodHexString(int b) {
        String s = Integer.toHexString(b);
        if( s.length() == 1 ) 
            return "0x0"+s;
        return "0x"+s;
    }


}

  static public void main(String args[]) {     PApplet.main(new String[] { "BlinkMScriptTool" });  }}