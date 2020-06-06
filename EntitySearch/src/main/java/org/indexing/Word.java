package org.indexing;

import java.util.List;

public class Word {
	
	private String word;
	private List<Occurrence> occurence;
	private int frequency;
	
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public List<Occurrence> getOccurence() {
		return occurence;
	}
	public void setOccurence(List<Occurrence> occurence) {
		this.occurence = occurence;
	}
	public int getFrequency() {
		return frequency;
	}
	public void setFrequency(int frequency) {
		this.frequency =this.occurence.size() ;
	}
	

}