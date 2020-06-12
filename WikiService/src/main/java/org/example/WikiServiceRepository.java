package org.example;

import java.util.ArrayList;
import java.util.List;

public class WikiServiceRepository {
    List<WikiData> Datas;

    public WikiServiceRepository()
    {
        Datas= new ArrayList<>();
        WikiData w1= new WikiData();
        w1.setLabel("Barack Obama");
        w1.setIdentifier("Q76");
        w1.setDescription("President of US");
        WikiData w2= new WikiData();
        w2.setLabel("Barack");
        w2.setIdentifier("Q76");
        w2.setDescription("President of US");
        System.out.println("Getting Description...");
        Datas.add(w1);
        Datas.add(w2);
    }
    public List<WikiData> getAllWikiData()
    {
        return Datas;
    }
    public WikiData getData(String id)
    {

        for(WikiData w: Datas)
        {
            if(w.getLabel()==id){
                return w;
            }
        }
        return new WikiData();
    }

    public void createData(WikiData wikidata) {
        Datas.add(wikidata);
        System.out.println(wikidata.getLabel());
    }
}
