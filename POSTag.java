package cs481.postag;

import cs481.token.*;
import cs481.util.*;

import java.io.*;
import java.util.*;

/**
 * Determines the part of speech tags based on Viterbi.
 *
 * <pre>
 * Typical use:
 * POSTag pt = new POSTag();
 * pt.train(training);
 * pt.tag(testing);
 * </pre>
 *
 * Run from the commandline.
 *
 * @author Sterling Stuart Stein
 * @author Shlomo Argamon
 */
public class POSTag
{
    /**
     * Special start tag
     */
    public static String StartTag = "*START*";
    /**
     * Special start tag
     */
    public static String SprevTag = "*Previous*";
    
    /**
     * Small probability for when not found
     */
    public static float epsilon = -10000000f;
    
    /**
     * Array of all tags
     */
    protected String[] tags;
    
    /**
     * Probability of tags given specific words
     */
    protected HashMap pTagWord;
    /**
     * Probability of tags tag pair : transition probability
     */
    protected HashMap pTagTag;
    /**
     * Probability of individual tags (i.e., P(tag)
     */
    protected HashMap pTag;	
    
    /**
     * Hashmap of all known words
     */
    protected HashMap allWords;;	
    
    /**
     * Make an untrained part of speech tagger.
     */
    public POSTag()
    {
	pTagWord    = new HashMap();
	pTagTag    = new HashMap();
	pTag        = new HashMap();
	allWords    = new HashMap();
    }
    
    /**
     * Remove all training information.
     */
    public void clear()
    {
	pTag.clear();
	pTagWord.clear();
	pTagTag.clear();
	allWords.clear();
	tags = null;
    }
    
    /**
     * Increment the count in a HashMap for t.
     *
     * @param h1 The HashMap to be modified
     * @param t  The key of the field to increment
     */
    protected void inc1(HashMap h1, String t)
    {
	if(h1.containsKey(t))
	    {
		int[] ip = (int[])h1.get(t);  //Used as int *
		ip[0]++;
	    }
	else
	    {
		int[] ip = new int[1];
		ip[0] = 1;
		h1.put(t, ip);
	    }
    }
    
    /**
     * Increment the count in a HashMap for [t1,t2].
     *
     * @param h2 The HashMap to be modified
     * @param t1 The 1st part of the key of the field to increment
     * @param t2 The 2nd part of the key of the field to increment
     */
    protected void inc2(HashMap h2, String t1, String t2)
    {
	//Have to use Vector because arrays aren't hashable
	Vector key = new Vector(2);
	key.setSize(2);
	key.set(0, t1);
	key.set(1, t2);
	
	if(h2.containsKey(key)) {
		int[] ip = (int[])h2.get(key);  //Used as int *
		ip[0]++;
	} else {
		int[] ip = new int[1];
		ip[0] = 1;
		h2.put(key, ip);
	    }
    }
    protected void inc3(HashMap h3, String tag1, String tag2)
    {
	//Have to use Vector because arrays aren't hashable
	Vector key = new Vector(2);
	key.setSize(2);
	key.set(0, tag1);
	key.set(1, tag2);
	
	if(h3.containsKey(key)) {
		int[] ip = (int[])h3.get(key);  //Used as int *
		ip[0]++;
	} else {
		int[] ip = new int[1];
		ip[0] = 1;
		h3.put(key, ip);
	    }
    }
    /**
     * Train the part of speech tagger.
     *
     * @param training A vector of paragraphs which have tokens with the attribute &quot;pos&quot;.
     */
    public void train(Vector training)
    {
	int cTokens = 0;
	HashMap cWord    = new HashMap();
	HashMap cTag     = new HashMap();
	HashMap cTagWord = new HashMap();
	HashMap cTagTag = new HashMap();
	boolean[] bTrue = new boolean[1];
	bTrue[0] = true;
	
	clear();
	String previoustag;
	//Count word and tag occurrences
	for(Iterator i = training.iterator(); i.hasNext();) {
		Vector para = (Vector)i.next();
		
		for(Iterator j = para.iterator(); j.hasNext();) {
			Vector sent    = (Vector)j.next();
			String curtag  = StartTag;
			inc1(cTag, curtag);
			
			for(Iterator k = sent.iterator(); k.hasNext(); ) {
				
				Token tok = (Token)k.next();
				//if (curtag == StartTag){
				//	previoustag = SprevTag;
				//	inc1(cTag, previoustag);
				//}
				//else{
				previoustag = curtag;
				//}
				
				curtag = (String)tok.getAttrib("pos");
				inc1(cTag, curtag);
				String name = tok.getName().toLowerCase();
				inc1(cWord, name);
				allWords.put(name, bTrue);
				inc2(cTagWord, curtag, name);
				cTokens++;
				inc3(cTagTag, previoustag,curtag);
			    }
		    }
	    }
	
	//Find probabilities from counts
	for(Iterator i = cTag.keySet().iterator(); i.hasNext();) {
	    String key   = (String)i.next();
	    int[]  count = (int[])cTag.get(key);
	    pTag.put(key, new Float(Math.log(((float)count[0]) / (float)cTokens)));
	}
	
	for(Iterator i = cTagWord.keySet().iterator(); i.hasNext();) {
	    Vector key   = (Vector)i.next();
	    int[]  count = (int[])cTagWord.get(key);
	    int[]  total = (int[])cWord.get(key.get(1));

	    pTagWord.put(key, new Float(Math.log(((float)count[0]) / ((float)total[0]))));
	}
	//Make list of all possible tags
	tags = (String[])cTag.keySet().toArray(new String[0]);
	
	for(Iterator i = cTagTag.keySet().iterator(); i.hasNext();) {
	    Vector key   = (Vector)i.next();
	    int[]  count = (int[])cTagTag.get(key);
	    int[]  total = (int[])cTag.get(key.get(0));

	    pTagTag.put(key, new Float(Math.log((((float)count[0])+1) / (((float)total[0])+tags.length))));
	}
    }
    
