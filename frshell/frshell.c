/*****************************************************************************/
/*                                                                           */
/*                                                                           */
/* AUTHOR:    Franco Rancan                                                  */
/*                                                                           */
/* DATA:      Dec 2013                                                       */
/*                                                                           */
/* File name: frshell.c                                                      */
/*                                                                           */
/*                                                                           */
/*****************************************************************************/

*/
#define _debug_print
#define _debug_pipe_exec




#include <sys/types.h>
#include <sys/stat.h>
#include <sys/param.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <string.h>

/*-------------------------  def e  macro  ---------------------------------*/

#define FALSE 0
#define TRUE  1


/* def. language just one set to 1 */
#define ENG_LANG 0
#define ITA_LANG 1


#define LEN_MAX_COMMAND_LINE     1023
#define LEN_MAX_NAMES_VARIOUS     128
#define LEN_MAX_OUTPUT_VARIOUS    512 
#define LEN_MAX_INPUT            2047
#define LEN_MAX_MSG_ERROR         255
#define LEN_MAX_PROMPT            255
#define TAB_JOB_LIST_INCREASE      32
#define N_MAX_ARG                 128
#define N_MAX_COMMANDS_HISTORY     64


#define CHAR_PIPE                '|'
#define CHAR_BACKGROUND          '&'
#define CHAR_SINGLE_QUOTE        '\''
#define CHAR_DOUBLE_QUOTE        '\"'
#define CHAR_SEPARATOR_1         ' '
#define CHAR_SEPARATOR_2         '\t'
#define CHAR_COMMANDS_SEPARATOR  ';'
#define CHAR_REDIRECT_INPUT      '<'
#define CHAR_REDIRECT_OUTPUT     '>'
#define CHAR_STD_ERROR           '2'

/* shell internal commands */
#define INT_CMD_EXIT             "exit"     
#define INT_CMD_CHANGE_DIR       "cd"
#define INT_CMD_PRINT_CUR_DIR    "pwd"
#define INT_CMD_PRINT_VERSION    "version"


/*------------------------- const  globali ---------------------------------*/

const char AUTHOR[] = "Franco RANCAN";

const char VERSION[] = "version 1.0";

const char SHELL_ID[] = "FR_sh";

/*---------------------------  var globali ---------------------------------*/
  
typedef int boolean;

enum valToken {BLANK_T,                   COMMANDS_SEPARATOR_T, 	   
	       PIPE_T,                    BACKGROUND_T,         
	       REDIRECT_INPUT_T,          REDIRECT_OUTPUT_T, 
	       REDIRECT_OUTPUT_APPEND_T,  REDIRECT_ERROR_T, 
	       REDIRECT_ERROR_APPEND_T }; 

typedef int token_t;

struct jobRec {
  char *strCommand;
  int child_pid;
  boolean background;
  struct taskRec *task;
  struct taskRec *lastTask;
};

typedef struct jobRec jobRec;


struct taskRec {
  char *command;
  char **args;
  char *fileNameIn; 
  char *fileNameOut;
  char *fileNameErr;
  boolean appendFileOut;
  boolean appendFileErr;
  struct taskRec *succ;
  struct taskRec *prev;
  int fd_pipe[2];
};

typedef struct taskRec taskRec;

char g_bufferInput[LEN_MAX_COMMAND_LINE +1];  

char *g_commandsHistory[N_MAX_COMMANDS_HISTORY] = {"NULL"};

int g_currentCommand = 0;

char g_hostName[LEN_MAX_NAMES_VARIOUS +1];

uid_t g_realUser;

char *g_userName;

char g_currentDir[MAXPATHLEN];

char g_currentWorkingDir[MAXPATHLEN];
 
char g_userMark[3];

int g_dimTabJob;

jobRec **g_jobList;


/*--------------------- function declarations --------------------------*/

void initialization(void); 

void finalization(void);  

void initNewInput(void);   

void showPrompt(void);

void inputCommandLine(char *, int); 

boolean syntaxAnalysis(char *input);   /* if all ok return 1  */

int jobManager(char *input);   /* run e return 0 if have to exit */

void *mallocSafe(size_t, char *);

void taskExec(int);

void pipeExec(struct taskRec *, int fd_pipe_prec[2]);
/*
void pipeExec(struct taskRec *task, int *pid, int fd_pipe_prec[2]);
*/

void freeMemJob(int);

void zombieRecover();

/*------------------------------- MAIN -------------------------------------*/



int main(int argc, char **argv)
{
  int loop;
  int posMemC;

  initialization();      

  do {

    initNewInput();      

    showPrompt();

    inputCommandLine(g_bufferInput, LEN_MAX_COMMAND_LINE);  

    if ((g_bufferInput[0] != '\0') && ( syntaxAnalysis(g_bufferInput) )) {
      /* sintax test ok      */

      /* command line history */
      posMemC = g_currentCommand % N_MAX_COMMANDS_HISTORY;
      if (g_commandsHistory[posMemC] != NULL) free(g_commandsHistory[posMemC]);

#if ENG_LANG == 1
      g_commandsHistory[posMemC] = 
	mallocSafe(strlen(g_bufferInput) +1, "commands memorize\n");
#elif ITA_LANG == 1
      g_commandsHistory[posMemC] = 
	mallocSafe(strlen(g_bufferInput) +1, "memorizzazione comandi\n");
#endif

      strcpy(g_commandsHistory[posMemC], g_bufferInput);
      g_currentCommand++;

      #ifdef _debug_print
      printf("stringa introdotta : %s di lun = %d\n", g_bufferInput, 
	     strlen(g_bufferInput));
      #endif

      loop = jobManager(g_bufferInput);
    }
    else loop = 1;

    zombieRecover();     /* terminate zombie process verification */

  } while (loop == 1);

  finalization();
 
  exit(0);
}  




/*---------------------- utility functions   ---------------------------*/

/* strConcat
   concatena la stringa source alla stringa char aggionando anche
   la var count (l'aumenta della lung. di source)
   l'operazione ha luogo solo se non si eccede a max caratteri
*/
void strConcat(char dest[], const char *source, int *count, int max)
{
  int len;

  len = strlen(source);
  if (max > (*count + len)) {
    memcpy(&dest[*count], source, len);
    *count += len;
  }
  dest[*count] = '\0';   /* aggiunge in fondo la term. si stringa */                                    /* senza pero' aumentare count */
}


