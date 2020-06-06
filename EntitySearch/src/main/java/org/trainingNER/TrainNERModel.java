package org.trainingNER;

import java.util.Properties;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.StringUtils;

public class TrainNERModel {

	public static void trainAndWrite(String modelOutPath, String prop, String trainingFilepath) {
		Properties props = StringUtils.propFileToProperties(prop);
		props.setProperty("serializeTo", modelOutPath);

		// if input use that, else use from properties file.
		if (trainingFilepath != null) {
			props.setProperty("trainFile", trainingFilepath);
		}

		SeqClassifierFlags flags = new SeqClassifierFlags(props);
		CRFClassifier<CoreLabel> crf = new CRFClassifier<>(flags);
		crf.train();

		crf.serializeClassifier(modelOutPath);
	}

	public static void main(String[] args) {
		TrainNERModel.trainAndWrite(
				"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/ner-model.ser.gz",
				"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/propertiesfile.properties",
				"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/training.rules");
		System.out.println("model trained..");
	}
}
