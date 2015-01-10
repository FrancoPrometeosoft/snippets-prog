/**  QuadDecomposition.java

  
 
  AUTHOR   RANCAN  FRANCO  

DESCRIPTION:		Algoritmo di quad splitting su immagini a scala di  
			grigio 


*/

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;


/**
   Classe principale che contiene tutti i metodi accessibili 
   dall'esterno che consentono l'elaborazione.
   L'elaborazione si avvale della creazione di 4 Thread che corrispondono
   alle prime 4 suddivisioni. L'elaborazione di Queste suddivisioni avviene
   in modo parallelo.
*/
public class QuadDecomposition  {

    protected static final String STR_NULL_IMAGE = 
	"Non e' stata caricata nessuna immagine";
    protected static final String STR_NO_GRAY_IMAGE = 
	"Non e' un'immagine a scala di grigi";
    protected static final String STR_NO_SQUARE_IMAGE = 
	"Questo programma elabora solo immagini quadrate";
    protected static final String STR_DIM_IMAGE_NO_POW_2 = 
	"Questo programma elabora solo immagini con dim. potenza di 2";
    protected static final String STR_ERR_IMG_RASTER = 
	"E' stato rilevato un errore nei raster dell'immmagine.\n" +
	"L'elaborazione non puo' continuare";
    protected static final String STR_NO_TREE = 
	"Non esiste nessuna struttura QDTree derivante da un'elab. ";

    private WritableRaster rasterW;
    private Raster rasterR       = null;
    private BufferedImage imgIn  = null;
    private BufferedImage imgOut = null;
    private float threshold;  
    private QDNodeT qdRoot       = null;
    private int nSteps, nCells;
    private boolean isAlreadyAverage = false;

    public QuadDecomposition(BufferedImage img, float T) 
	throws QDWrongImageFormat {

	if (img == null)
	    throw new QDWrongImageFormat(STR_NULL_IMAGE);

	if (img.getType() != BufferedImage.TYPE_BYTE_GRAY)
	    throw new QDWrongImageFormat(STR_NO_GRAY_IMAGE);

	if (img.getWidth() != (img.getHeight()))
	    throw new QDWrongImageFormat(STR_NO_SQUARE_IMAGE);

	// si controlla che la dim dell'immagine sia una potenza di 2
	//

	int w = img.getWidth();
	int l = (int)(Math.log((float)w) / Math.log(2.0));
	int p = (int)Math.pow(2.0,(float)l);
	if (w != p)
	    throw new QDWrongImageFormat(STR_DIM_IMAGE_NO_POW_2);

	this.imgIn = img;
	this.threshold = T;
    }


    /** 
	costruttore che serve per creare un oggetto QuadDecomposition
	vuoto.  Verra' riempito da un'immagine di input dopo la chiamata
	del metodo loadTree() che carica un oggetto di tipo QuadTree
    */
    public QuadDecomposition(float T) 
	throws QDWrongImageFormat {

	this.threshold = T;
    }