/* strConcatChar
   simile alla strConcat, ma aggiunge un carattere invece di una stringa
*/
void strConcatChar(char dest[], const char ch_source, int *count, int max)
{ 
  if (max > (*count + 1))
    dest[(*count)++] = ch_source;

  dest[*count] = '\0';   /* aggiunge in fondo la term. si stringa */                                    /* senza pero' aumentare count */
}


void errorPrintAndExit(char *msg)
{
  perror(msg);
  exit(EXIT_FAILURE);
}


/* mallocSafe
   simile alla malloc standard, si differenzia per il fatto che effettua
   un controllo se la malloc e' andata a buon fine e in caso negativo
   stampa msg sullo standard error
   inoltre azzera i byte allocati
*/
void *mallocSafe(size_t size, char *msg)
{
  void *pt;
  char *msgErr;

  if ((pt = malloc(size)) == NULL) {

#if ENG_LANG == 1
    msgErr =  strcat("reallocation error ", msg);
#elif ITA_LANG == 1
    msgErr =  strcat("Errore reallocazione ", msg);
#endif
    write(2, msgErr, strlen(msgErr)); 
    exit(EXIT_FAILURE);  
  }
  /* va bene */
  memset(pt, 0, size); /* azzera i byte allocati */
  return pt;
}


/* callocSafe
   simile alla calloc; vedi spiegazioni di mallocSafe
*/
void *callocSafe(size_t nEl, size_t size, char *msg)
{
  return mallocSafe(nEl * size, msg);
}


/*-------------------- definizione delle funzioni ---------------------------*/

void initialization(void) /* inizializzazioni principali */
{
  g_jobList = callocSafe(TAB_JOB_LIST_INCREASE, sizeof(g_jobList),
			 "allocazione tab job\n");
  g_dimTabJob = TAB_JOB_LIST_INCREASE;
}


void finalization(void)   /* operazioni da fare prima di uscire */  
{
  int i;

  /* eliminazione dello storico comandi */
  for (i=0; i < N_MAX_COMMANDS_HISTORY; i++)
    /* si devono testare ed eventualmente deallocare tutte le posizioni
       perche' si usa un accesso all'array (in fase di memorizzazione) 
       modulare (buffer circolare)
    */
    if (g_commandsHistory[i] != NULL) free(g_commandsHistory[i]);

  /* eliminazione della tabella dei job */
  free(g_jobList);
}


/* getUserName
   fornisce la stringa del nome dell' utente leggendola dal file apposito
   provvede ad allocare la stringa nello Heap
   se non trova l'utente fornisce NULL
*/
char *getUserName(uid_t codeUser)  
{
  const char FileUsersData[] = "/etc/passwd";
  const int PosUID = 2;

  char *strUser, *userTmp, *tokenTmp;
  FILE *inputFile;
  char line[LEN_MAX_INPUT];
  char strUID[20];
  int i;

  strUser = NULL;
  if ((inputFile = fopen(FileUsersData,"r")) != NULL)
    { 
      sprintf(strUID, "%d", codeUser);  /* converte uid in una stringa */
      while (!feof(inputFile))
	{
	  fgets(line, LEN_MAX_INPUT, inputFile); 
	  userTmp = strtok(line, ":");   /* legge il nome dell'utente */

	  for (i = 1; i <= PosUID; i++)
	    tokenTmp = strtok(NULL, ":");  /* accede al token dove c'e' UID */

	  if (strcmp(tokenTmp, strUID) == 0) {
	    /* si e' trovato la riga col codice utente */
	    strUser = malloc(strlen(userTmp) + 1);
	    strcpy(strUser, userTmp);
	    break; 
	  }
	}	      
      fclose(inputFile);
    }        	
		    
  return strUser;
}



void initNewInput(void)   /* inizializzazioni varie per un nuovo input */
{
  /* si settano alcune var. globali (prima di ogni input) 
     perche' potrebbero essere state modificate dall'esecuzione
     di un comando precedente richiamato dalla shell
  */

  const char MarkSuperUser[]  = "#";
  const char MarkNormalUser[] = "$";

  static int prevUser = -1;

  /* setta la var nome dell'host ) */
  if (gethostname(g_hostName, LEN_MAX_NAMES_VARIOUS) == -1) 

#if ENG_LANG == 1
    errorPrintAndExit("get host name");
#elif ITA_LANG == 1
    errorPrintAndExit("ottenimento del nome dell'host");
#endif

  else {
    strcpy(g_hostName, strtok(g_hostName, "."));/* ricava il nome a Sx del . */
  }

  /* setta la var nome dell'utente */
  g_realUser = getuid();
  if ((int)g_realUser != prevUser) {
    /* occorre ricavare il nome dell'utente perche' e' cambiato dal prec.
       oppure si e' alla prima chiamata di questa funz. */

    if (g_userName != NULL)
      free(g_userName);       /* si recupera lo spazio allocato in prec. */

    g_userName = getUserName(g_realUser);

    (g_realUser == 0) ? strcpy(g_userMark, MarkSuperUser) : 
                      strcpy(g_userMark, MarkNormalUser); 
    strcat(g_userMark, " ");
    
    prevUser = (int)g_realUser;/* serve per le future chiamate a questa fun. */
  }; 


  /* setta le var dir. corrente  */
  if ((getcwd(g_currentDir, MAXPATHLEN)) == NULL )
    errorPrintAndExit("errore lettura directory corrente");

  /* la var g_currentWorkingDir deve contenere solo l'ultima parte del path 
     quindi la si deve estrarre da g_currentDir che contiene il path completo 
     la strrchr fornisce l'ultima occorenza di '/' (un puntatore)
     in questo modo si ricava l'ultima parte del path
     inoltre va tolto ancora il / in testa, pero' se ci si trova in una
     directory interna
  */
  strcpy(g_currentWorkingDir, strrchr(g_currentDir, '/'));  
  if (strcmp(g_currentDir, g_currentWorkingDir) != 0)
    /* si toglie il car di testa */
    strcpy(g_currentWorkingDir, &g_currentWorkingDir[1]);
}