    /**
     * Print out a HashMap<Vector,int[1]>.
     *
     * @param h The HashMap to be printed.
     */
    protected void debugPrintHashInt(HashMap h) {
	for(Iterator i = h.keySet().iterator(); i.hasNext();) {
	    Vector key = (Vector)i.next();
	    int[]  ip  = (int[])h.get(key);
	    
	    for(int j = 0; j < key.size(); j++) {
		System.out.print(", " + key.get(j));
	    }
	    
	    System.out.println(": " + ip[0]);
	}
    }
    
    /**
     * Print out a HashMap<Vector,Float>.
     *
     * @param h The HashMap to be printed.
     */
    protected void debugPrintHashFloat(HashMap h) {
	for(Iterator i = h.keySet().iterator(); i.hasNext();) {
		Vector key = (Vector)i.next();
		float  f   = ((Float)h.get(key)).floatValue();
		
		for(int j = 0; j < key.size(); j++) {
			System.out.print(", " + key.get(j));
		    }
		
		System.out.println(": " + f);
	    }
    }

    protected void debugPrintHashKeys(HashMap h) {
	for(Iterator i = h.keySet().iterator(); i.hasNext();) {
	    String key = ((String)i.next());
	    System.out.println(": " + key);
	}
    }
    
    
    /**
     * Tags a sentence by setting the &quot;pos&quot; attribute in the Tokens.
     *
     * @param sent The sentence to be tagged.
     */
    public void tagSentence(Vector sent) {
	int len     = sent.size();
	if (len == 0) {
	    return;
	}
	
	int numtags = tags.length;
	System.out.println(numtags);
	
	float smoothing = (float) Math.log(1/(float)numtags);
	
	System.out.println(smoothing);
	
	Vector twkey = new Vector(2);
	twkey.setSize(2);
	
	Vector ttkey = new Vector(2);
	ttkey.setSize(2);
	
	//Probability of best path to word with tag
	float[][] pathprob = new float[len + 1][numtags]; 
	
	//  Edge to best path to word with tag
	int[][]   backedge = new int[len + 1][numtags];
	
	String previoustag1;
	int   back = 0;
	//For words in sentence
	for(int i = 0; i < pathprob.length - 1; i++) {
	    String word = ((Token)sent.get(i)).getName().toLowerCase();
	    twkey.set(1, word);

	    //Loop over tags for this word
	    for(int j = 0; j < numtags; j++) {
	    	if (j == 0){
	    		previoustag1 = StartTag;
	    	}
	    	else{
	    		previoustag1 = tags[back];
	    	}
	    ttkey.set(0,previoustag1);
		
	    String thistag = tags[j];
		Float tagProb1 = (Float)pTag.get(thistag);
		float tagProb = (tagProb1 == null) ? epsilon : tagProb1.floatValue();
		twkey.set(0, thistag);
		
		ttkey.set(1,thistag);
		
		boolean[] knownWord = (boolean[])allWords.get(word);
		Float twp1 = (Float)pTagWord.get(twkey);
		float twp  = (((knownWord == null)||(knownWord[0] != true)) ?
			      tagProb : 
			      ((twp1 == null) ?
			       epsilon :
			       twp1.floatValue()));
		
		Float tagtagProb1 = (Float)pTagTag.get(ttkey);
		float tagtagProb = (tagtagProb1 == null) ? smoothing : tagProb1.floatValue();
		
		
		// In a unigram model, only the current probability matters
		pathprob[i][j]    = twp + tagtagProb;

		// Now create the back link to the max prob tag at the previous stage
		// If we are at the second word or further
		if (i > 0) {
		    //int   back = 0;
		    float max  = -100000000f;
		
		    //Loop over previous tags
		    for(int k = 0; k < numtags; k++) {
			String prevtag = tags[k];
		    
			// Probability for path->prevtag k + thistag j->word i
			float test = pathprob[i-1][k];
		    
			String prevword = ((Token)sent.get(i-1)).getName().toLowerCase();

			if (test > max) {
			    max     = test;
			    back    = k;
			}
		    }
		    backedge[i][j]    = back;

			Token tok = (Token)sent.get(i-1);
			tok.putAttrib("pos", tags[back]);
			//prevtag = backedge[i][prevtag];
		    
		    
		}
	    }
	}

	//Trace back finding most probable path
	/*{
	    float max    = -100000000f;
	    int   prevtag = 0;
	    
	    //Find final tag
	    for(int i = 0; i < numtags; i++) {
		float test = pathprob[len-1][i];
		
		if(max < test) {
		    max       = test;
		    prevtag    = i;
		}
	    }
	    
	    //Follow back edges to start tag and set tags on words
	    //for(int i = len-1; i >= 0; i--) {
		//Token tok = (Token)sent.get(i);
		//tok.putAttrib("pos", tags[prevtag]);
		//prevtag = backedge[i][prevtag];
	    //}
	}*/
    }
    
