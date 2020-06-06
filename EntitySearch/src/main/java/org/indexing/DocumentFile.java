package org.indexing;

public class DocumentFile {

	private String content;
	private int docId;
	private int docScore; 

	public int getDocScore() {
		return docScore;
	}

	public void setDocScore(int docScore) {
		this.docScore = docScore;
	}

	public DocumentFile(String text) {
		this.content = text;
	}

	public String getText() {
		return content;
	}

	public void setText(String text) {
		this.content = text;
	}
	public int getDocId() {
		return docId;
	}
	
	public void setDocId(int docId) {
		this.docId = docId;
	}

}