void showPrompt(void)
{
  /* mostra il prompt della shell */

  const char initialBracket[]  = "[";
  const char finalBracket[]    = "]";

  static char ptPrompt[LEN_MAX_PROMPT];
  int count = 0;

  /* si concatenano le varie stringhe (la maggior parte delle quali 
     sono settate dalla initNewInput)
  */
  strConcat(ptPrompt, initialBracket,      &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, SHELL_ID,            &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, " ",                 &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, g_userName,          &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, "@",                 &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, g_hostName,          &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, " ",                 &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, g_currentWorkingDir, &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, finalBracket,        &count, LEN_MAX_PROMPT);
  strConcat(ptPrompt, g_userMark,          &count, LEN_MAX_PROMPT);

  write(1, ptPrompt, strlen(ptPrompt));
}



/* inputCommandLine 
   gestisce l'input della linea di comando 
   lavora direttamente sulla stringa buf
   al massimo legge maxLen caratteri
*/
void inputCommandLine(char *buf, int maxLen)
{
  memset(buf, 0, maxLen -1);  /* azzera il buffer */

  read(0, buf, maxLen);  
  /* legge fino a quando non si preme invio ed al massimo carica in
     in buf  maxLen caratteri
  */

  /* si aggiunge \0 al posto del car \n */
  buf[strlen(buf) - 1] = 0;  
}



void writeErrorCharUnexpected(const char ch)
{
  /* stampa  su standard error che il car ch e' inatteso */
  
  int countStrError = 0;
  char msgError[LEN_MAX_MSG_ERROR];

  strConcat(msgError, SHELL_ID,  
	    &countStrError, LEN_MAX_MSG_ERROR);  

#if ENG_LANG == 1
  strConcat(msgError, 
	    ": syntax error near unexpected token '",
	    &countStrError, LEN_MAX_MSG_ERROR);  
#elif ITA_LANG == 1
  strConcat(msgError, 
	    ": errore di sintassi: token '",
	    &countStrError, LEN_MAX_MSG_ERROR);  
#endif

  strConcatChar(msgError, ch, &countStrError, 
  		LEN_MAX_MSG_ERROR);

#if ENG_LANG == 1
  strConcat(msgError, "'\n", 
	    &countStrError, LEN_MAX_MSG_ERROR);
#elif ITA_LANG == 1
  strConcat(msgError, "' inatteso\n", 
	    &countStrError, LEN_MAX_MSG_ERROR);
#endif

  write(2, msgError, strlen(msgError));
}

 

/* syntaxAnalysis
   funzione che analizza la stringa input e in base alle sue regole
   implementate fornisce TRUE se va bene, FALSE altrimenti. 
   Nel caso che non vada bene, fornisce sullo stderr un messaggio
   appropriato
*/
boolean syntaxAnalysis(char *input)
{
  /* sono state implementate solo le seguenti regole:

     se c'e' il carattere di pipe non ci puo' essere quello & di background
     subito dopo (o separato da spazi, ma subito dopo)
     Inoltre il car di pipe non puo' essere messo all'inizio
     e poi se c'e' deve essere seguito da almeno un car normale

     se c'e' il carattere di & non ci puo' essere quello di pipe
     subito dopo (o separato da spazi, ma subito dopo)
     Inoltre il car di background &  non puo' essere messo all'inizio

     controlla che il quoting sia almeno di tipo pari, non effettua pero'
     un controllo piu' rigoroso, ma almeno puo' consentire di individuare
     qualche distrazione dell'utente
   */
  
  int countStrError = 0;
  char msgError[LEN_MAX_MSG_ERROR];
  int count = 0;
  int len;
  int presenceOfCharBackground  = FALSE;
  int presenceOfCharPipe        = FALSE;
  int presenceOfCharSingleQuote = FALSE;
  int presenceOfCharDoubleQuote = FALSE;
  int presenceOfNormalChar      = FALSE;
  int presenceOfCharCmdSep      = FALSE;
  char ch;

  len = strlen(input);
  while (count < len) {
    ch = input[count];
    switch (ch) {

    case CHAR_SEPARATOR_1: case CHAR_SEPARATOR_2:
      break;  /* non fa niente */ 

    case CHAR_SINGLE_QUOTE:
      presenceOfCharSingleQuote = !presenceOfCharSingleQuote;
      break;

    case CHAR_DOUBLE_QUOTE:
      presenceOfCharDoubleQuote = !presenceOfCharDoubleQuote;
      break;

    case CHAR_BACKGROUND:
      if ((!presenceOfNormalChar) || presenceOfCharPipe || 
	  presenceOfCharCmdSep) 
	{
	  writeErrorCharUnexpected(CHAR_BACKGROUND);
	  return FALSE;                /* esce con cod. di errore */
	}
      presenceOfCharBackground = TRUE;
      break;

    case CHAR_PIPE:
      if ((!presenceOfNormalChar) || presenceOfCharBackground || 
	  presenceOfCharCmdSep)
	{
	  writeErrorCharUnexpected(CHAR_PIPE);
	  return FALSE;                  /* esce con cod. di errore */
	}
      presenceOfCharPipe = TRUE;
      break;

    case CHAR_COMMANDS_SEPARATOR:
      presenceOfCharCmdSep = TRUE;
      if (presenceOfCharPipe || presenceOfCharBackground) {
	writeErrorCharUnexpected(CHAR_COMMANDS_SEPARATOR);
	return FALSE;                        /* esce con cod. di errore */
      }
      break;

    default:
      /* e' un carattere non compreso tra i precedenti */
      presenceOfNormalChar = TRUE;
      presenceOfCharBackground =  presenceOfCharPipe = 
	presenceOfCharCmdSep = FALSE;
      break;

    } /* di switch */

    count++;
  } /* di while */

  if (presenceOfCharSingleQuote || presenceOfCharDoubleQuote) {
    /* uno o tutte due i tipi di virgolette sono dispari */

#if ENG_LANG == 1
   strConcat(msgError, 
	      "syntax error: have to control quoting\n",
	      &countStrError, LEN_MAX_MSG_ERROR);
#elif ITA_LANG == 1
    strConcat(msgError, 
	      "errore di sintassi: controllare le virgolette (quoting)\n",
	      &countStrError, LEN_MAX_MSG_ERROR);
#endif

    write(2, msgError, strlen(msgError));

    return FALSE;                         /* esce con cod. di errore */
  }

  if (presenceOfCharPipe) {
    /* esiste un car si pipe pendente ossia non seguito da un comando */

#if ENG_LANG == 1
    strConcat(msgError, 
	      "syntax error: one or more invalid pipe char\n",
	      &countStrError, LEN_MAX_MSG_ERROR);
#elif ITA_LANG == 1
    strConcat(msgError, 
	      "errore di sintassi: esite almeno una pipe pendente\n",
	      &countStrError, LEN_MAX_MSG_ERROR);
#endif

    write(2, msgError, strlen(msgError));

    return FALSE;                       /* esce con cod. di errore */
  }

  return 1;  /* se si arriva qui, l'analisi sintattica va bene */
}   