    /**
       metodo che permette l'esecuzione dell'elaborazione
       restituisce una BufferedImage che contiene il risultato
    */
    public BufferedImage elaboration() throws QDElabException {

	rasterR = imgIn.getData();

	rasterW = 
	    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
				      rasterR.getWidth(),rasterR.getHeight(),
				      1, new Point());
	try {

	    int ofsX, ofsY;
	    ofsX = ofsY = 0;

	    // Per iniziare l'elaborazione, si deve prima creare
	    // la struttura ad albero che conterra' i dati principali
	    // (che possono permettere di ricostruire l'immagine)
	    //
	    // Si istanzia un oggetto QDTreeRoot che permette di costruire
	    // il nodo radice dell'albero 
	    QDTreeRoot qdr = new QDTreeRoot(rasterR, rasterW, 
					    ofsX, ofsY, threshold);  

	    // costruisce il primo nodo e calcola la media e la varianza
	    // dell'intera immagine
	    qdRoot = qdr.rootNodeCreate();

	    this.isAlreadyAverage = true;

            /*
	    System.out.println("Dati immagine iniziale  "+
			       "  media " + qdRoot.getAverage() +
			       "  sigma " + qdRoot.getSigma());
            */
	    

	    // primo test della varianza sigma sull'intera immagine
	    //
	    if (qdRoot.getSigma() > threshold) {
   
		int w = rasterR.getWidth()  / 2;
		int h = rasterR.getHeight() / 2;

		// si istanziano 4 oggetti di tipo QDTree
		// essi sono oggetti attivi: non appena inizializzati
		// partono con un loro Thread indipendente
		// ognuno di essi procede nel calcolo e nella costruzione
		// dei sottoalberi.
		// Come effetti collaterali, scrivono il rasterW di uscita
		// (ognuno nella porzione di raster che gli compete)

		QDTree qdt0 = new QDTree(rasterR, rasterW, 0, 0,
					 threshold, qdRoot, 0,
					 0, 0, w, h);

		QDTree qdt1 = new QDTree(rasterR, rasterW, w, 0,
					 threshold, qdRoot, 1,
					 0, 0, w, h);

		QDTree qdt2 = new QDTree(rasterR, rasterW, 0, h,
					 threshold, qdRoot, 2,
					 0, 0, w, h);

		QDTree qdt3 = new QDTree(rasterR, rasterW, w, h,
					 threshold, qdRoot, 3,
					 0, 0, w, h);

		// per attendere la fine dei singoli thread
		// si utilizza il metodo join() sul Thread specifico
		// che ha il vantaggio di mettersi in wait e svegliarsi
		// automaticamente non appena il Thread ha finito
		//
		try {
		    qdt0.getThreadID().join();
		    qdt1.getThreadID().join();
		    qdt2.getThreadID().join();
		    qdt3.getThreadID().join();
		}
		catch(InterruptedException e ) {;}
		
		nSteps = qdt0.getNSteps() + qdt1.getNSteps() + 
		    qdt2.getNSteps() +  qdt3.getNSteps();

		nCells = qdt0.getNCells() + qdt1.getNCells() + 
		    qdt2.getNCells() + qdt3.getNCells();

                /*
		System.out.println("Completato in " + this.getNSteps()+
				   " passi. costruite " + this.getNCells() +
				   " celle."); 
                */     
	    } else {
		// l'immagine di partenza e' gia' omogenea rispetto
		// alla soglia
		// si disegna uniformemente il valore medio
		//
		int [] pixels = new int[qdRoot.getW()* qdRoot.getH()];
		Arrays.fill(pixels, qdRoot.getAverage());
		rasterW.setSamples(0, 0, qdRoot.getW(), qdRoot.getH(),
				   0, pixels);
	    }
	}
	catch(QDNullPointerException e) {
	    throw new QDElabException(STR_ERR_IMG_RASTER + "/n  " +
				      e.getMessage());
	}

	imgOut = new BufferedImage(rasterW.getWidth(),rasterW.getHeight(),
				   BufferedImage.TYPE_BYTE_GRAY);
	imgOut.setData(rasterW);

