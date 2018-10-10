package com.sylweb.listedecourses;

import android.content.Intent;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by sylvain on 10/10/2018.
 */

public class Article {

    public String id;
    public String name;
    public int quantity;
    public int deleted;
    public long modified_on;

    public Article(HashMap data) {
        this.id = (String)data.get("id");
        this.name = (String)data.get("name");
        this.quantity = Integer.valueOf((String)data.get("quantity"));
        this.deleted = Integer.valueOf((String)data.get("deleted"));
        this.modified_on = Long.valueOf((String)data.get("modified_on"));
    }

    public Article(JSONObject data) {
        try {
            this.id = String.valueOf(data.get("id"));
            this.name = String.valueOf(data.get("name"));
            this.quantity = Integer.valueOf(String.valueOf(data.get("quantity")));
            this.deleted = Integer.valueOf(String.valueOf(data.get("deleted")));
            this.modified_on = Long.valueOf(String.valueOf(data.get("modified_on")));
        }catch(Exception ex) {

        }
    }

    public  JSONObject getAsJSON() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", this.id);
            data.put("name", this.name);
            data.put("quantity", this.quantity);
            data.put("deleted", this.deleted);
            data.put("modified_on", this.modified_on);
            return data;
        }
        catch (Exception ex) {
            return null;
        }

    }

}