/* tokenExtraction
   analizza la stringa input
   restituisce un token 
   restituisce anche, tramite gli argomenti:
     nextToken la posizione del prossimo token (rispetto all'inizio della
     linea di comando originale)
     tokenType, il tipo (la semantica) del token trovato 
*/
char *tokenExtraction(char *input, int *nextToken, token_t *tokenType)
{
  int count = 0;
  int i, len;
  char *token;
  char ch, ch2;
  boolean done = FALSE;

  len = strlen(input);

  /* deve eliminare eventuali spazi iniziali 
     questa condizione puo accadere quando ad es. i token sono separati 
     da piu' spazi e alla succ. chiamata di questa funz. di presenta
     appunto, questa situazione (perche' al primo spazio questa funz. esce)
   */
  count = 0;
  while ((count < len) && 
	 ((input[count] == CHAR_SEPARATOR_1) ||
	  (input[count] == CHAR_SEPARATOR_1)))    
    count++;

  if (count > 0) {
    /* sono stati eliminati dei car separatori */
    input = &input[count]; /* riassegna ad input l'inizio della stringa */

    /* assegna a nextToken la posizione del prossimo token */
    /*
    if ((count + 1) < len)
      *nextToken += count;
    else *nextToken = len;
    */

    *nextToken += count;

    len = strlen(input);
    count = 0;
  }

  while ((count < len) && !done) {
    ch = input[count];

    switch (ch) {

    case CHAR_SEPARATOR_1: case CHAR_SEPARATOR_2:
      input[count] = '\0';   /* termina la stringa dove ha trovato il car. */

      *tokenType = BLANK_T;
      done = TRUE;
      break; 

    case CHAR_SINGLE_QUOTE: case CHAR_DOUBLE_QUOTE:
      ch2 = (ch == CHAR_SINGLE_QUOTE) ? CHAR_SINGLE_QUOTE : CHAR_DOUBLE_QUOTE;

      /* deve scorrere fino a quando non trova un car uguale */
      count++;
      while ((count < len) &&(input[count] != ch2))
	count++;

      if ((count < len) && (len < LEN_MAX_COMMAND_LINE)){
	count++;

	/* sposta a dx di un posto i caratteri per fare spazio al term. 
	   perche' in questo caso il carattere finale serve e non puo'
	   essere eliminato 
	*/
	for (i = len; i > count; i--)
	  input[i] = input[i-1];

	len++;
	input[count] = '\0';
      }

      tokenType = BLANK_T;
      done = TRUE;
      break;

    case CHAR_BACKGROUND:
      input[count] = '\0';   /* termina la stringa dove ha trovato il car. */
      *tokenType = BACKGROUND_T;
      done = TRUE;
      break;

    case CHAR_PIPE:
      input[count] = '\0';   
      *tokenType = PIPE_T;
      done = TRUE;
      break;

    case CHAR_REDIRECT_INPUT:
      input[count] = '\0';   
      *tokenType = REDIRECT_INPUT_T;
      done = TRUE;
      break;

    case CHAR_REDIRECT_OUTPUT:
      input[count] = '\0';  
      *tokenType = REDIRECT_OUTPUT_T;
      if ((count + 1) < len)
	if (input[count + 1] == CHAR_REDIRECT_OUTPUT) {
	  /* car di ridirezione doppio */
	  count++;
	  *tokenType = REDIRECT_OUTPUT_APPEND_T;
	}
      done = TRUE;
      break;

    case CHAR_STD_ERROR:
	/* questo e' un tipo di carattere (di solito e' '2'  es. 2> )
	   che puo' essere un token solo se si verificano che sia seguito
	   dal carattere di ridirezione, eventualmente doppio
	*/
      if (((count + 1) < len) && (input[count + 1] == CHAR_REDIRECT_OUTPUT)) { 
	input[count] = '\0'; 
	count++;
	*tokenType = REDIRECT_ERROR_T;
	if ((count + 1) < len)
	  if (input[count + 1] == CHAR_REDIRECT_OUTPUT) {
	    /* car di ridirezione doppio */
	    count++;
	    *tokenType = REDIRECT_ERROR_APPEND_T;
	  }
	done = TRUE;
      }
      break;

    case CHAR_COMMANDS_SEPARATOR:
      input[count] = '\0'; 
      *tokenType = COMMANDS_SEPARATOR_T;
      done = TRUE;
      break;

    default:
      /* e' un carattere non compreso tra i precedenti ossia e' 
	 un carattere normale */
      *tokenType = BLANK_T;   
      break; 

    } /* di switch */

    count++;

  } /* while */
  
  token = input;    /* inizio del token fino a dove si e' messo '\0' */

  /* assegna a nextToken la posizione del prossimo token */
  *nextToken += count;

  return token;           
}



int searchTabEntry(void)
{
  /* ricerca un elemento libero dalla tabella dei job e fornisce il
     suo indice; se non c'e' spazio, incrementa le dim. della tabella
  */
  int i;

  for(i=0; g_jobList[i] != NULL; i++);

  if (g_jobList[i] == NULL) 
    return i;
  else {

    /* non bastano le entry: vanno ampliate */

    g_dimTabJob += TAB_JOB_LIST_INCREASE;

    if ((g_jobList = realloc(g_jobList, g_dimTabJob * sizeof(g_jobList)))
	== NULL) 
      {

#if ENG_LANG == 1
	write(2,"error reallocation job tab\n",
	      strlen("error reallocation job tab\n"));
#elif ITA_LANG == 1
	write(2,"Errore reallocazione tab job\n",
	      strlen("Errore reallocazione tab job\n"));
#endif

	exit(EXIT_FAILURE);
      }
    
    memset(&g_jobList[i+1], 0, TAB_JOB_LIST_INCREASE); 

    return i + 1;
  }

}