	return imgOut;
    }


    /**
       metodo che permette il calcolo della media dei valori dei pixels
       dell'immagine  iniziale (l'intera immagine)
    */
    public int getAverageWholeImage() {
	if (this.isAlreadyAverage) return qdRoot.getAverage();
	else {
	    averigeCalculus(); 
	    if (this.isAlreadyAverage) return qdRoot.getAverage();
	    else return 0;
	}
    }



    /**
       metodo che permette il calcolo della varianza dell'immagine
       iniziale (l'intera immagine)
    */
    public float getSigmaWholeImage() {
	if (this.isAlreadyAverage) return qdRoot.getSigma();
	else {
	    averigeCalculus();
	    if (this.isAlreadyAverage) return qdRoot.getSigma();
	    else return (float)0.0;
	}
    }


    /**
       metodo che permette il calcolo della media e della varianza
       dei valori dei pixels  dell'immagine  iniziale (l'intera immagine)
    */
    private void averigeCalculus() {

	rasterR = imgIn.getData();

	rasterW = 
	    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
				      rasterR.getWidth(),
				      rasterR.getHeight(),
				      1, new Point());
	try {
	    QDTreeRoot qdr = new QDTreeRoot(rasterR, rasterW, 0, 0, 
						(float)0.0); 

	    // costruisce il primo nodo e calcola la media
	    qdRoot = qdr.rootNodeCreate();
	    this.isAlreadyAverage = true;
	}
	catch(QDNullPointerException e) {;}
    }


    /**
       restituisce il numero di nodi creati, cioe' il n. di zone calcolate
    */
    public int getNSteps() { return nSteps; }

    /**
       restituisce il numero di zone omogenee calcolate
    */
    public int getNCells() { return nCells; }


    /**
       permette di salvare l'albero dati su file
    */
    public void saveTree( OutputStream out ) 
	throws IOException, QDNoTreeException {

	ObjectOutputStream w = null;

	if (qdRoot == null) 
	    throw new QDNoTreeException(STR_NO_TREE);
	try {
	    w = new ObjectOutputStream(new BufferedOutputStream(out));
	    w.writeObject(qdRoot);
	    w.flush();
	}
	finally {
	    if (w != null)
		w.close();
	}
    }

    /** 
	A partire da un file dati salvato
	carica una struttura dati di tipi QuadTree
	carica i raster
	restituisce una BufferedImage
	ATTENZIONE: in questa versione NON si effettuano controlli
	sulla provenuenza dei dati: si suppone che il file sia proprio 
	un file del tipo voluto
    */
    public BufferedImage loadTree( InputStream in ) 
	throws IOException {

	ObjectInputStream r = null;

	try {
	    r = new ObjectInputStream(new BufferedInputStream(in));
	    qdRoot = (QDNodeT)r.readObject();
	}
	catch( ClassNotFoundException e ) { ; } 
	finally {
	    if (r != null)
		r.close();
	}

	rasterW = 
	    Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
				      qdRoot.getW(), qdRoot.getH(),
				      1, new Point());

	this.nodeVisit(qdRoot); 

	imgOut = new BufferedImage(rasterW.getWidth(),rasterW.getHeight(),
				   BufferedImage.TYPE_BYTE_GRAY);
	imgOut.setData(rasterW);

	return imgOut;
    }


    /** 
	metodo privato ricorsivo
	Visita tutti i nodi e carica la porzione del rasterW in 
	corrispondenza delle foglie
	Serve per poter ricostruire l'immagine a partire dalla struttura
	dati ad albero
    */
    private void nodeVisit(QDNodeT node) {
	int [] pixels;
	int w, h, np;

	for (int i = 0; i < QDTree.N_CHILDS; i++) {
	    if (node.getChild(i) == null) {
		// nodo foglia
		w = node.getW();
		h = node.getH();
		np = w * h;
		
		pixels = new int[np];

		// scrive la porzione sul rasterW
		if ((w != 1) || (h != 1)) {
		    // area di pixels
		    Arrays.fill(pixels, node.getAverage());
		    rasterW.setSamples(node.getX(), node.getY(),
				       w, h, 0, pixels); 
		} else {
		    // singolo pixel
		    rasterW.setSample(node.getX(), node.getY(),
				      0, node.getAverage());
		}

		/*
		System.out.println("----------- media " + node.getAverage());
		System.out.println(" x " + node.getX() + "  y " + node.getY()+
				   "  w  " + w + "   h  " + h);
		*/

	    } else {
		// chiamata ricorsiva
		this.nodeVisit(node.getChild(i));
	    }
	}
    }

} // QuadDecomposition



/**
   classe che rappresenta i nodi dell'albero QDTree
   ognuno di questi nodi contiene il risultato del calcolo
   della media e della varianza dell'area di pixel specificata
   nei suoi attributi
*/
class QDNodeT implements Serializable {
    private int mu;
    private float sigma;
    private int x, y, w, h;
    private QDNodeT [] child;

    public QDNodeT(QDNodeT parent, int parentID,
		   int x, int y, int w, int h,
		   int mu, float sigma) {

	this.x = x;  this.y = y; 
	this.w = w;  this.h = h;
	this. mu = mu;
	this.sigma = sigma;

	child = new QDNodeT[QDTree.N_CHILDS];
	for (int i = 0; i < QDTree.N_CHILDS; i++)
	    child[i] = null;
	if (parent != null)
	    parent.setChild(parentID, this);
    }

    public void setChild(int childID, QDNodeT node) {
	child[childID] = node;
    }

    public QDNodeT getChild(int childID) {
	return child[childID];
    }

    public int getAverage() { return mu; }

    public float getSigma() { return sigma; }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getW() { return w; }

    public int getH() { return h; }

} //QDNodeT


/** 
    la classe QDTreeRoot definisce gli attributi ed i metodi
    principali per inizializzare l'elaborazione 
    Essa' verra' estesa dalla classe QDTree che ha i metodi piu' specifici
    L'importa di questa classe e' anche quella di permettere la creazione
    del nodo radice dell'albero di elaborazione
*/
class QDTreeRoot {
    protected static final String STR_NO_PARENT_SPEC = 
	"Non e' stato specificato un nodo radice per l'albero QDTree";
    protected static final String STR_NO_RASTER_R =
	"Raster di lettura null";
    protected static final String STR_NO_RASTER_W =
	"Raster di scrittura null";

