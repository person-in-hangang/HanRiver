package org.med.darknetandroid;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;


@IgnoreExtraProperties
public class FirebasePost {


    public int person;
    public boolean fall;
    public byte[] photo;
    public String time;
        public FirebasePost(){
            // Default constructor required for calls to DataSnapshot.getValue(FirebasePost.class)
        }

        public FirebasePost(int person,boolean fall, byte[] photo, String time) {
            this.person = person;
            this.fall = true;
            this.photo =photo;
            this.time ="none";
        }

        @Exclude
        public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("person", person);
            result.put("fall", true);
            result.put("photo", photo);
            result.put("time", time);
            return result;
        }
    }