char *fileNameExtraction(char *cmdLine, int *posNextToken, token_t *tokenType)
{
  /* funzione di utilita' di jobManager */
  char *name = NULL;
  char *token;
  int lenTok;

  token = tokenExtraction(&cmdLine[*posNextToken], posNextToken, 
			  tokenType);
  if ((lenTok = (strlen(token))) > 0) {
#if ENG_LANG == 1
    name = mallocSafe(lenTok +1, "file name string allocation\n");
#elif ITA_LANG == 1
    name = mallocSafe(lenTok +1, "allocazione stringa nome file\n");
#endif
    strcpy(name, token);
  }
  return name;
}


/* jobManager
   funzione principale di parsing ed esecuzione dei comandi
   analizza la stringa cmdLine costruendo la coda dei comandi (nel caso
   ce ne siano piu' d'uno) e chiama le opportune funzioni per l'esecuzione
   Restituisce 0 se parsa il comando exit, altrimenti fornisce 1
   (caso normale)
*/
int jobManager(char *cmdLine)
{
  int job;
  int len, i;
  char *token;
  token_t tokenType;
  int posNextToken;
  int lenTok;
  struct taskRec *task;
  struct taskRec *prevTask = NULL;
  boolean otherCommand, otherTask;
  boolean finish = FALSE;
  char *args[N_MAX_ARG];
  int argCount = 0;

  len = strlen(cmdLine);
  posNextToken = 0;

  do {
    token = tokenExtraction(&cmdLine[posNextToken], &posNextToken, 
			    &tokenType);

    /* se la stringa ritornata da token e' nulla, ritorna */
    if ((strlen(token)) == 0) return 1;

    /* si controlla se il comando non sia il comando di exit
       in tal caso si esce da questa funzione restituendo 0
       che provochera' la terminazione della shell (nella funz. main).
       Notare che prima si testa se il suddetto comando esista (str > 0)
    */
    if ((strlen(INT_CMD_EXIT) > 0) && (0 == (strcmp(INT_CMD_EXIT, token))))
      return 0;

    job = searchTabEntry(); /* prima casella libera */

    g_jobList[job] = mallocSafe(sizeof(jobRec), 
		       "allocazione nuovo elemento di jobList\n");

    g_jobList[job]->task = mallocSafe(sizeof(taskRec), 
		       "allocazione nuovo task in jobList\n");
    g_jobList[job]->task->succ = g_jobList[job]->task->prev = NULL;

    task = prevTask = g_jobList[job]->lastTask = g_jobList[job]->task;


    do {
      otherTask = FALSE;
      if ((lenTok = (strlen(token))) > 0) {

#if ENG_LANG == 1
	task->command = mallocSafe(lenTok +1, "jobManager(): string alloc.\n");
#elif ITA_LANG == 1
	task->command = mallocSafe(lenTok +1, "jobManager():alloc. stringa\n");
#endif

	strcpy(task->command, token);

	/* il comando deve essere copiato nel primo elem. degli arg. */
#if ENG_LANG == 1
	args[0] =  mallocSafe(lenTok +1, "jobManager(): argument alloc.\n");
#elif ITA_LANG == 1
	args[0] =  mallocSafe(lenTok +1, "jobManager(): alloc. arg.\n");
#endif
	strcpy(args[0], token);
	argCount = 1;  

	/* in questa fase non si effettuano i controlli se il comando esite
	   perche' e' un compito che spetta alla funz. che esegue i comandi.
	   Ci si limita a copiare il token
	*/
      }

      finish = FALSE;

      while (!finish && (posNextToken <= len)) {

	while ((tokenType == BLANK_T) && (posNextToken < len)) {
	  /* caricamento degli argomenti */
	  token = tokenExtraction(&cmdLine[posNextToken], &posNextToken, 
				  &tokenType);
	  if (((lenTok = (strlen(token))) > 0) && (argCount < N_MAX_ARG)) {

#if ENG_LANG == 1
	    args[argCount] =  
	      mallocSafe(lenTok +1, "jobManager(): argument allocation\n");
#elif ITA_LANG == 1
	    args[argCount] =  
	      mallocSafe(lenTok +1, "jobManager(): alloc. argomento\n");
#endif

	    strcpy(args[argCount], token);
	    argCount++;  
	  }
	}

	switch (tokenType) {

	case BLANK_T:
	  finish = (posNextToken == len);
	  break;

	case REDIRECT_INPUT_T:
	  task->fileNameIn = fileNameExtraction(cmdLine, &posNextToken, 
						&tokenType);
	  break;

	case REDIRECT_OUTPUT_T:
	  task->fileNameOut = fileNameExtraction(cmdLine, &posNextToken, 
						 &tokenType);
	  task->appendFileOut = FALSE;
	  break;

	case REDIRECT_OUTPUT_APPEND_T:
	  task->fileNameOut = fileNameExtraction(cmdLine, &posNextToken, 
						 &tokenType);
	  task->appendFileOut = TRUE;
	  break;

	case REDIRECT_ERROR_T:
	  task->fileNameErr = fileNameExtraction(cmdLine, &posNextToken, 
						 &tokenType);
	  task->appendFileErr = FALSE;
	  break;

	case REDIRECT_ERROR_APPEND_T:
	  task->fileNameErr = fileNameExtraction(cmdLine, &posNextToken, 
						 &tokenType);
	  task->appendFileErr = TRUE;
	  break;

	case COMMANDS_SEPARATOR_T:
	  finish = TRUE;
	  break;

	case PIPE_T:
	  otherTask = finish = TRUE;
	  break;

	case BACKGROUND_T:
	  g_jobList[job]->background = TRUE;
	  finish = TRUE;
	  break;

	} /* di switch */

      } /* di while finish */
        
      /* copia della tab. momentanea degli argomenti, nella struttura
	 apposita legata al task corrente 
      */
#if ENG_LANG == 1
      task->args = callocSafe(argCount +1, sizeof(char *), 
			      "arguments tab allocation\n");
#elif ITA_LANG == 1
      task->args = callocSafe(argCount +1, sizeof(char *), 
			      "allocazione tab. argomenti\n");
#endif
      for (i = 0; i < argCount; i++) task->args[i] = args[i];
      task->args[argCount] = NULL;
      argCount = 0;

      if ((otherTask) && (posNextToken < len)){
	/* si crea un altro task accodato allo stesso job */
#if ENG_LANG == 1
	task = mallocSafe(sizeof(taskRec), 
			  "new task allocation\n");
#elif ITA_LANG == 1
	task = mallocSafe(sizeof(taskRec), 
			  "allocazione nuovo task\n");
#endif
	g_jobList[job]->lastTask = task;
	prevTask->succ = task;
	task->prev = prevTask;
	task->succ = NULL;
	prevTask = task;
	/* estrazione del token del nuovo comando */
	token = tokenExtraction(&cmdLine[posNextToken], &posNextToken, 
				&tokenType);
      } else {
	otherTask = FALSE;
      }

    } while(otherTask);   /* ciclo do - while */

    taskExec(job);  /* esecutore effettivo del comando */

    /* si libera la mem. allocata solo se non e' un job che esegue
       in backgrond; in tal caso ci pensera' la zombieRecover()
    */
    if (g_jobList[job]->background == FALSE) freeMemJob(job);

    otherCommand = (posNextToken < len);
  } while(otherCommand); /* ciclo do - while */

  return 1;
}   


