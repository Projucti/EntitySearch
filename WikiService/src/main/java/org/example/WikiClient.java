package org.example;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;



public class WikiClient {

    public static void main(String[] args){
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8085/WikiDataService_war/webapi/myresource/fetchDocx?q=barack");
        System.out.println(target.request(MediaType.APPLICATION_JSON).get(String.class));
    }


}
