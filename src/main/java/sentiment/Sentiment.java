package sentiment;

import java.util.Properties;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentClass;
import edu.stanford.nlp.util.CoreMap;

public class Sentiment {
	static private final Logger LOGGER;
	static {
        LOGGER = LoggerFactory.getLogger(Sentiment.class);
    }
	
	Node node;
	StanfordCoreNLP pipeline;
	
	private Sentiment(Node node) {
		this.node = node;
	}
	
	public static void start(Node parent) {
		final Sentiment sentiment = new Sentiment(parent);
		sentiment.init();
	}
	
	private void init() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, parse, sentiment");
		pipeline = new StanfordCoreNLP(props);
		
		Action act = new Action(Permission.READ, new SentenceHandler());
		act.addParameter(new Parameter("Sentence", ValueType.STRING));
		act.addResult(new Parameter("Sentiment", ValueType.STRING));
		node.createChild("analyze sentence").setAction(act).build().setSerializable(false);
	}
	
	private class SentenceHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			String text = event.getParameter("Sentence", ValueType.STRING).getString();
			Annotation document = new Annotation(text);
			
			pipeline.annotate(document);
			
			CoreMap sentence =  document.get(SentencesAnnotation.class).get(0);
			//Tree sat = sentence.get(SentimentAnnotatedTree.class);
			String answer = sentence.get(SentimentClass.class);
			LOGGER.debug("Analyzed sentence as " + answer + ": \"" + text + "\"");
			
			Table table = event.getTable();
			table.addRow(Row.make(new Value(answer)));
			
		}
	}

}