#if 0
/* ================================================================== */

/* taskExec
   funzione che esegue i comandi
*/
void taskExec(int job)
{
  char bufStr[LEN_MAX_OUTPUT_VARIOUS];
  int fd_pipe[2];
  int status;

  /* fork principale */
  if ((g_jobList[job]->child_pid = fork()) == -1)   
    errorPrintAndExit("fork in taskExec()\n");

  if (g_jobList[job]->child_pid == 0) {
    /* processo figlio  */

    if (g_jobList[job]->task->succ != NULL) {
      /* esistono almeno due task da eseguire in pipe */

      pipeExec(g_jobList[job]->lastTask, fd_pipe);  
         /* in questo caso, fd_pipe e' messo solo perche' richiesto
	    dalla sintassi della funzione pipeExec() 
	    Notare che si passa il lastTask perche' la catena di 
	    chiamate fork() viene eseguita in ordine inverso rispetto
	    a come sono stati memorizzati i comandi
	 */

    } else {
      /*
      execve(g_jobList[job]->task->command, 
	     g_jobList[job]->task->args, NULL);
      */
      execvp(g_jobList[job]->task->command, 
	     g_jobList[job]->task->args);

      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

    }

  } else {
    /* processo padre principale */

    if (g_jobList[job]->background == FALSE) {

      #ifdef _debug_print
      printf("Padre principale: attendo il termine del  figlio %d \n",
	     g_jobList[job]->child_pid);
      #endif

      if (waitpid(g_jobList[job]->child_pid, &status, 0) == -1)
      
#if ENG_LANG == 1
	errorPrintAndExit("in wait first parent\n");
#elif ITA_LANG == 1
	errorPrintAndExit("in wait padre principale\n");
#endif
      
      #ifdef _debug_print
      printf("terminato figlio con pid %d  con status %d\n",
	     g_jobList[job]->child_pid, status);
      #endif

    } else {
      /* e' comando da eseguire in background */
      sprintf(bufStr, "[child pid is %d]\n", g_jobList[job]->child_pid);
      write(1, bufStr, strlen(bufStr));
    }

  } 
}


/* pipeExec
   funzione esecutrice di pipe
   da chiamare quando ci sono almeno due task da eseguire in pipe
*/
void pipeExec(struct taskRec *task, int fd_pipe_prec[2]) 
{
  int ris;
  int fd_pipe[2];
  int pid;

  /* pipe */
  if (pipe(fd_pipe) == 1)
#if ENG_LANG == 1
    errorPrintAndExit("pipe creation\n");
#elif ITA_LANG == 1
    errorPrintAndExit("creazione pipe\n");
#endif

  /* fork */
    if ((pid = fork()) == -1)
    errorPrintAndExit("fork in pipeExec()\n");

  if (pid == 0) {
    /* processo figlio  */

    if (task->prev != NULL) {
      /* esistono altri comandi (pipe) precedenti ) */

      pipeExec(task->prev, fd_pipe);   /* chiamata ricorsiva */

    } else {
      /* e' il primo comando da mettere in pipe */
      close(fd_pipe[0]);
      ris = dup2(fd_pipe[1], 1);
      /* chiude stdout e duplica il lato output della pipe */

      /*
      execve(task->command, task->args, NULL);
      */
      execvp(task->command, task->args);

      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

    }

  } else {
    /* processo padre */

    close(fd_pipe[1]);
    close(0);
    dup(fd_pipe[0]); 
    /* chiude stdin e duplica il lato input della pipe */

    if (task->succ != NULL) {
      /* si tratta di un comando intermedio tra due pipe e quindi a sua
	 volta deve mandare in output il risultato 
      */
      close(fd_pipe_prec[0]);
      close(1);
      dup(fd_pipe_prec[1]); 
      /* chiude stdout e duplica il lato output della pipe */
    }

    /*
    execve(task->command, task->args, NULL);
    */
    execvp(task->command, task->args);

      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

  }
}


#endif
/* ================================================================== */



/* versione 2 */
#if 0
/* ================================================================== */


