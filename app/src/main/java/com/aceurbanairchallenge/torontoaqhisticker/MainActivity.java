package com.aceurbanairchallenge.torontoaqhisticker;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    //This refers to the image display
    ImageView targetImage;

    //This Hashmap is used to store AQHI value per location
    HashMap<String, String> aqhi_values = new HashMap<String, String>();

    //This refers to the button to change location
    Button areaBtn = null;

    //All the stickered images will be saved to this folder
    File imageRoot = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "TorontoAQHI");

    //This sets whether we're showing date menu or not; by default it's false.
    boolean showDateTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Select photo here
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 0);
            }
        });

        targetImage = findViewById(R.id.targetimage);
        areaBtn = findViewById(R.id.locationBtn);

        //Get the urls to Toronto AQHI values from the AQHI_XML file
        XmlResourceParser parser = this.getResources().getXml(R.xml.aqhi_xml_file_list);

        try {
            int eventType = parser.next();
            boolean areaFound = false;
            boolean pathFound = false;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (pathFound && eventType == XmlResourceParser.TEXT) {
                    //Get the url to get the current AQHI value
                    String url = parser.getText();
                    //Reset all flags
                    areaFound = false;
                    pathFound = false;
                    //Execute the AsyncTask to grab the AQHI value from url
                    new DataGrabber(url).execute();
                } else if (eventType == XmlResourceParser.START_TAG) {
                    if (areaFound && tagName.equalsIgnoreCase("pathToCurrentObservation")) {
                        //Set the flag for text
                        pathFound = true;
                    } else if (tagName.equalsIgnoreCase("region")) {
                        //Check if we have the right region
                        String areaName = parser.getAttributeValue(null, "nameEn");
                        if (areaName.startsWith("Toronto ")) {
                            areaFound = true;
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //Display the selected photo with the AQHI index
            Uri targetUri = data.getData();
            Bitmap bitmap;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                options.inScaled = false;
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri), null, options);
                //Draw the text on the photo here
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                paint.setColor(Color.WHITE); // Text Color
                int textHeight = 200;
                paint.setTextSize(textHeight); // Text Size
                paint.setTypeface(Typeface.create("Abel", Typeface.NORMAL));
                canvas.drawBitmap(bitmap, 0, 0, paint);
                //Draw the selected aqhi value
                String aqhi = aqhi_values.get(areaBtn.getText());
                canvas.drawText(aqhi, 10, textHeight, paint);
                //Draw the current datetime, if applicable
                if (showDateTime) {
                    Date current = new Date();
                    String date = new SimpleDateFormat("dd/MM/yy").format(current);
                    String time = new SimpleDateFormat("HH:mm").format(current);
                    textHeight += 200;
                    canvas.drawText(date, 10, textHeight, paint);
                    textHeight += 200;
                    canvas.drawText(time, 10, textHeight, paint);
                }
                targetImage.setImageBitmap(bitmap);

                //Show the location button
                areaBtn.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    public void onClickLocation(View v) {
        // Show the preference screen here to select the current location
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        final View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        //Build the selections if needed
        LinearLayout popupLayout = popupView.findViewById(R.id.popup_view);
        if (popupLayout.getChildCount() == 0) {
            for (String location : aqhi_values.keySet()) {
                TextView areaOption = new TextView(this);
                areaOption.setClickable(true);
                areaOption.setText(location);
                areaOption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
                areaOption.setTextColor(Color.BLACK);
                areaOption.setPadding(0,0,0,16);
                areaOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Set the current location to the selected area
                        TextView txtView = (TextView) view;
                        areaBtn.setText(txtView.getText());
                        //Also close the popup
                        popupWindow.dismiss();
                    }
                });

                popupLayout.addView(areaOption);
            }
        }

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            //TODO: do the saving on AsyncTask if possible
            //Get the bitmap of the AQHI image
            Bitmap bmp = ((BitmapDrawable)targetImage.getDrawable()).getBitmap();

            //Create the folder if it doesn't exist yet
            if (!imageRoot.exists()) {
                imageRoot.mkdir();
            }

            //Actually save the image with AQHI
            File file = new File(imageRoot, new Date().toString() + ".jpg");
            try {
                OutputStream stream = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG,100,stream);
                stream.flush();
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Broadcast intent to Media so the photo will be in Gallery
            Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(android.net.Uri.parse(file.toURI().toString()));
            sendBroadcast(mediaScanIntent);

            //Notify user with Snackbar
            Snackbar snackbar = Snackbar
                    .make(getWindow().getDecorView(), "Image saved!", Snackbar.LENGTH_LONG);
            snackbar.show();
        } else if (id == R.id.action_showDate) {
            //Set the showDateTime flag depending on whether menu item is checked or not
            showDateTime = !item.isChecked();
            item.setChecked(showDateTime);
        }

        return super.onOptionsItemSelected(item);
    }

    //New class for the Asynctask, where the data will be fetched in the background
    public class DataGrabber extends AsyncTask<Void, Void, Void> {

        private Document doc = null;
        private String url = null;

        public DataGrabber(String xmlUrl) {
            url = xmlUrl;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // NO CHANGES TO UI TO BE DONE HERE
            try {
                doc = Jsoup.connect(url).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //This is where we update the UI with the acquired data
            if (doc != null){
                //Grab the location & current AQHI elements
                Elements locationElement = doc.getElementsByTag("region");
                Elements aqhiElement = doc.getElementsByTag("airQualityHealthIndex");
                String location = locationElement.attr("nameEn");
                //Set the first AQHI location as the current selection
                boolean isCurrentLocation = aqhi_values.isEmpty();
                //Show current location on the button
                if (isCurrentLocation) {
                    areaBtn.setText(location);
                }
                //We might need to round the aqhi index value
                float aqhiFl = Float.valueOf(aqhiElement.text());
                String aqhi = String.valueOf(Math.round(aqhiFl)) + " AQHI";
                //Finally we can store the aqhi index for this location
                aqhi_values.put(location, aqhi);
            }else{
                System.out.print("Failed to get AQHI information.");
            }
        }
    }
}