    public static final int N_CHILDS = 4;

    protected QDNodeT root = null;

    protected float threshold;  // la soglia della sigma
    protected Raster rasterR;    
    protected WritableRaster rasterW;

    protected int ofsX, ofsY;       // offset di posizione coordinate
 
    public QDTreeRoot(Raster rasterR, WritableRaster rasterW, 
			   int ofsX, int ofsY, float t) 
	throws QDNullPointerException {

	if (rasterR == null)
	    throw new QDNullPointerException(STR_NO_RASTER_R);

	if (rasterW == null)
	    throw new QDNullPointerException(STR_NO_RASTER_W);

	this.threshold = t;
	this.rasterR = rasterR;
	this.rasterW = rasterW;
	this.ofsX = ofsX;
	this.ofsY = ofsY;
    }


    /**
       questo metodo va chiamato prima di iniziare l'elaborazione
       esso restituisce il nodo radice dell'albero
    */
    public QDNodeT rootNodeCreate () throws QDNullPointerException {

	root =  nodeCreate(null, 0, 
			   0, 0, rasterR.getWidth(), rasterR.getHeight());
	return root;
    }


    /**
       metodo interno per l'istanzione dei nodi dell'albero
       Utilizza dei metodi specifici per il calcolo della media e della
       varianza
    */
    protected QDNodeT nodeCreate(QDNodeT parent, int parentID,
				 int x, int y, int h, int w)
	throws QDNullPointerException {

	QDNodeT node;
	int   mu = 0;
	float sigma = 0;
	int [] pixels;
	int np;

	np = w * h;

	if ((w != 1) || (h != 1)) {
	    // gruppo di pixels
	    //
	    pixels = new int[np];
	    pixels = rasterR.getPixels(x + ofsX, y + ofsY, w, h, pixels);
	    
	    mu    = averige(pixels);
	    sigma = variants(pixels, mu);

	} else {
	    // singolo pixel
	    mu    = rasterR.getSample(x + ofsX, y + ofsY, 0);
	    sigma = (float)0;
	}

	node = new QDNodeT(parent, parentID, x + ofsX, y + ofsY, h, w,
			   mu, sigma);

	return node;
    }


    protected int averige(int [] pixels) {
	// calcolo della media
	long mu = 0;
	int np;

	np = pixels.length;
	for (int i = 0; i < np; i++)
	    mu += pixels[i];
	float muF = (float)(mu / np);  // per maggiore precisione 
	mu /= np;

	return (int)mu;
    }	    

    protected float variants(int [] pixels, int mu) {
	// calcolo della varianza
	double sigma = 0;
	int np, tmp;

	np = pixels.length;
	for (int i = 0; i < np; i++) {
	    tmp = pixels[i] - mu;
	    sigma += (tmp * tmp);
	}
	sigma /= (np -1);   

	return (float)sigma;
    }

} //QDTreeRoot



/** 
    La classe QDTree permette di instanziare oggetti di tipo attivo
    in quanto per ognuno di essi viene istanziato un proprio Thread
    questa classe contiene i metodi che permettono l'elaborazione
    dell'algoritmo di splitting e la creazione dell'albero quad tree
    Viene utilizzato un metodo ricorsivo
    Ha anche l'importante compito di scrivere i risultati intermedi
    direttamente nel rasterW di uscita (nella porzione che gli compete)
*/
class QDTree extends QDTreeRoot implements Runnable {

    private boolean finish;
    private int nSteps;
    private int nCells;
    private int myID, initialX, initialY, initialW, initialH;
    private Thread selfThread;

    /**
       predispone per l'elaborazione
       fa partire automaticamente un suo thread
       L'identificazione del Thread e' accessibile utilizzando
       un apposito metodo
    */
    public QDTree (Raster rasterR, WritableRaster rasterW, 
		   int ofsX, int ofsY, float t,
		   QDNodeT parent, int myID,
		   int x, int y, int w, int h)
	throws QDNullPointerException {

	super(rasterR, rasterW, ofsX, ofsY, t);

	if (parent == null)
	    throw new QDNullPointerException(STR_NO_PARENT_SPEC);

	this.root = parent;
	this.myID = myID;
	this.initialX = x;   this.initialY = y;
	this.initialW = w;   this.initialH = h;
	this.nSteps   = 0;   this.nCells   = 0;     
	this.finish   = false;

	selfThread = new Thread(this);
	selfThread.start();
    }
 
   
    public void run() {
	treeCreate(root, myID, initialX, initialY, initialW, initialH);

	this.finish = true;

	// System.out.println("Sono il processo " + myID + "  HO FINITO !");
    }