/* taskExec
   funzione effettiva di esecuzione comandi
*/
void taskExec(int job)
{
  char bufStr[LEN_MAX_OUTPUT_VARIOUS];
  int fd_pipe[2];
  int pid_succ, status;

  #ifdef _debug_pipe_exec
  char str[512];
  #endif

  /* fork principale */
  if ((g_jobList[job]->child_pid = fork()) == -1)   
    errorPrintAndExit("fork in taskExec()\n");

  if (g_jobList[job]->child_pid == 0) {
    /* processo figlio  */

    if (g_jobList[job]->task->succ != NULL) {
      /* esistono almeno due task da eseguire in pipe */

      pipeExec(g_jobList[job]->lastTask, &pid_succ, fd_pipe);  
         /* in questo caso, fd_pipe e' messo solo perche' richiesto
	    dalla sintassi della funzione pipeExec() 
	    Notare che si passa il lastTask perche' la catena di 
	    chiamate fork() viene eseguita in ordine inverso rispetto
	    a come sono stati memorizzati i comandi
	 */

      /* le seguenti istruzioni, normalmente non verrano mai
	 eseguite perche' se la execve funziona normalmente, terminera' 
	 il figlio, se invece verra' catturato un errore, 
	 verra' eseguita una exit.
	 Tuttavia le lascio ugualmente, perche' mi erano servite in fase
	 di messa a punto.
      */
        #ifdef _debug_pipe_exec
        sprintf(str, "  figlio piu' esterno , attendo termine figlio %d\n",
	        pid_succ);
        write(1, str, strlen(str));
        #endif

        if (waitpid( pid_succ, &status, 0) == -1)
	  errorPrintAndExit("in waitpid() figlio piu' esterno \n");
        exit(0);      


    } else {
      /*
      execve(g_jobList[job]->task->command, 
	     g_jobList[job]->task->args, NULL);
      */
      execvp(g_jobList[job]->task->command, 
	     g_jobList[job]->task->args);

      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

    }

  } else {
    /* processo padre principale */

    if (g_jobList[job]->background == FALSE) {

      #ifdef _debug_print
      printf("Padre principale: attendo il termine del  figlio %d \n",
	     g_jobList[job]->child_pid);
      #endif

      if (waitpid(g_jobList[job]->child_pid, &status, 0) == -1)
      
#if ENG_LANG == 1
	errorPrintAndExit("in wait first parent\n");
#elif ITA_LANG == 1
	errorPrintAndExit("in wait padre principale\n");
#endif
      
      #ifdef _debug_print
      printf("terminato figlio con pid %d  con status %d\n",
	     g_jobList[job]->child_pid, status);
      #endif

    } else {
      /* e' comando da eseguire in background */
      sprintf(bufStr, "[child pid is %d]\n", g_jobList[job]->child_pid);
      write(1, bufStr, strlen(bufStr));
    }

  } 
}



/* pipeExec
   funzione esecutrice di pipe
   da chiamare quando ci sono almeno due task da eseguire in pipe
*/
void pipeExec(struct taskRec *task, int *pid, int fd_pipe_prec[2]) 
{
  int fd_pipe[2];
  int pid_succ;
  struct taskRec *child_task;

  #ifdef _debug_pipe_exec
  char str[512];
  #endif

  /* pipe */
  if (pipe(fd_pipe) == 1)
#if ENG_LANG == 1
    errorPrintAndExit("pipe creation\n");
#elif ITA_LANG == 1
    errorPrintAndExit("creazione pipe\n");
#endif

  /* fork */
    if ((*pid = fork()) == -1)
    errorPrintAndExit("fork in pipeExec()\n");

  if (*pid == 0) {
    /* processo figlio  */

    if ((task->prev != NULL) && (task->prev->prev != NULL)) {
      /* esistono altri comandi da mettere in pipe */

      pipeExec(task->prev, &pid_succ, fd_pipe);   /* chiamata ricorsiva */

      #ifdef _debug_pipe_exec
      write(1, "+++ esecuzione della exit", 
	    strlen("+++ esecuzione della exit"));
      #endif;

      exit(0); /* di questo figlio */

    } else {
      /* e' il primo comando da mettere in pipe */

      child_task = task->prev;

      #ifdef _debug_pipe_exec
      /* importante: tutti i messaggi vanno messi PRIMA della ridirezione 
	 dello stdout */
      write(1,"\n->     Exec del figlio ultimo\n",
	    strlen("\n->     Exec del figlio ultimo\n"));
      write(1,"command f = ", strlen("command f = "));
      write(1, child_task->command, strlen(child_task->command));
      write(1,"\n", strlen("\n"));	 
      #endif

      close(fd_pipe[0]);

      ris = dup2(fd_pipe[1], 1);
      /*
      close(1);
      dup(fd_pipe[1]); 
      */
      /* chiude stdout e duplica il lato output della pipe */

      /*
      execve(child_task->command, child_task->args, NULL);
      */
      execvp(child_task->command, child_task->args);

      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

    }

  } else {
    /* processo padre */

    #ifdef _debug_pipe_exec
    sprintf(str, "generato figlio INTERNO con pid %d\n", *pid); 
    write(1, str, strlen(str));
    #endif

    close(fd_pipe[1]);
    close(0);
    dup(fd_pipe[0]); 
    /* chiude stdin e duplica il lato input della pipe */

    if (task->succ != NULL) {
      /* si tratta di un comando intermedio tra due pipe e quindi a sua
	 volta deve mandare in output il risultato 
      */
      close(fd_pipe_prec[0]);
      close(1);
      dup(fd_pipe_prec[1]); 
      /* chiude stdout e duplica il lato output della pipe */
    }


    #ifdef _debug_pipe_exec
    write(1,"\n->     Exec del padre\n",
	  strlen("\n->     Exec del padre\n"));
    write(1,"command = ", strlen("command = "));
    write(1, task->command, strlen(task->command));
    write(1,"\n", strlen("\n"));	 
    #endif

    /*
    execve(task->command, task->args, NULL);
    */
    execvp(task->command, task->args);
    
      /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif

  }
}

#endif
/* ================================================================== */




/* versione 3 */


