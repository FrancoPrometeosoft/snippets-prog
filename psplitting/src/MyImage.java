/**  MyImage.java


  AUTHOR   RANCAN  FRANCO  

DESCRIPTION:		Interfacce e Classi specializzate per la gestione di 
			alcuni tipi di immagini

			tipi di immagini supportate:
			PGM P2,  PGM P5                 

*/

import java.io.*;
import java.awt.*;
import java.awt.image.*;


/**
  definisce i metodi fondamentali per il caricamento ed il salvataggio
  implementabili nei vari tipi di formati di immagine
*/
interface MyImage {

    /**
       carica l'immagine in una BufferedImage, leggendola da InputStream
    */
    BufferedImage load(InputStream in) 
	throws IOException, MyImgUnknownFormat, MyImgParseException;

    /**
       salva la BufferedImage, sull' OutputStream.
       E' possibile fornire un commento che verra' messo nell'header
       del file immagine (se consentito)
    */
    void save(BufferedImage img, OutputStream out, String comment)
	throws IOException, MyImageWrongFormat;

    /**
       restituisce una matrice di interi ottenuti dai pixel dell'immagine
    */ 
    int [][] getPixelsMatrix();
}


/**
  permette di rendere ridefinibile l'operazione di creazione delle
  classi che implementano MyImage
*/
interface MyImageFactory {
    MyImage createMyImage()
	throws IOException, MyImgUnknownFormat;
}


/**
  definizione di stringhe costanti utili nella generazioni dei messaggi
  (si evita di usare "magic values" cablati nel codice)
*/
interface MyImageSTR {
    // qui si mettono le definizioni delle stringhe dei messaggi
    // comuni, nel linguaggio che si preferisce
    //
    String STR_ERR_UNKNOWN_FORMAT = "L'immagine non e' in Formato ";
    String NUMBER_EXPECTED = "atteso un numero";
    String STR_WIDTH  = "larghezza";
    String STR_HEIGHT = "altezza";
    String STR_MAX_GREY_VALUE = "max valore di grigio";
    String STR_NO_GRAY_IMAGE = "Non e' un'immagine a scala di grigi";
}



/**
  riunisce le parti comuni per elaborare i dei tipi di immagine 
  PGM P2 e P5
*/
abstract class PGMFilter implements MyImage, MyImageSTR {

    //stringhe costanti dei messaggi
    protected static final char   PGM_PREFIX = 'P'; 
    protected static final char   COMMENT_PREFIX = '#'; 
    protected static final int    MAX_GRAY_VALUE = 255;
    protected static final String STR_ERR_MAX_GREY = 
	"Questo programma processa solo immagini con 8-bit di scala" +
	" di grigi";
    protected static final String STR_ERR_READ_VALUE = 
	"Valore non corretto o dati insufficienti nello stream. Pos = ";

    protected int width, height, maxGreyValue;
    protected StreamTokenizer tokenizer;
    
    protected int [][]pixelsMatrix;

    abstract public BufferedImage load(InputStream in) 
	throws IOException, MyImgUnknownFormat, MyImgParseException;

    abstract public void save(BufferedImage img, OutputStream out, 
			      String comment)
	throws IOException, MyImageWrongFormat;

    public int [][] getPixelsMatrix() { return this.pixelsMatrix; }
 
