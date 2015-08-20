package edu.ucsc.fluffy;

import java.io.*;
import java.util.ArrayList;
import android.util.Pair;

public class Patient implements java.io.Serializable {


    Integer ID;
    Integer age;
    Float BMI;
    Float VAS;
    String race;

    // array of time stamps vs pain levels
    ArrayList<Pair<Long,Float>> pain;

    // array of time stamps vs procedure steps
    ArrayList<Pair<Long,String>> steps;


    Patient(Integer name) {
        ID = name;
        pain = new ArrayList<Pair<Long,Float>>();
        steps = new ArrayList<Pair<Long,String>>();
    }

    void setPain(Long l, Float f) {
        pain.add(Pair.create(l,f));
    }

    void setStep(Long l, String s) {
        steps.add(Pair.create(l,s));
    }

    void serialize(String s) {
        try
        {
            FileOutputStream fileOut = new FileOutputStream(s);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            //System.out.printf("Serialized data is saved in " + s);
        }catch(IOException i)
        {
            i.printStackTrace();
        }
    }

    static Patient deserialize(String s) {
        Patient p = null;
        try
        {
            FileInputStream fileIn = new FileInputStream(s);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            p = (Patient) in.readObject();
            in.close();
            fileIn.close();
            return p;
        }catch(IOException i)
        {
            i.printStackTrace();
            return null;
        }catch(ClassNotFoundException c)
        {
            //System.out.println("Patient class not found");
            c.printStackTrace();
            return null;
        }
    }

}

