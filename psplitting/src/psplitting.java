/**
=========================================================================

  FRpsplitting   V 0.5.0   dev 2014

  AUTHOR   RANCAN  FRANCO  

DESCRIPTION:		Applicazione grafica che implementa l'algoritmo 
			di Splitting

=========================================================================
*/



import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;



/**
  definizione di stringhe costanti utili nella generazioni 
  dei messaggi e dei widgets
*/

interface pSplittingSTR {
    String TITLE = "P-SPLITTING";
    String WELCOME_MSG = "QUAD P-SPLITTING     Autore:  Franco RANCAN"+
	"  matr. n. 607927";
    String MENU = "File";
    String OPEN_MENU = "Apri";
    String SAVE_MENU = "Salva";
    String OPEN_TREE_MENU = "Apri  Struttura Dati";
    String SAVE_TREE_MENU = "Salva Struttura Dati";
    String EXIT_MENU = "Esci";
    String EXECUTE_BUTTON = "ELABORA";
    String STOP_BUTTON = "ANNULLA";
    String VARIANT_LABEL = "Soglia Varianza";
    String ERR_WRONG_FILE = "Tipo di file non consentito";
    String ERR_I0 = "Errore di Lettura / Scrittura su disco";
    String ERR_LOAD_IMG = "Errore lettura immagine";
    String ERR_SAVE_IMG = "Errore scrittura immagine";
    String ERR_LOAD_TREE = "Errore lettura Struttura Dati Albero";
    String ERR_SAVE_TREE = "Errore scrittura Struttura Dati Albero";
    String ERR_ELAB_IMG = "Errore elaborazione immagine";

}