    /**
       e' possibile chiamare questo metodo per sapere se ha finito.
       Tuttavia e' consigliabile usare il metodo nativo join() sapendo
       chi e' questo Thread (usare il metodo getThreadID()
    */
    public boolean isFinish() { return this.finish; }


    /**
       fornisce il Thread utilizzato
       Indispensabile per accedere a metodi sul Thread con join()
    */
    public Thread getThreadID() {
	// fornisce il proprio Thread
	return selfThread;
    }


    /**
       metodo ricorsivo che ha diversi compiti:
         crea i nodi di un albero QDTree
         per ogni area (rappresentata da un nodo dell'albero)
         viene  calcolata la media e la varianza
	 se e' minore alla soglia, scrive nel raster di uscita rasterW
	 il valore omogeneo, altrimenti prosegue ricorsivamente creando
	 altre quattro sottoaree
    */
    private void treeCreate(QDNodeT parent, int parentID,
			   int xNode, int yNode, int wNode, int hNode) { 
	QDNodeT node = null;
	int np;

	np = wNode * hNode;

	nSteps++;  // conteggio dei nodi elaborati

	try {
	    node = this.nodeCreate(parent, parentID, 
				   xNode, yNode, wNode, hNode);
	}
	catch(QDNullPointerException e) { 
	    ;
	}


	/*	
	System.out.println("******** dati nodo =           " +
			   xNode + ", " +
			   yNode + "  " +
			   (xNode + wNode -1) + ",  " +
			   (yNode + hNode -1) + "          " +
			   " w = " + wNode + "  h " + hNode);
	
	System.out.println("mu = " + node.getAverage() + 
			   "  sigma = " + node.getSigma());
	*/

	/*
	// questo blocco di codice
	// serve per debug per fare dei test 
	// sulla succesione forzata dei processi
	//
	try {
	    switch(myID) {
	    case 0:Thread.sleep(1);  break;
	    case 1:Thread.sleep(1); break;
	    case 2:Thread.sleep(1); break;
	    case 3:Thread.sleep(1); break;
	    }
	}
	catch(InterruptedException e) {;}
	*/

	// test della varianza sigma
	if (node.getSigma() <= threshold) {
	    // si e' trovata una zona omogenea.
	    // condizione che fa uscire dal metodo
	    //
	    nCells++;  // conteggio dei nodi foglia (terminali)
	    
	    // scrive la porzione sul rasterW
	    if ((wNode != 1) || (hNode != 1)) {
		// area di pixels
		int [] pixels = new int[np];
		Arrays.fill(pixels, node.getAverage());
		rasterW.setSamples(ofsX + xNode, ofsY + yNode, 
				   wNode, hNode, 0, pixels);
		// per come e' costruito l'arry pixels si e' certi che 
		// la precedente istruzione non sollevera' eccezioni

		// pixels = null; // per il garbage collector
	    } else {
		// singolo pixel
		rasterW.setSample(ofsX + xNode, ofsY + yNode, 
				  0, node.getAverage());
	    }

	} else {

	    // si deve suddividere ulteriormente in quattro
	    // porzioni uguali
	    // l'elaborazione e' di tipo ricorsivo: si richiama
	    // questo metodo
	    //
	    int w = wNode / 2;
	    int h = hNode / 2;

	    this.treeCreate(node, 0, xNode,     yNode,     w, h);   
	    
	    this.treeCreate(node, 1, xNode + w, yNode,     w, h); 

	    this.treeCreate(node, 2, xNode,     yNode + h, w, h);  
	    
	    this.treeCreate(node, 3, xNode + w, yNode + h, w, h);
	}
    }

    public int getNSteps() { return nSteps; }

    public int getNCells() { return nCells; }

} // QDTree


/**
  Tipo di immagine non supportata
*/
class QDWrongImageFormat extends Exception {
    public QDWrongImageFormat(String msg) {
	super(msg);
    }
}


/**
  Errore per passaggio paramentro puntatorie nullo
*/
class QDNullPointerException extends Exception {
    public QDNullPointerException(String msg) {
	super(msg);
    }
}



/**
  Errore di elaborazione
*/
class QDElabException extends Exception {
    public QDElabException(String msg) {
	super(msg);
    }
}


/**
  Non esiste l'albero QDTree
*/
class QDNoTreeException extends Exception {
    public QDNoTreeException(String msg) {
	super(msg);
    }
}
