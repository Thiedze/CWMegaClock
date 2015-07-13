package campuswoche.thiems.de.cwmegaclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;


public class MegaClock extends Activity {

    public static final String PREFERENCES_NAME = "MegaClockSettings";

    private SeekBar green, blue, red, brightness;
    private ToggleButton points;
    private boolean showTemp = false;
    private boolean togglePoints = false;
    private String local_temp = "";
    private int green_value, blue_value, red_value;
    private float brightness_value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mega_clock);

        green = (SeekBar) findViewById(R.id.sbGreen);
        blue = (SeekBar) findViewById(R.id.sbBlue);
        red = (SeekBar) findViewById(R.id.sbRed);
        brightness = (SeekBar) findViewById(R.id.sbBrightness);
        Button temp;
        temp = (Button) findViewById(R.id.btLocalTemp);
        Button btColorPicker;
        btColorPicker = (Button) findViewById(R.id.btColorPicker);
        points = (ToggleButton) findViewById(R.id.tbPoints);

        points.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                togglePoints = isChecked;
                new Thread(new ClientThread()).start();
            }
        });

        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTemp = true;
                local_temp = getTemperature();
                new Thread(new ClientThread()).start();
            }
        });

        btColorPicker.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

        green.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new Thread(new ClientThread()).start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                green_value = (int) (progress * brightness_value);
                new Thread(new ClientThread()).start();
            }
        });

        blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new Thread(new ClientThread()).start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                blue_value = (int) (progress * brightness_value);
                new Thread(new ClientThread()).start();
            }
        });

        red.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new Thread(new ClientThread()).start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                red_value = (int) (progress * brightness_value);
                new Thread(new ClientThread()).start();
            }
        });

        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                brightness_value = (progress / (float) 100);

                red_value = (int) (red.getProgress() * brightness_value);
                green_value = (int) (green.getProgress() * brightness_value);
                blue_value = (int) (blue.getProgress() * brightness_value);

                new Thread(new ClientThread()).start();
            }
        });
        new Thread(new ClientThread()).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadState();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        loadState();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    public void onDestroy() {
        saveState();
        super.onDestroy();
        System.exit(0);
    }

    public void loadState() {
        SharedPreferences settings;
        settings = getSharedPreferences(PREFERENCES_NAME, 0);
        red_value = settings.getInt("RedValue", 100);
        green_value = settings.getInt("GreenValue", 100);
        blue_value = settings.getInt("BlueValue", 100);
        brightness_value = settings.getFloat("BrightnessValue", 1);
        togglePoints = settings.getBoolean("TogglePoints", true);

        red.setProgress((int)(red_value / brightness_value));
        green.setProgress((int)(green_value / brightness_value));
        blue.setProgress((int)(blue_value / brightness_value));
        brightness.setProgress((int) (brightness_value * 100));
        points.setChecked(togglePoints);
    }

    public void saveState() {
        SharedPreferences settings;
        settings = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("RedValue", red_value);
        editor.putInt("GreenValue", green_value);
        editor.putInt("BlueValue", blue_value);
        editor.putFloat("BrightnessValue", brightness_value);
        editor.putBoolean("TogglePoints", togglePoints);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mega_clock, menu);
        return true;
    }

    public String getTemperature() {
        String temp = "-20";
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location locationGPS = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            URL url = null;

            if (locationGPS != null)
                url = new URL(
                        "http://api.openweathermap.org/data/2.5/weather?lat="
                                + locationGPS.getLatitude() + "&lon="
                                + locationGPS.getLongitude() + "&mode=xml");
            else if (locationNet != null)
                url = new URL(
                        "http://api.openweathermap.org/data/2.5/weather?lat="
                                + locationNet.getLatitude() + "&lon="
                                + locationNet.getLongitude() + "&mode=xml");

            HttpURLConnection con = null;
            if (url != null) {
                con = (HttpURLConnection) url.openConnection();
            }

            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            if (con != null) {
                parser.setInput(con.getInputStream(), null);
            }
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "current");
            while (true) {
                parser.next();
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().contains("temperature")) {
                    temp = parser.getAttributeValue(0);
                    break;
                }

                if (parser.getName().contains("city")) {
                    Context context = getApplicationContext();
                    CharSequence text = parser.getAttributeValue(1);
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }

            // in.close();
        } catch (Exception e) {
            Context context = getApplicationContext();
            CharSequence text = "Please check your internet connection.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        int t = (int) (Float.parseFloat(temp) - 273.0);
        temp = String.valueOf(t);

        return temp;
    }

    class ClientThread implements Runnable {

        private DatagramSocket socket;
        private final int port = 4242;

        private void InitSocket() throws SocketException {
            if (socket == null) {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(new InetSocketAddress(port));
            }
        }

        @Override
        public void run() {
            try {

                InitSocket();
                InetAddress serverIP = InetAddress.getByName("255.255.255.255");

                if (showTemp) {
                    byte[] temp = ("Temp:" + local_temp + "\n").getBytes();
                    DatagramPacket out = new DatagramPacket(temp, temp.length,
                            serverIP, port);
                    socket.send(out);
                    showTemp = false;
                } else {
                    byte[] toggle = ("TogglePoints:"
                            + ((togglePoints) ? '1' : '0') + "\n").getBytes();
                    DatagramPacket out2 = new DatagramPacket(toggle,
                            toggle.length, serverIP, port);
                    socket.send(out2);

                    byte[] color = ("Color:" + green_value + ":" + blue_value
                            + ":" + red_value + "\n").getBytes();
                    DatagramPacket out = new DatagramPacket(color,
                            color.length, serverIP, port);
                    socket.send(out);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