public class psplitting extends JFrame
    implements ActionListener, ChangeListener, pSplittingSTR {

    private static final int    X_SIZE_FRAME    = 800;
    private static final int    Y_SIZE_FRAME    = 600;
    private static final int    TEXT_FIELD_SIZE =  10;
    private static final int    X_SIZE_SLIDER   = 120;
    private static final int    Y_SIZE_SLIDER   =  40;
    private static final String FILE_EXT     = ".pgm";

    private float threshold;  
    private MyImageFactory imgF;
    private MyImage myImg;

    private PaintPane imgInPaintPane;
    private PaintPane imgOutPaintPane;

    private ElabSplitting elabSplitting;

    private BufferedImage imgIn  = null;
    private BufferedImage imgOut = null;

    protected int mu;
    protected float sigma;

    protected JMenuBar  menuBar;

    protected JMenuItem OpenMenuItem;
    protected JMenuItem SaveMenuItem;
    protected JMenuItem OpenTreeMenuItem;
    protected JMenuItem SaveTreeMenuItem;
    protected JMenuItem ExitMenuItem;

    protected JToolBar toolBar;

    protected JButton OpenButton;
    protected JButton SaveButton;

    protected JTextField directVariantValueTF;
    protected JTextField relativeVariantValueTF;

    protected JSlider slider;

    protected JLabel variantLabel;

    protected JButton executeButton;
    protected JButton stopButton;

    protected JLabel statusBar;

    protected JScrollPane imgInScrollPan;
    protected JScrollPane imgLinesScrollPan;
    protected JScrollPane imgPointScrollPan;
    protected JScrollPane imgOutScrollPan;

    protected JFileChooser fileChooser;


    public psplitting() {
	super();

	inizialization();
	setupFrame();
	setupFrameBehaviour();
	setupComponents();
	buildGui();
	registerListeners();    
	registerChangeListeners();    

	guiInitialAction();
    }


    protected void inizialization() {
	threshold = (float)0.0;
    }


    protected void setupFrame() {
	setTitle(TITLE);
	setSize(X_SIZE_FRAME, Y_SIZE_FRAME);
    }


    protected void setupFrameBehaviour() {
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    protected void setupComponents() {
	imgInPaintPane  = new PaintPane();
	imgOutPaintPane = new PaintPane();

	fileChooser = createFileChooser();
	buildMenuItems();    
	buildToolBarButtons();
	buildButtons();
	buildTextFields();
	buildLabels();
	buildSlider();
	buildStatusBar();
    }

    
    protected JFileChooser createFileChooser() {
	return new JFileChooser();
    }


    protected JFileChooser getFileChooser() {
	return fileChooser;
    }


    protected void buildMenuItems() {
	OpenMenuItem     = createMenuItem(OPEN_MENU,
					  new ImageIcon("Open24.gif"));
	SaveMenuItem     = createMenuItem(SAVE_MENU,
					  new ImageIcon("Save24.gif"));
	OpenTreeMenuItem = createMenuItem(OPEN_TREE_MENU);
	SaveTreeMenuItem = createMenuItem(SAVE_TREE_MENU);
	ExitMenuItem     = createMenuItem(EXIT_MENU);
    }


    protected JMenuItem createMenuItem(String name,Icon icon) {
	return new JMenuItem(name,icon);
    }


    protected JMenuItem createMenuItem(String name) {
	return new JMenuItem(name);
    }


    protected void buildToolBarButtons() {
	OpenButton = createToolbarButton(new ImageIcon("Open24.gif"));
	SaveButton = createToolbarButton(new ImageIcon("Save24.gif"));
    }

    protected JButton createToolbarButton(Icon icon) {
	return new JButton(icon);
    }


    protected void buildButtons() {
	executeButton = new JButton(EXECUTE_BUTTON);
        stopButton    = new JButton(STOP_BUTTON);
	stopButton.setForeground(Color.RED);
    }


    protected void buildTextFields() {
	directVariantValueTF   = new JTextField("0", TEXT_FIELD_SIZE);
	directVariantValueTF.setEditable(false);
	directVariantValueTF.setBackground(Color.WHITE);
	relativeVariantValueTF = new JTextField("0", TEXT_FIELD_SIZE);
	relativeVariantValueTF.setEditable(false);
	relativeVariantValueTF.setBackground(Color.WHITE);
    }


    protected void buildLabels() {
	// costruisce le label presenti nella gui

	variantLabel = new JLabel(VARIANT_LABEL);
	variantLabel.setForeground(Color.BLUE);
    }


    protected void buildSlider() {
	slider = new  JSlider();
	slider.setPaintTicks(true);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
	//        slider.setPaintLabels(true);
	slider.setPreferredSize(new Dimension(X_SIZE_SLIDER, Y_SIZE_SLIDER));
    }


    protected void buildStatusBar() {
	statusBar = new JLabel();
	statusBar.setForeground(Color.BLUE);
	statusBar.setFont(new Font("Courier", Font.PLAIN,14)); 
   }


    protected void buildGui() {
	JPanel rootPan;

	rootPan = (JPanel)getContentPane();
	menuBar = createMenuBar();
   	setJMenuBar(menuBar);
	toolBar = createToolBar();

	rootPan.setLayout(new BorderLayout());

	rootPan.add(BorderLayout.NORTH,toolBar);

	rootPan.add(BorderLayout.CENTER, createImgPan()); 

	rootPan.add(BorderLayout.EAST, createControlPan()); 

	rootPan.add(BorderLayout.SOUTH, createStatusBarPan()); 
    }


    protected JMenuBar createMenuBar() {
	JMenu menu = new JMenu(MENU);
	menu.add(OpenMenuItem);
	menu.add(SaveMenuItem);
	menu.addSeparator();
	menu.add(OpenTreeMenuItem);
	menu.add(SaveTreeMenuItem);
	menu.addSeparator();
	menu.add(ExitMenuItem);

	JMenuBar menuBar = new JMenuBar();
	menuBar.add(menu);
    
	return menuBar;
    }


    protected JToolBar createToolBar() {
	JToolBar toolBar = new JToolBar();
	toolBar.add(OpenButton);
	toolBar.add(SaveButton);
	//	toolBar.addSeparator();

	return toolBar;
    }


    protected JPanel createImgPan() {
	// pannello che contiene le 2 immagini
	//
	JPanel pan = new JPanel();

	imgInScrollPan = new JScrollPane(imgInPaintPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

	imgOutScrollPan = new JScrollPane(imgOutPaintPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

	pan.setLayout(new GridLayout(2,1));
	pan.add(imgInScrollPan);
	pan.add(imgOutScrollPan);

	return pan;
    }

 
    protected JPanel createControlPan() {
	// pannello di controllo (pulsanti e textfiels )
	//
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	JPanel pan = new JPanel(gbl);

	c.weightx = 1.0;
	c.insets = new Insets(15,3,0,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(variantLabel, c);
	pan.add(variantLabel);

	c.weightx = 1.0;
	c.insets = new Insets(15,3,0,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(directVariantValueTF, c);
	pan.add(directVariantValueTF);

	c.weightx = 1.0;
	c.insets = new Insets(15,3,0,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(relativeVariantValueTF, c);
	pan.add(relativeVariantValueTF);

	c.weightx = 1.0;
	c.insets = new Insets(15,3,15,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(slider, c);
	pan.add(slider);

	c.weightx = 1.0;
	c.insets = new Insets(35,3,15,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(executeButton, c);
	pan.add(executeButton);

	c.weightx = 1.0;
	c.insets = new Insets(15,3,15,3);  // top, left, bottom, right
	c.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(stopButton, c);
	pan.add(stopButton);

	return pan;
    }


    protected JPanel createStatusBarPan() {
	// Barra di stato
	//
	JPanel pan = new JPanel();
	pan.setLayout(new BorderLayout());
	pan.add(BorderLayout.WEST, statusBar); 

	return pan;
    }


    protected void writeStatusBar(String msg) {
	statusBar.setText("                                             " +
			  "                                             ");
	if (msg.length() > 0) statusBar.setText("  " + msg);
    }


    protected void registerListeners() {
	// voci del menu
	OpenMenuItem.addActionListener(this);
	SaveMenuItem.addActionListener(this);
	OpenTreeMenuItem.addActionListener(this);
	SaveTreeMenuItem.addActionListener(this);
	ExitMenuItem.addActionListener(this);

	// pulsanti della status bar
	OpenButton.addActionListener(this);
	SaveButton.addActionListener(this);

	// pulsanti vari
	executeButton.addActionListener(this);
	stopButton.addActionListener(this);
    }


    protected void registerChangeListeners() {
        slider.addChangeListener(this);
    }


    public void promptOpen() {
	int response = getFileChooser().showOpenDialog(this);
	if(response == JFileChooser.APPROVE_OPTION) {
	    try {
		open(getFileChooser().getSelectedFile());
	    }
	    catch(IllegalArgumentException e) {
		JOptionPane.showMessageDialog(this, 
					      ERR_WRONG_FILE,
					      "",
					      JOptionPane.WARNING_MESSAGE);
	    }      
	    catch(IOException e) {
		JOptionPane.showMessageDialog(this,
					      ERR_LOAD_IMG,
					      ERR_I0,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImgUnknownFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_IMG,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImgParseException e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_IMG,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImageWrongFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_IMG,
					      JOptionPane.WARNING_MESSAGE);
	    }
	}  
    }



    
    public void open(File f) throws IOException, IllegalArgumentException,
	MyImgUnknownFormat, MyImgParseException, MyImageWrongFormat {

	if(isValidFiletype(f)) {
	    imgF = new PGMP2Factory();  
	    myImg = imgF.createMyImage();
	    try {
		imgIn = myImg.load(new FileInputStream(f));

		// calcolo media e varianza intera immagine
		QuadDecomposition qd = new 
		    QuadDecomposition(imgIn, (float)0.0);
		this.sigma = qd.getSigmaWholeImage();
		this.mu    = qd.getAverageWholeImage();

		imgInPaintPane.load(imgIn);  // disegna l'immagine

		guiActionOnImageLoad();
	    }
	    catch (QDWrongImageFormat e){
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_IMG,
					      JOptionPane.WARNING_MESSAGE);    
	    }
	}
	else
	    throw new IllegalArgumentException();
    }


    protected boolean isValidFiletype(File f) {
	return f.getName().toLowerCase().endsWith(FILE_EXT);
    }


    
    public void promptSaveAs() {
	int response = getFileChooser().showSaveDialog(this);
	if(response == JFileChooser.APPROVE_OPTION) {
	    try {
		save(getFileChooser().getSelectedFile());
	    }
	    catch(IOException e) {
		JOptionPane.showMessageDialog(this,
					      ERR_SAVE_IMG,
					      ERR_I0,
					      JOptionPane.WARNING_MESSAGE);    
	    }
	    catch(MyImageWrongFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_SAVE_IMG,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImgUnknownFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_SAVE_IMG,
					      JOptionPane.WARNING_MESSAGE);
	    }
	}  
    }


    public void save(File f) 
	throws IOException, MyImageWrongFormat, MyImgUnknownFormat {

	imgF = new PGMP2Factory();  
	myImg = imgF.createMyImage();
	myImg.save(elabSplitting.getElabImg(), 
		   new FileOutputStream(f), " Immagine elaborata");
    }



    /**
       lettura struttura dati Albero Quad Tree
    */
    public void promptOpenTree() {
	int response = getFileChooser().showOpenDialog(this);
	if(response == JFileChooser.APPROVE_OPTION) {
	    try {
		openTree(getFileChooser().getSelectedFile());
	    }
	    catch(IOException e) {
		JOptionPane.showMessageDialog(this,
					      ERR_LOAD_TREE,
					      ERR_I0,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImgUnknownFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_TREE,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImgParseException e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_TREE,
					      JOptionPane.WARNING_MESSAGE);
	    }
	    catch(MyImageWrongFormat e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_LOAD_TREE,
					      JOptionPane.WARNING_MESSAGE);
	    }
	}  
    }


  
    /**
       lettura struttura dati Quad Tree
    */
    public void openTree(File f) 
	throws IOException, MyImgUnknownFormat, MyImgParseException,
	       MyImageWrongFormat {

	guiBeforeElaboration();  // cambiamenti alla GUI prima dell'elab

	// Per il caricamento del'albero e successiva visualizzazione
	// ci si avvale del un Thread elabSplitting
	//
	elabSplitting = new ElabSplitting(this, imgOutPaintPane, f);

	// quando avra' finito', il Thread chiamera' il metodo 
	// guiActionAfterElaboration() (contenuto in questa classe) 
    }



    /**
       Salvataggio struttura dati Quad Tree
    */
    public void promptSaveTreeAs() {
	int response = getFileChooser().showSaveDialog(this);
	if(response == JFileChooser.APPROVE_OPTION) {
	    try {
		saveTree(getFileChooser().getSelectedFile());
	    }
	    catch(IOException e) {
		JOptionPane.showMessageDialog(this,
					      ERR_SAVE_TREE,
					      ERR_I0,
					      JOptionPane.WARNING_MESSAGE);    
	    }
	    catch(QDNoTreeException e) {
		JOptionPane.showMessageDialog(this,
					      e.getMessage(),
					      ERR_SAVE_TREE,
					      JOptionPane.WARNING_MESSAGE);
	    }
	}  
    }


    /**
       Salvataggio struttura dati Quad Tree
    */
    public void saveTree(File f) 
	throws IOException,  QDNoTreeException {

	// si deve utilizzare un metodo della classe QuadDecomposition
	// la cui istanza si trova nell'oggetto elabSplitting (il Thread
	// che pilota l'elaborazione)
	//
	elabSplitting.getQDHandle().saveTree(new FileOutputStream(f));	
    }



    /**
       metodo che fa i necessari cambiamenti alla GUI
       per lo stato iniziale di partenza
    */
    protected void guiInitialAction() {
	writeStatusBar(WELCOME_MSG);
	slider.setValue(0);
	slider.setEnabled(false);
	SaveMenuItem.setEnabled(false);
	SaveTreeMenuItem.setEnabled(false); 
	SaveButton.setEnabled(false); 
	executeButton.setEnabled(false);
	stopButton.setEnabled(false);
   }


    /**
       metodo che fa i necessari cambiamenti alla GUI
       per lo stato immediatamente precedente l'elaborazione
    */
    protected void guiBeforeElaboration() {

	writeStatusBar("");

	slider.setEnabled(false);
	SaveMenuItem.setEnabled(false);
	SaveTreeMenuItem.setEnabled(false); 
	SaveButton.setEnabled(false); 
	executeButton.setEnabled(false);
	stopButton.setEnabled(true);
   }


    /**
       metodo che fa i necessari cambiamenti alla GUI
       dopo l'elaborazione
       Questo e' un metodo pubblico perche' deve essere accessibile
       dall'esterno
    */
    public void guiActionAfterElaboration(int nSteps, int nCells) {

	imgOutScrollPan.repaint();

	if (nSteps == 0 && nCells == 0)
	    writeStatusBar("");
	else
	    writeStatusBar("elaborazione eseguita in " + nSteps + " passi.  " +
			   "Costruite " + nCells + " celle omogenee");

	slider.setEnabled(true);
	SaveMenuItem.setEnabled(true);
	SaveTreeMenuItem.setEnabled(true); 
	SaveButton.setEnabled(true); 
	executeButton.setEnabled(true);
	stopButton.setEnabled(false);
    }


    /**
       metodo che fa i necessari cambiamenti alla GUI dopo che
       e' stata caricata un'immagine
    */
    protected void guiActionOnImageLoad() {
	slider.setValue(50);
	writeStatusBar("Immagine caricata: " + imgIn.getWidth() + "x" +
		       imgIn.getHeight() + " pixels     media " + mu + 
		       "  varianza = " + sigma);

	imgInScrollPan.repaint();

	imgOutPaintPane.clearPan();
	imgOutScrollPan.repaint();

	slider.setEnabled(true);
	SaveMenuItem.setEnabled(false);
	SaveTreeMenuItem.setEnabled(false); 
	SaveButton.setEnabled(false); 
	executeButton.setEnabled(true);
	stopButton.setEnabled(false);
    }

    /** 
	predispone per eseguire l'elaborazione
    */
    public void elabAction() {
	// si e' richiesto di fare l'elaborazione
	// per l'elaborazione si usa un Thread apposito: elabSplitting
	//

	guiBeforeElaboration();  // cambiamenti alla GUI prima dell'elab


	elabSplitting = new ElabSplitting(this, 
					  imgInPaintPane.getImage(),
					  imgOutPaintPane, threshold);

	// quando avra' finito, il Thread chiamera' il metodo 
	// guiActionAfterElaboration() (contenuto in questa classe) 
    }



    /** 
	predispone per fermare l'elaborazione
    */
    public void stopAction() {
	// si e' richiesto di fermare l'elaborazione
	// per l'elaborazione si usa un Thread apposito: elabSplitting
	//

	elabSplitting.getThread().interrupt();
	// quando avra' finito, il Thread chiamera' il metodo 
	// guiActionAfterElaboration() (contenuto in questa classe) 
    }


    public void exitAction() {
	// si e' richiesto di fermare uscire
	//
	// per semplicita', non si fanno controlli, si esce e basta
	// (i file vengono chiusi non appena finiti i caricamenti
	// ed i salvataggi)

	System.exit(0);
    }


    public void actionPerformed(ActionEvent ae) {

	if(ae.getSource().equals(OpenButton) || 
	   ae.getSource().equals(OpenMenuItem)) {
	    promptOpen();
	}
	else if(ae.getSource().equals(SaveButton) || 
		ae.getSource().equals(SaveMenuItem)) {
	    promptSaveAs();
	}
	else if(ae.getSource().equals(executeButton)) {
	    elabAction();
	}
	else if(ae.getSource().equals(OpenTreeMenuItem)) {
	    promptOpenTree();
	}
	else if(ae.getSource().equals(SaveTreeMenuItem)) {
	    promptSaveTreeAs();
	}
	else if(ae.getSource().equals(stopButton)) {
	    stopAction();
	}
	else if(ae.getSource().equals(ExitMenuItem)) {
	    exitAction();
	}
    }


    public void stateChanged(ChangeEvent ce) {

	if (ce.getSource().equals(slider)) {
	    float scale = (float)slider.getValue() / 100;
	    relativeVariantValueTF.setText(String.valueOf(scale));
	    // si fissa il valore limite come la sigma dell'intera immagine
	    // (calcolata in fase di caricamento immagine)
	    this.threshold = scale * (sigma + 10);
	    directVariantValueTF.setText(String.valueOf(this.threshold));
	}
    }



    public static void main (String [] args) {
	psplitting ps = new psplitting();

	//	ps.pack();
	ps.setVisible(true);
    }
}



/**
   classe specializzata nel realizzare oggetti di contenimento di grafica
   Data un'immagine, e' possibile caricarla nel pannello ed anche cancellarla
*/
class PaintPane extends JPanel {

    private int height = 0;
    private int width  = 0;
    private BufferedImage img = null;

    public PaintPane() {
	super();
    }


    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D)g;

	
	if (img != null) 
	    g2.drawImage(img, 0, 0, null);
	
    }


    public void load(BufferedImage img) {
	this.img = img;

	if (this.width != this.img.getWidth() || 
	    this.height != this.img.getWidth()) {
	    this.width  = this.img.getWidth();
	    this.height = this.img.getHeight();
	    setPreferredSize(new Dimension(width, height));

	    setOpaque(false);
	}
	this.revalidate();
    }

    public BufferedImage getImage() { return img; }

    /**
       cancella il pannello, eliminando l'immagine precedente
    */      
    public void clearPan() {
	WritableRaster rasterW = 
	    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
				      1, 1, 1, new Point());
	rasterW.setSample(0,0,0,128);
	img = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
	img.setData(rasterW);
	this.revalidate();
    }
}



/**
   classe di oggetti attiva che, creando un proprio Thread
   superviusionano all'elaborazione di Quad Splitting.
   Questi oggetti partono da soli non appena istanziati. 
   Questi oggetti sono in grado anche di pilotare la visualizzazione
   dell'immagine, una volta elaborata
   Prevede due modi di funzionamento (selezionabili chiamando con due
   tipi di parametri, i due costruttori): l'elaborazione vera e propria
   a partire dall'immagine originale, ed il caricamento di una struttura ad
   albero salvata in precedenza
*/
class ElabSplitting extends JFrame implements Runnable, pSplittingSTR {

    private Thread myThread;
    private QuadDecomposition qd = null;
    private psplitting parent;
    private BufferedImage imgIn;
    private BufferedImage imgOut;
    private PaintPane outPan;
    private float T;
    private int nSteps, nCells;
    private File fileTree;

    /**
       costruttore usato solitamente: predispone per un elaborazione normale
       partendo dall'immagine di input che verra' elaborata ottenendo
       l'immagine di uscita
    */
    public ElabSplitting(psplitting parent, 
			 BufferedImage imgIn, PaintPane outPan, float T) {
	super();

	this.parent = parent;
	this.imgIn  = imgIn;
	this.outPan = outPan;
	this.T = T;

	myThread = new Thread(this);
	myThread.start();
    }


    /** 
	costruttore che predispone al caricamento ed elaborazione
	di una struttura dati di tipo Quad Tree che permette di ottenere
	un immagine di uscita, ricostruendola dall'albero senza rifare
	l'elaborazione
    */
    public ElabSplitting(psplitting parent, PaintPane outPan, File f) {
	super();

	this.parent = parent;
	this.imgIn  = null;
	this.outPan = outPan;
	this.T = 0;
	this.fileTree = f;

	myThread = new Thread(this);
	myThread.start();
    }

    public void run() {
	try {
	    if (imgIn == null) {
		// si elabora la struttura ad albero
		qd = new QuadDecomposition(50);
		imgOut = qd.loadTree(new FileInputStream(fileTree));

		outPan.load(imgOut);   // provoca la visualizza dell'img.

		parent.guiActionAfterElaboration(0, 0);

	    } else {
		// elaborazione normale, a partire dall'immagine di input
		qd = new QuadDecomposition(imgIn, T);

		imgOut = qd.elaboration();

		outPan.load(imgOut);   // provoca la visualizza dell'img.

		nSteps = qd.getNSteps();
		if (nSteps == 0) nSteps = 1;
		nCells = qd.getNCells();
		if (nCells == 0) nCells = 1;
		parent.guiActionAfterElaboration(nSteps, nCells);
	    }
	}
	catch ( QDWrongImageFormat e) {  
	    JOptionPane.showMessageDialog(parent,
					  e.getMessage(),
					  ERR_ELAB_IMG,
					  JOptionPane.WARNING_MESSAGE);   
	}
	catch( QDElabException e ) {
	    JOptionPane.showMessageDialog(parent,
					  e.getMessage(),
					  ERR_ELAB_IMG,
					  JOptionPane.WARNING_MESSAGE);   
	}
	catch( IOException e ) {
	    JOptionPane.showMessageDialog(parent,
					  e.getMessage(),
					  ERR_I0,
					  JOptionPane.WARNING_MESSAGE);   
	}
    }

    public BufferedImage getElabImg() { return imgOut;}

    public QuadDecomposition getQDHandle() { return qd; }

    public Thread getThread() { return myThread; }
}
