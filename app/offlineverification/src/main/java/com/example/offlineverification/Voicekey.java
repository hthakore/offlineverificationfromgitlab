package com.example.offlineverification;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.SpeakerModel;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class Voicekey {
    public static final String LANG_EN_IN = "en_in";
    public static final String LANG_EN_US = "en_us";
    private static String PREF_KEY = "VOICEKEY1";
    private double decibels;
    private String enrollmentPhrase;
    private Context context;
    private Model in_model;
    private Model us_model;
    private Model hi_model;
    private SpeakerModel spkModel;
    private String verificationPhrase;

    public Voicekey(Context context) throws IOException {
        this.context = context;
//        StorageService.unpack(context, "model-en-us", "model", (model) -> {
//            this.us_model = model;
//        }, (exception) -> {
//            System.out.println("Failed to unpack the model" + exception.getMessage());
//        });
        StorageService.unpack(context, "model-en-in", "model", (model) -> {
            this.in_model = model;
        }, (exception) -> {
            System.out.println("Failed to unpack the model" + exception.getMessage());
        });

        StorageService.unpack(context, "model-en-us", "model",
                (model) -> {
                    this.us_model = model;
                    Log.e("MODEL----","---"+model);

                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));


        StorageService11.unpack11(context, "model-spk", "model-spk",
                (model) -> {
                    this.spkModel = model;
                },
                (exception) -> setErrorState("Failed to unpack the model " + exception.getMessage()));


        this.verificationPhrase = "one two three four five";
        this.enrollmentPhrase = "one two three four five";
    }


    private void setErrorState(String message) {

    }

    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0D;
        double normA = 0.0D;
        double normB = 0.0D;

        for(int i = 0; i < vectorA.length; ++i) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2.0D);
            normB += Math.pow(vectorB[i], 2.0D);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public Result createUser(String name) {
        SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
        UUID uuid = UUID.randomUUID();
        String suuid = uuid.toString();
        Editor editor = preferences.edit();
        editor.putString(suuid + ":name", name);
        editor.commit();
        Result r = new Result();
        r.id = suuid;
        r.status = "Okay";
        r.success = true;
        return r;
    }

    public Result enrollUser(String id, String wave1, String wave2, String wave3, String language) throws IOException, JSONException {
        Result r = new Result();
        r.spokenPhrase = null;
        r.phraseMatch = false;
        String hypothesis = null;
        String text = null;
        double[] xspk=null;
        Recognizer recognizer = this.createRecognizer(language);
        if (this.checkEnvironment(wave1)) {
            r.status = "sample 1 is noisy.";
            r.success = false;
            r.id = id;
            return r;
        } else if (this.checkEnvironment(wave2)) {
            r.status = "sample 2 is noisy.";
            r.success = false;
            r.id = id;
            return r;
        } else if (this.checkEnvironment(wave3)) {
            r.status = "sample 3 is noisy.";
            r.success = false;
            r.id = id;
            return r;
        } else {
            FileInputStream fileInputStream = new FileInputStream(wave1);
            InputStream ais = fileInputStream;
            if (fileInputStream.skip(44L) != 44L) {
                r.status = "";
            } else {
                byte[] b = new byte[4096];

                int nbytes;
                while((nbytes = ais.read(b)) >= 0) {
                    recognizer.acceptWaveForm(b, nbytes);
                }

                hypothesis = recognizer.getFinalResult();
            }

            JSONObject res = new JSONObject(hypothesis);
            text = res.getString("text");
            if (!text.equals("")) {
                r.spokenPhrase = text;
                r.status = text;
                if (!res.has("spk")) {
                    r.success = false;
                    r.status = r.status + "(no speakerid)";
                } else {
                    if (text.equalsIgnoreCase(this.enrollmentPhrase)) {
                        r.phraseMatch = true;
                    }

                    JSONArray a = res.getJSONArray("spk");
                    int frames = 128;
                    xspk = new double[frames];

                    for(int i = 0; i < frames; ++i) {
                        double d = a.getDouble(i);
                        xspk[i] = d;
                    }

                    ByteBuffer bb = ByteBuffer.allocate(xspk.length * 8);
                    double[] var26 = xspk;
                    int var18 = xspk.length;

                    for(int var19 = 0; var19 < var18; ++var19) {
                        double dn = var26[var19];
                        bb.putDouble(dn);
                    }

                    byte[] b = bb.array();
                    String sxspk = Base64.encodeToString(b, 0);
                    SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
                    Editor editor = preferences.edit();
                    editor.putString(id + ":spk", sxspk);
                    editor.commit();
                    r.success = true;
                    r.status = "enrolled successfully";
                }
            } else {
                r.success = false;
                r.status = "(no text)";
            }

            r.id = id;
            return r;
        }
    }

    private Recognizer createRecognizer(String language) {
        return language.equalsIgnoreCase("en_in") ? new Recognizer(this.in_model, 16000.0F,this.spkModel) : new Recognizer(this.us_model, 16000.0F,this.spkModel);
    }

    private String getHypothesis(String waveFile, String language) throws IOException {
        String hypothesis = null;
        Recognizer recognizer = this.createRecognizer(language);
        FileInputStream fileInputStream = new FileInputStream(waveFile);
        InputStream ais = fileInputStream;
        if (fileInputStream.skip(44L) != 44L) {
            hypothesis = "{\"error\": \"input not wave file\"}";
        } else {
            byte[] b = new byte[4096];

            int nbytes;
            while((nbytes = ais.read(b)) >= 0) {
                recognizer.acceptWaveForm(b, nbytes);
            }

            hypothesis = recognizer.getFinalResult();
        }

        return hypothesis;
    }

    private double[] getSpeakerMatrix(String hypothesis) throws JSONException {
        String text = null;
        double[] xspk = null;
        JSONObject res = new JSONObject(hypothesis);
        text = res.getString("text");
        if (!text.equals("") && res.has("spk")) {
            JSONArray a = res.getJSONArray("spk");
            int frames = 128;
            xspk = new double[frames];

            for(int i = 0; i < frames; ++i) {
                double d = a.getDouble(i);
                xspk[i] = d;
            }
        }

        return xspk;
    }

    private String getEnrollName(String id) {
        SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
        String spkenroll = preferences.getString(id + ":name", (String)null);
        return spkenroll;
    }

    private String getEnrollPrefs(String id) {
        SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
        String spkenroll = preferences.getString(id + ":spk", (String)null);
        return spkenroll;
    }

    public boolean isSpokePhraseorrect(String wave1, String language) throws IOException {
        String hypothesis = this.getHypothesis(wave1, language);
        JSONObject res = null;
        try {
            res = new JSONObject(hypothesis);
            String spokenPhrase = res.getString("text");
            Log.e("spoke phrase => ",spokenPhrase);
            return spokenPhrase.equalsIgnoreCase(this.verificationPhrase);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;

    }

    public Result verifyUser(String id, String wave1, String language) throws IOException, JSONException {
        Log.e("ID for Verification =>",id);
        Result r = new Result();
        r.spokenPhrase = null;
        r.phraseMatch = false;
        String hypothesis = null;
        double[] xspk=null;
        if (this.checkEnvironment(wave1)) {
            r.status = "sample 1 is noisy.";
            r.success = false;
            r.id = id;
            return r;
        } else {
            hypothesis = this.getHypothesis(wave1, language);
            JSONObject res = new JSONObject(hypothesis);
            r.spokenPhrase = res.getString("text");
             xspk = this.getSpeakerMatrix(hypothesis);
            if (xspk != null) {
                String spkenroll = this.getEnrollPrefs(id);
                if (spkenroll != null) {
                    if (r.spokenPhrase.equalsIgnoreCase(this.verificationPhrase)) {
                        r.phraseMatch = true;
                    }

                    double d = this.getSimilarity(xspk, spkenroll);
                    r.success = true;
                    r.similarityScore = d;
                    r.status = "success";
                }
            } else {
                r.success = false;
                r.status = "(no text)";
            }

            r.name = this.getEnrollName(id);
            r.id = id;
            return r;
        }
    }

    private double getSimilarity(double[] xspk, String spkenroll) {
        int frames = 128;
        byte[] bdata = Base64.decode(spkenroll, 0);
        double[] espk = new double[frames];
        ByteBuffer byf = ByteBuffer.wrap(bdata, 0, bdata.length);

        for(int i = 0; i < espk.length; ++i) {
            espk[i] = byf.getDouble();
        }

        return cosineSimilarity(espk, xspk);
    }

    public Result deleteUser(String id) {
        SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
        Editor editor = preferences.edit();
        editor.remove(id + ":name");
        editor.remove(id + ":spk");
        editor.commit();
        Result r = new Result();
        r.id = id;
        r.status = "Deletion Successful";
        r.success = true;
        return r;
    }

    public String getPhrase() {
        return this.verificationPhrase;
    }

    public void setPhrase(String phrase) {
        this.verificationPhrase = phrase;
    }

    public boolean checkLicense() {
        return true;
    }

    public boolean renewLicense() {
        return true;
    }

    public String getAllEnrolled() throws JSONException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        SharedPreferences preferences = this.context.getSharedPreferences(PREF_KEY, 0);
        Map<String, ?> all = preferences.getAll();
        Iterator var5 = all.entrySet().iterator();

        while(var5.hasNext()) {
            Entry<String, ?> entry = (Entry)var5.next();
            JSONObject jsonObject = new JSONObject();
            String key = (String)entry.getKey();
            String[] split = key.split(":");
            if (split[1].equals("name")) {
                String id = split[0];
                String spk = preferences.getString(id + ":spk", "");
                jsonObject.put("id", id);
                jsonObject.put("name", entry.getValue().toString());
                jsonObject.put("spk", spk);
                array.put(jsonObject);
            }
        }

        object.put("enrolled", array);
        return object.toString();
    }

    public Result findSimilarUser(String wavefile, String language) throws IOException, JSONException {
        Result r = new Result();
        String hypothesis = null;
        double[] xspk=null;
        if (this.checkEnvironment(wavefile)) {
            r.status = "sample 1 is noisy. ";
            r.success = false;
            return r;
        } else {
            hypothesis = this.getHypothesis(wavefile, language);
            JSONObject res = new JSONObject(hypothesis);
            r.spokenPhrase = res.getString("text");
            xspk = this.getSpeakerMatrix(hypothesis);
            double dclose = 0.0D;
            String sclose = "";
            r.status = "failure";
            if (xspk != null) {
                String enrolled = this.getAllEnrolled();
                JSONObject object = new JSONObject(enrolled);
                JSONArray array = object.getJSONArray("enrolled");

                for(int i = 0; i < array.length(); ++i) {
                    JSONObject item = array.getJSONObject(i);
                    String id = item.getString("id");
                    String name = item.getString("name");
                    String spk = item.getString("spk");
                    Log.d("vk/vu/sl", "" + spk.length());
                    if (spk != "" && spk.length() > 0) {
                        double d = this.getSimilarity(xspk, spk);
                        if (d > dclose) {
                            r.id = id;
                            r.success = true;
                            r.name = name;
                            r.similarityScore = d;
                            r.status = "success";
                            dclose = d;
                        }
                    }
                }
            } else {
                r.success = false;
                r.status = "(no text)";
            }

            return r;
        }
    }

    public void setDecibels(double decibels) {
        this.decibels = decibels;
    }

    public boolean checkEnvironment(String waveFile) throws IOException {
        boolean r = false;
        double dbLevel = 0.0D;
        long size = (new File(waveFile)).length();
        size -= 44L;
        FileInputStream fileInputStream = new FileInputStream(waveFile);
        if (fileInputStream.skip(44L) != 44L) {
            dbLevel = 0.0D;
            r = false;
        } else {
            short[] samples = new short[(int)size];
            byte[] b = new byte[(int)size];
            fileInputStream.read(b);

            for(int i = 0; (long)i < size; ++i) {
                samples[i] = (short)b[i];
            }

            double sumOfSampleSq = 0.0D;
            double peakSample = 0.0D;
            short[] var16 = samples;
            int var17 = samples.length;

            for(int var18 = 0; var18 < var17; ++var18) {
                short sample = var16[var18];
                double normSample = (double)sample / 32767.0D;
                sumOfSampleSq += normSample * normSample;
                if ((double)Math.abs(sample) > peakSample) {
                    peakSample = (double)Math.abs(sample);
                }
            }

            double rms = 10.0D * Math.log10(sumOfSampleSq / (double)samples.length);
            double peak = 20.0D * Math.log10(peakSample / 32767.0D);
            dbLevel = rms / peak;
            if (dbLevel < this.decibels) {
                r = true;
            }
        }

        return r;
    }

    public String getEnrollmentPhrase() {
        return this.enrollmentPhrase;
    }

    public void setEnrollmentPhrase(String phrase) {
        this.enrollmentPhrase = phrase;
    }

    public class Enrolled {
        public String id;
        public double[] enroll;

        public Enrolled() {
        }
    }

    public class Result {
        public String id;
        public String name;
        public String status;
        public double confidence;
        public double threshold;
        public double similarityScore;
        public boolean success;
        public String spokenPhrase;
        public boolean phraseMatch;

        public Result() {
        }
    }
}

