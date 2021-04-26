package com.example.notice;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;


@IgnoreExtraProperties
public class FirebasePost {


    public String time;

        public FirebasePost(){
            // Default constructor required for calls to DataSnapshot.getValue(FirebasePost.class)
        }

        public FirebasePost(int person, boolean fall, String photo, String time) {

            this.time ="none";
        }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("time", time);
        return result;
    }

    }


