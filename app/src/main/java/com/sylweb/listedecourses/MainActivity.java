package com.sylweb.listedecourses;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String WEB_SERVICE = "http://digitalapi.vetoquinol.com/api.php/records/";

    private ListView myList;
    private ImageView addButton;
    private ImageView syncButton;
    private ImageView sendMailButton;
    private EditText articleName;
    private EditText articleQuantity;

    private MessageReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DBManager.initDB(this);

        this.myList = findViewById(R.id.listView);
        this.myList.setAdapter(new MyAdapter(this, new ArrayList<Article>()));

        this.articleName = findViewById(R.id.articleName);
        this.articleQuantity = findViewById(R.id.articleQuantity);

        this.addButton = findViewById(R.id.addButton);
        this.addButton.setOnClickListener(this);

        this.syncButton = findViewById(R.id.syncButton);
        this.syncButton.setOnClickListener(this);

        this.sendMailButton = findViewById(R.id.sendMailButton);
        this.sendMailButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(this.myReceiver == null) this.myReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter("DataReady"));
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter("AskForUpdate"));
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter("DisplayMessage"));

        articleName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    //Clear focus here from edittext
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(articleName.getWindowToken(), 0);
                    articleName.clearFocus();
                }
                return false;
            }
        });

        new UpdateThread().start();
    }

    @Override
    public void onPause() {
        if(this.myReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(addButton)) {

            if(!articleName.getText().toString().trim().equals("")) {

                String query = "INSERT INTO article(id, name, quantity, deleted, modified_on) VALUES('%s', '%s', %d, 0, %d)";
                long modifiedOn = Calendar.getInstance().getTimeInMillis();
                String id = "TEMP_" + modifiedOn;
                int qty = 1;
                if (!articleQuantity.getText().toString().trim().equals("")) {
                    qty = Integer.valueOf(articleQuantity.getText().toString().trim());
                }
                query = String.format(query, id, articleName.getText().toString(), qty, modifiedOn);
                DBManager.executeQuery(query);

                this.articleName.setText("");
                this.articleQuantity.setText("");

                new UpdateThread().start();
            }
        }
        else if(v.equals(syncButton)) {
            startSyncAnimation();
            new SynchroThread(this).start();
        }
        else if(v.equals(sendMailButton)) {
            String pdfName = PDFUtils.exportReportAsPDF(this, ((MyAdapter)this.myList.getAdapter()).data);
            sendPDFViaEmail(pdfName);
        }
    }

    private void startSyncAnimation() {
        syncButton.clearAnimation();
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(1000);
        rotate.setRepeatCount(Animation.INFINITE);
        syncButton.startAnimation(rotate);
    }

    private void update(ArrayList<Article> data) {
        if(data != null) ((MyAdapter)this.myList.getAdapter()).data = data;
        else ((MyAdapter)this.myList.getAdapter()).data = new ArrayList<>();
        ((MyAdapter)this.myList.getAdapter()).notifyDataSetChanged();
    }

    public class UpdateThread extends Thread {

        @Override
        public void run() {
            ArrayList<Article> results = new ArrayList<>();
            String query = "SELECT * FROM article WHERE deleted=0";
            ArrayList<HashMap> records = DBManager.executeQuery(query);
            for(Object entry : records) {
                HashMap a = (HashMap) entry;
                results.add(new Article(a));
            }
            Intent i = new Intent("DataReady");
            i.putExtra("data", results);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("DataReady")) {
                update((ArrayList<Article>) intent.getExtras().getSerializable("data"));
            }
            else if(intent.getAction().equals("AskForUpdate")) {
                syncButton.clearAnimation();
                new UpdateThread().start();
            }
            else if(intent.getAction().equals("DisplayMessage")) {
                String message = intent.getExtras().getString("message");
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public class SynchroThread extends Thread {

        private Context c;

        public SynchroThread(Context c) {
            this.c = c;
        }

        @Override
        public void run() {
            //Check if there's a force update flag because of a db structure update
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

            //Read when last synchro was achieved for this user
            String dataSyncKey = "last_data_sync";
            long lastSyncDate = prefs.getLong(dataSyncKey, 0);

            boolean success = sync(lastSyncDate);

            //If data synchronization worked, save the current date as the last sync date
            if (success) {
                lastSyncDate = Calendar.getInstance().getTimeInMillis();
                prefs.edit().putLong(dataSyncKey, lastSyncDate);
                Intent i = new Intent("DisplayMessage");
                i.putExtra("message", "Synchronisation réussie.");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }
            else {
                Intent i = new Intent("DisplayMessage");
                i.putExtra("message", "Echec de la synchronisation.");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }
            Intent i = new Intent("AskForUpdate");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }

        private boolean sync(long since) {

            boolean returnValue = true;

            String query = String.format("%sarticle?transform=1&filter=modified_on,gt,%d",WEB_SERVICE, since);

            JSONObject answer = HttpRequestHelper.executeGET(query);
            JSONArray results = null;
            try {
                results = answer.getJSONArray("records");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject a = (JSONObject) results.get(i);
                    Article remoteEntry = new Article(a);

                    query = "SELECT * FROM article WHERE id LIKE '%s' LIMIT 1";
                    query = String.format(query, remoteEntry.id);
                    ArrayList r = DBManager.executeQuery(query);
                    Article localEntry = null;

                    if (r != null && r.size() > 0) {
                        localEntry = new Article((HashMap) r.get(0));
                    }

                    //If there's no local entry for this operation instance or if remote modification is newer than local on then UPDATE local DB
                    if (localEntry == null || (remoteEntry.modified_on > localEntry.modified_on)) {
                        query = "SELECT id FROM article WHERE id LIKE '%s'";
                        query = String.format(query, remoteEntry.id);
                        ArrayList re = DBManager.executeQuery(query);
                        if(re != null && re.size() == 1) {
                            //Update entry
                            query = "UPDATE article SET name = '%s', quantity=%d, deleted=%d, modified_on = %d WHERE id LIKE '%s'";
                            query = String.format(query, remoteEntry.name, remoteEntry.quantity, remoteEntry.deleted, remoteEntry.modified_on, remoteEntry.id);
                            DBManager.executeQuery(query);
                        }
                        else {
                            //Insert entry
                            query = "INSERT INTO article(id,name,quantity,deleted,modified_on) VALUES('%s','%s',%d,%d,%d)";
                            query = String.format(query, String.valueOf(remoteEntry.id), remoteEntry.name, remoteEntry.quantity, remoteEntry.deleted, remoteEntry.modified_on);
                            DBManager.executeQuery(query);
                        }
                    }
                }
            } catch (Exception ex) {
                returnValue &= false;
            }

            //Now check all local entries to see if some are newer since last sync
            query = "SELECT * FROM article WHERE modified_on > %d";
            query = String.format(query, since);
            ArrayList localRecords = DBManager.executeQuery(query);
            for (int i = 0; i < localRecords.size(); i++) {
                Article localEntry = new Article((HashMap) localRecords.get(i));

                //We are not interested in new entries yet...
                if (localEntry.id.contains("TEMP_")) continue;

                String request = "%sarticle?transform=1&filter=id,eq,%s";
                request = String.format(request,WEB_SERVICE, localEntry.id);

                answer = HttpRequestHelper.executeGET(request);
                results = null;
                try {
                    results = answer.getJSONArray("records");

                    for (int j = 0; j < results.length(); j++) {
                        Article remoteEntry = new Article((JSONObject) results.get(j));

                        if (remoteEntry != null && (localEntry.modified_on > remoteEntry.modified_on)) {

                            localEntry.modified_on = Calendar.getInstance().getTimeInMillis();

                            query = "%s/article/%s";
                            query = String.format(query, WEB_SERVICE, remoteEntry.id);

                            String answer2 = HttpRequestHelper.executePUT(query, localEntry.getAsJSON());
                            if (answer2 == null) return false;
                        }

                    }
                } catch (Exception ex) {
                    returnValue &= false;
                }
            }

            query = "SELECT * FROM article WHERE id like 'TEMP_%'";
            ArrayList r = DBManager.executeQuery(query);

            for (int i = 0; i < r.size(); i++) {

                Article entry = new Article((HashMap) r.get(i));

                //We want to change the modified_on field so if someone sync app between the moment of our local modification
                // and the moment we post it to server, the other user will receive this modification too
                entry.modified_on =Calendar.getInstance().getTimeInMillis();

                query = WEB_SERVICE + "article";

                JSONObject temp = entry.getAsJSON();
                temp.remove("id");
                String createdId = HttpRequestHelper.executePOST(query, temp);
                try {

                } catch (NumberFormatException ex) {
                    return false;
                }

                if (createdId == null || createdId.equals("0")) return false;


                query = "UPDATE article SET id = '%s' WHERE id LIKE '%s'";
                query = String.format(query, createdId, entry.id);
                DBManager.executeQuery(query);
            }

            return returnValue;
        }

    }

    private void sendPDFViaEmail(String pdfName) {
        try {
            String pdfPath = this.getFilesDir().getAbsolutePath()+ File.separator+"pdf"+File.separator;
            File pdf = new File(pdfPath + pdfName);
            //Ask user to choose what app will be e-mail provider and send pdf as joined file
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Liste de courses");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Voici notre liste de courses");
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, pdf));
            startActivityForResult(shareIntent, 1);
        }
        catch(ActivityNotFoundException ex) {
            Toast.makeText(this, "No e-mail client found", Toast.LENGTH_LONG).show();
        }
    }
}