/* taskExec
   funzione effetiva di esecuzione comandi
*/
void taskExec(int job)
{
  char bufStr[LEN_MAX_OUTPUT_VARIOUS];
  int fd_pipe[2];
  int status;

  #ifdef _debug_pipe_exec
  char str[512];
  #endif

  /* fork principale */
  if ((g_jobList[job]->child_pid = fork()) == -1)   
    errorPrintAndExit("fork in taskExec()\n");

  if (g_jobList[job]->child_pid == 0) {
    /* processo figlio  */

    if (g_jobList[job]->task->succ != NULL) {
      /* esistono almeno due task da eseguire in pipe */

      /* pipe */
      if (pipe( g_jobList[job]->lastTask->fd_pipe) == 1)
#if ENG_LANG == 1
	errorPrintAndExit("pipe creation into FIRST CHILD\n");
#elif ITA_LANG == 1
      errorPrintAndExit("creazione pipe nel primo figlio\n");
#endif


      #ifdef _debug_pipe_exec
      sprintf(str, "pipe del figlio %p\n", fd_pipe);
      write(1, str, strlen(str));	 
      #endif


      close( g_jobList[job]->lastTask->fd_pipe[1]); 
      close(0);
      dup( g_jobList[job]->lastTask->fd_pipe[0]); 
      /* chiude stdin e duplica il lato input della pipe */

      /* chiama la funzione ausiliaria di esecuzione comandi n cascta */
      pipeExec(g_jobList[job]->lastTask->prev, fd_pipe);  

    }

    #ifdef _debug_pipe_exec
    sprintf(str, "\n->     Exec PRIMO FIGLIO   command = %s\n\n",
	    g_jobList[job]->lastTask->command);
    write(1, str, strlen(str));	 
    #endif

    /*
      execve(g_jobList[job]->lastTask->command, 
	     g_jobList[job]->lastTask->args, NULL);
    */
    execvp(g_jobList[job]->lastTask->command, 
	   g_jobList[job]->lastTask->args);

    /* se la exec non ha successo si esegue: */
#if ENG_LANG == 1
      errorPrintAndExit("error into exec callin first child\n");
#elif ITA_LANG == 1
      errorPrintAndExit("Errore nella exec in primo figlio\n");
#endif


  } else {
    /* processo padre principale */

    if (g_jobList[job]->background == FALSE) {

      #ifdef _debug_print
      printf("Padre principale: attendo il termine del  figlio %d \n",
	     g_jobList[job]->child_pid);
      #endif

      if (waitpid(g_jobList[job]->child_pid, &status, 0) == -1)
      
#if ENG_LANG == 1
	errorPrintAndExit("in wait first parent\n");
#elif ITA_LANG == 1
	errorPrintAndExit("in wait padre principale\n");
#endif
      
      #ifdef _debug_print
      printf("terminato figlio con pid %d  con status %d\n",
	     g_jobList[job]->child_pid, status);
      #endif

    } else {
      /* e' comando da eseguire in background */
      sprintf(bufStr, "[child pid is %d]\n", g_jobList[job]->child_pid);
      write(1, bufStr, strlen(bufStr));
    }

  } 
}




/* pipeExec
   funzione esecutrice di pipe
*/
void pipeExec(struct taskRec *task, int fd_pipe_prev[2]) 
{
  int pid, status;
  int fd_pipe[2];

  #ifdef _debug_pipe_exec
  char str[512];
  #endif

  /* fork */
    if ((pid = fork()) == -1)
    errorPrintAndExit("fork in pipeExec()\n");

  if (pid == 0) {
    /* processo figlio  */



      #ifdef _debug_pipe_exec
      sprintf(str, "pipe nel figlio succ. %p\n", fd_pipe_prev);
      write(1, str, strlen(str));	 
      #endif




    #ifdef _debug_pipe_exec
    /* importante: mettere queste istruzione PRIMA della ridirezione stdout */
    sprintf(str, "\n->     Exec FIGLIO INTERNO command = %s\n\n",
	    task->command);
    write(1, str, strlen(str));	 
    #endif

    /*    close(task->succ->fd_pipe[0]); */
    
    close(1);
    dup(task->succ->fd_pipe[1]); 

    /* chiude stdout e duplica il lato output della pipe */

    if (task->prev != NULL) {
      /* esistono altri comandi da mettere in pipe */

      /* pipe */
      if (pipe(fd_pipe) == 1)
#if ENG_LANG == 1
	errorPrintAndExit("pipe creation\n");
#elif ITA_LANG == 1
      errorPrintAndExit("creazione pipe\n");
#endif

      close(fd_pipe[1]);
      close(0);
      dup(fd_pipe[0]); 
      /* chiude stdin e duplica il lato input della pipe */

      pipeExec(task->prev, fd_pipe);              /* chiamata ricorsiva */

    }

    /*
    execve(task->command, task->args, NULL);
    */
    execvp(task->command, task->args);

    /* se la exec non ha successo si prosegue: */
#if ENG_LANG == 1
    errorPrintAndExit("error into exec call\n");
#elif ITA_LANG == 1
    errorPrintAndExit("Errore nella esecuzione della exec\n");
#endif
 

  } else {
    /* processo padre */

    /* in sostanza deve solo attendere il figlio */

    #ifdef _debug_pipe_exec
    printf("Padre INTERNO:    attendo il termine del  figlio %d \n", pid);
    #endif

    if (waitpid(pid, &status, 0) == -1)
#if ENG_LANG == 1
      errorPrintAndExit("in wait first INTERNAL parent\n");
#elif ITA_LANG == 1
      errorPrintAndExit("in wait padre INTERNO\n");
#endif
      
 
    #ifdef _debug_pipe_exec
    printf("terminato figlio con pid %d  con status %d\n", pid, status);
    #endif

  }
}








void freeMemJob(int job) /* libera la mem. dinamica occupata da job */    
{
  struct taskRec *task, *tmp;
  int i;

  if (g_jobList[job] == NULL) return;  /* per sicurezza, ma e' ridondante */

  task = g_jobList[job]->task;

  while (task != NULL) {
    free(task->command);
    for (i = 0; task->args[i] != NULL; i++) free(task->args[i]);
    if (task->args != NULL) free(task->args);
    if (task->fileNameIn != NULL) free(task->fileNameIn);
    if (task->fileNameOut != NULL) free(task->fileNameOut);
    if (task->fileNameErr != NULL) free(task->fileNameErr);

    tmp = task;
    task = task->succ;
    free(tmp);
  }

  if ( g_jobList[job]->strCommand != NULL) free(g_jobList[job]->strCommand);
  free(g_jobList[job]);
  g_jobList[job] = NULL;
}



/* zombieRecover
   Verifica se nella tabella dei job ci sono processi che hanno finito
   in tal caso stampa un messaggio e deaolloca i relativi dati
*/
void zombieRecover(void)
{
  int i;
  char bufStr[LEN_MAX_OUTPUT_VARIOUS];
  int pid, status;

  for (i = 0; i < g_dimTabJob; i++)
    if ((g_jobList[i] != NULL) && g_jobList[i]->background) {

	/* wait del padre principale */
	if ((pid = 
	     waitpid(g_jobList[i]->child_pid, &status, WNOHANG)) == -1) {

	  /* errore nella wait */

#if ENG_LANG == 1
	  errorPrintAndExit("in wait of zombieRecover()\n");
#elif ITA_LANG == 1
	  errorPrintAndExit("in wait di zombieRecover()\n");
#endif

	}

	if (g_jobList[i]->child_pid == pid){
	  /* e' terminato un figlio zombie */
	  sprintf(bufStr, "[child %d terminated with status %d]\n", 
		  g_jobList[i]->child_pid, status);
	  write(2, bufStr, strlen(bufStr));

	  freeMemJob(i); /* si libera l'occupazione dati del job */
	}
      }
  
}