    protected void loadHeader(String ID, int id_number, BufferedReader r)
	throws IOException, MyImgUnknownFormat, MyImgParseException {

	tokenizer = new StreamTokenizer(r);

	// parsa il magic numer 
	r.mark(0);
	r.reset();
	int a = r.read();
	if (a != (int)PGM_PREFIX) throw new MyImgUnknownFormat
			  (STR_ERR_UNKNOWN_FORMAT + ID);
	if ((tokenizer.nextToken() != StreamTokenizer.TT_NUMBER) ||
	    (tokenizer.nval != id_number))
	    throw new MyImgUnknownFormat(STR_ERR_UNKNOWN_FORMAT + ID);

	tokenizer.commentChar(COMMENT_PREFIX);

	if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
	    throw new MyImgParseException(ID + " " + NUMBER_EXPECTED + " (" +
					   STR_WIDTH + ")");
	width = (int)tokenizer.nval;

	//System.out.println(STR_WIDTH + " " + width);

	if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
	    throw new MyImgParseException(ID + " " + NUMBER_EXPECTED + " (" +
					  STR_HEIGHT + ")");
	height = (int)tokenizer.nval;

	//System.out.println(STR_HEIGHT + " " + height);

	if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
	    throw new MyImgParseException(ID + " " + NUMBER_EXPECTED + " (" +
					   STR_MAX_GREY_VALUE + ")");
	maxGreyValue = (int)tokenizer.nval;
	if (maxGreyValue != MAX_GRAY_VALUE) 
	    throw new MyImgParseException(ID + " " + STR_ERR_MAX_GREY);

	//System.out.println("max livelli di grigio " + maxGreyValue);
    }

}//PGMFilter


/**
  Tratta le immagini PGM P2 in modo conforme all'interfaccia MyImage
*/
class PGMP2Filter extends PGMFilter implements MyImageSTR  {

    private static final String ID     = "PGM P2";
    private static final int    ID_NUM = 2;

    private BufferedImage img;

    public BufferedImage load(InputStream in) 
	throws IOException, MyImgUnknownFormat, MyImgParseException {

	BufferedReader r = null;
	WritableRaster raster;
	int pos;

	//System.out.println("entrata nel metodo Apertura immagine PGM2");
	
	pos = 0;
	try {
	    r = new BufferedReader(new InputStreamReader(in));
	    
	    this.loadHeader(ID, ID_NUM, r);

	    pixelsMatrix = new int[width] [height];

	    raster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
					       width, height, 1, new Point());

	    for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++) {
		    pos++;
		    if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
			throw new MyImgParseException
			    (ID + " " + NUMBER_EXPECTED + ". " +
			     STR_ERR_READ_VALUE + pos);
		    int tmp =  (int)tokenizer.nval;
		    if ((tmp < 0) || (tmp > maxGreyValue))
			throw new MyImgParseException(ID + " " + 
						      STR_ERR_READ_VALUE+ pos);
		    raster.setSample(x, y, 0, tmp);

		    pixelsMatrix[x][y] = tmp;
		}
	} 
	catch(IOException e) {
	    throw new MyImgParseException(ID + " " + STR_ERR_READ_VALUE + pos);
	}
	finally {
	    if (r != null)
		r.close();
	}

	img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
	img.setData(raster);

	//System.out.println("Immagine caricata:\n" + img);

	return img;
    }

    public void save(BufferedImage img, OutputStream out, 
		     String comment) 
	throws IOException, MyImageWrongFormat {

	final int MAX_ITEMS_FOR_LINE = 16;

	Raster raster;
	int width, height, pos;
	PrintWriter w = null;
	String str;

	if (img.getType() != BufferedImage.TYPE_BYTE_GRAY)
	    throw new MyImageWrongFormat(STR_NO_GRAY_IMAGE);

	raster = img.getData();	

	try {
	    w = new PrintWriter(new OutputStreamWriter(out));

	    width  = raster.getWidth();
	    height = raster.getHeight();

	    w.print(PGM_PREFIX);
	    w.println(ID_NUM);
	    if (comment.length() > 0)
		w.println(COMMENT_PREFIX + comment);
	    w.println(width + " " + height + " "); 
	    
	    w.println(MAX_GRAY_VALUE + " ");   // si mette il max value

	    pos = 0;
	    for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++) {
		    pos++;
		    w.print(raster.getSample(x,y,0) + " ");
		    if (pos >= MAX_ITEMS_FOR_LINE) {
			w.println();
			pos = 0;
		    }
		}

	    w.flush();
	}
	finally {
	    if (w != null)
		w.close();
	}
    }

}// PGMP2Filter