    /**
     * Tags a Vector of paragraphs by setting the &quot;pos&quot; attribute in the Tokens.
     *
     * @param testing The paragraphs to be tagged.
     */
    public void tag(Vector testing) {
	for(Iterator i = testing.iterator(); i.hasNext();) {
	    Vector para = (Vector)i.next();
	    
	    for(Iterator j = para.iterator(); j.hasNext();) {
		Vector sent = (Vector)j.next();
		tagSentence(sent);
	    }
	}
    }
    
    /**
     * Train on             the 1st XML file,
     * tag                  the 2nd XML file,
     * write the results in the 3rd XML file.
     *
     * @param argv An array of 3 XML file names.
     */
    public static void main(String[] argv) throws Exception
    {
	if(argv.length != 3) {
	    System.err.println("Wrong number of arguments.");
	    System.err.println(
			       "Format:  java cs481.postag.POSTag <train XML> <test XML> <output XML>");
	    System.err.println(
			       "Example: java cs481.postag.POSTag train.xml untagged.xml nowtagged.xml");
	    System.exit(1);
	}
	
	Vector training = Token.readXML(new BufferedInputStream(
								new FileInputStream(argv[0])));
	System.out.println("Read training file.");
	
	POSTag pt = new POSTag();
	pt.train(training);
	System.out.println("Trained.");
	training = null;  //Done with it, so let garbage collector reclaim
	
	Vector testing = Token.readXML(new BufferedInputStream(
							       new FileInputStream(argv[1])));
	System.out.println("Read testing file.");
	pt.tag(testing);
	System.out.println("Tagged.");
	Token.writeXML(testing,
		       new BufferedOutputStream(new FileOutputStream(argv[2])));
    }
}
