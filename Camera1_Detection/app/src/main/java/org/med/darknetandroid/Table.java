package org.med.darknetandroid;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Table {


    public String time;



    public Table() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Table(int person) {
        /*
        this.person = person;
        this.fall = true;

        this.time ="none";

         */
    }

    public Table(String formatDate) {


        this.time =formatDate;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();


        return result;
    }
    // [END post_to_map]

}