package edu.ucsc.fluffy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Patient implements java.io.Serializable {


    Integer ID;
    Integer age;
    Float BMI;
    Float VAS;
    String race;

    // array of time stamps vs pain levels
    ArrayList<Pair<Long, Float>> pain;

    // array of time stamps vs procedure steps
    ArrayList<Pair<Long, String>> steps;


    Patient(Integer name) {
        ID = name;
        pain = new ArrayList<Pair<Long, Float>>();
        steps = new ArrayList<Pair<Long, String>>();
    }

    void setPain(Long l, Float f) {
        pain.add(Pair.create(l, f));
    }

    void setStep(Long l, String s) {
        steps.add(Pair.create(l, s));
    }

    String getSteps() {
        String str = "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for (Pair<Long, String> step : steps) {
            Date resultdate = new Date(step.getFirst());
            str += sdf.format(resultdate) + " " + step.getSecond() + "\n";
        }
        return (str);
    }

    void serialize(String s) {
        try {
            FileOutputStream fileOut = new FileOutputStream(s);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            //System.out.printf("Serialized data is saved in " + s);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    static Patient deserialize(String s, int id) {
        Patient p = null;
        try {
            FileInputStream fileIn = new FileInputStream(s);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            p = (Patient) in.readObject();
            in.close();
            fileIn.close();
            return p;
        } catch (InvalidClassException e) {
            File f = new File(s);
            f.delete();
            return null; // they should create a new patient but delete the old file first
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            //System.out.println("Patient class not found");
            c.printStackTrace();
            return null;
        }
    }
}

