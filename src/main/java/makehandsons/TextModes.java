package makehandsons;

/** 
 * The global boolean state flags from the main program.
 * Made a separate class mainly for ease of testing.
 * @author Ian Darwin
 */
class TextModes {
	boolean inCutMode = false;
	boolean inCommentMode = false;
	boolean fileChanged = false;
	boolean inExchangeMode = false;
	boolean inCppMode = false;
}