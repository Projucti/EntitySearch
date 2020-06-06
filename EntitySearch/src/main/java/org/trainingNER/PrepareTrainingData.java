package org.trainingNER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PrepareTrainingData {

	public static void main(String[] args) {

		try {
			File myObj = new File(
					"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/TrainigDataSet_GERMAN.rtf");
			Scanner myReader = new Scanner(myObj);
			try {
				FileWriter fw;
				fw = new FileWriter(
						"/Users/amansharma/git/EntitySearch/EntitySearch/EntitySearch/resources/OutTrainigDataSet_GERMAN.txt");
				while (myReader.hasNextLine()) {
					String data = myReader.nextLine();
					/*
					 * if (data.contains("B-MISC")) data = data.replaceAll("B-MISC", "MISC"); if
					 * (data.contains("I-MISC")) data = data.replaceAll("I-MISC", "MISC"); if
					 * (data.contains("B-PER")) data = data.replaceAll("B-PER", "PERSON"); if
					 * (data.contains("I-PER")) data = data.replaceAll("I-PER", "PERSON"); if
					 * (data.contains("B-LOC")) data = data.replaceAll("B-LOC", "LOCATION"); if
					 * (data.contains("I-LOC")) data = data.replaceAll("I-LOC", "LOCATION"); if
					 * (data.contains("B-ORG")) data = data.replaceAll("B-ORG", "ORGANISATION"); if
					 * (data.contains("I-ORG")) data = data.replaceAll("I-ORG", "ORGANISATION");
					 */
					String[] columns = data.split(" ");
					if (!columns[columns.length - 1].isEmpty() && !columns[0].contains("\\")
							&& !columns[columns.length - 1].startsWith("O")) {
						// System.out.println(columns[0] + "\t" + columns[columns.length -
						// 1].replace("\\", ""));

						fw.append(columns[0] + "\t" + columns[columns.length - 1].replace("\\", ""));
						fw.append("\n");
					}
				}

				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		System.out.println("file prepared");
	}

}