/**
  permette di costruire una classe conforme all'interfaccia MyImage
  che tratta immagini PGM P2
*/
class PGMP2Factory implements MyImageFactory {

    public MyImage createMyImage()
	throws IOException, MyImgUnknownFormat {

	MyImage img = new PGMP2Filter();
	return img;
    }
}



/**
  Tratta le immagini PGM P5 in modo conforme all'interfaccia MyImage
*/
class PGMP5Filter extends PGMFilter implements MyImageSTR  {

    private static final String ID     = "PGM P5";
    private static final int    ID_NUM = 5;

    private BufferedImage img;

    public BufferedImage load(InputStream in) 
	throws IOException, MyImgUnknownFormat, MyImgParseException {

	BufferedReader r = null;
	WritableRaster raster;
	int pos;

	//System.out.println("entrata nel metodo Apertura immagine PGM2");

	pos = 0;
	try {
	    r = new BufferedReader(new InputStreamReader(in));

	    this.loadHeader(ID, ID_NUM, r);

	    pixelsMatrix = new int[width] [height];

	    raster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 
					       width, height,1, new Point());
	    for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++) {
		    pos++;

		    int tmp = r.read();
		    if ((tmp < 0) || (tmp > maxGreyValue))
			throw new MyImgParseException(ID + " " + 
						      STR_ERR_READ_VALUE+ pos);
		    raster.setSample(x, y, 0, tmp);

		    pixelsMatrix[x][y] = tmp;
		}
	} 
	catch(IOException e) {
	    throw new MyImgParseException(ID + " " + STR_ERR_READ_VALUE + pos);
	}
	finally {
	    if (r != null)
		r.close();
	}

	img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
	img.setData(raster);

	//System.out.println("Immagine caricata:\n" + img);

	return img;
    }


    public void save(BufferedImage img, OutputStream out, 
		     String comment) 
	throws IOException, MyImageWrongFormat {

	Raster raster;
	int width, height;
	PrintWriter w = null;
	String strHeader;

	if (img.getType() != BufferedImage.TYPE_BYTE_GRAY)
	    throw new MyImageWrongFormat(STR_NO_GRAY_IMAGE);

	raster = img.getData();	
	try {
	    w = new PrintWriter(new OutputStreamWriter(out));

	    width  = raster.getWidth();
	    height = raster.getHeight();

	    w.print(PGM_PREFIX);
	    w.println(ID_NUM);
	    if (comment.length() > 0)
		w.println(COMMENT_PREFIX + comment);
	    w.println(width + " " + height);

	    w.println(MAX_GRAY_VALUE);   // si mette il max value

	    for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		    w.write(raster.getSample(x,y,0));

	    w.flush();
	}
	finally {
	    if (w != null)
		w.close();
	}
    }

}// PGMP5Filter


/**
  permette di costruire una classe conforme all'interfaccia MyImage
  che tratta immagini PGM P5
*/
class PGMP5Factory implements MyImageFactory {

    public MyImage createMyImage()
	throws IOException, MyImgUnknownFormat {

	MyImage img = new PGMP5Filter();
	return img;
    }
}



/**
   L'immagine non e' riconosciuta da nessuna di queste classi
*/
class MyImgUnknownFormat extends Exception {
    public MyImgUnknownFormat(String msg) {
	super(msg);
    }
}


/**
  Esiste un errore nel file dell'immagine che non e' conforme al 
  suo standard
*/
class MyImgParseException extends Exception {
    public MyImgParseException(String msg) {
	super(msg);
    }
}


/**
  esite un errore nel formato dell'immagine, non conforme al suo
  standard
*/
class MyImageWrongFormat extends Exception {
    public MyImageWrongFormat(String msg) {
	super(msg);
    }
}
