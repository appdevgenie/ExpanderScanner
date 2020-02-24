package com.appdevgenie.expanderscanner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.ua.jenchen.drivers.mcp23017.MCP23017;
import com.ua.jenchen.drivers.mcp23017.MCP23017GPIO;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String I2C_DEVICE_NAME = "I2C1";//Pi
    private final static String TAG = "I2C address scanner";
    private final static long DELAY_BETWEEN_SCANS = 5; //in seconds
    private final static int TEST_REGISTER = 0x0;
    private static final int MCP_ADDRESS = 0x20; // dec 23
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 250;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final PeripheralManager peripheralManager = PeripheralManager.getInstance();

    private MCP23017 mcp23017; // gpio A0 to A7 and B0 to B7
    private Gpio mcpA6, mcpA5, mcpA7, mcpB0;
    private Handler mHandler = new Handler();
    private boolean mLedState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.d(TAG, "on create");

        /*try {

            mcpA6 = PeripheralManager.getInstance().openGpio("BCM26");
            mcpA6.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            Log.i(TAG, "Start blinking LED GPIO pin");
            // Post a Runnable that continuously switch the state of the GPIO, blinking the
            // corresponding LED
            mHandler.post(mBlinkRunnable);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }*/

        try {
            mcp23017 = new MCP23017(
                    I2C_DEVICE_NAME,    // required I2C
                    MCP_ADDRESS        // address of MCP23017
            );
        } catch (IOException e) {
            // couldn't configure the IO expander
            Log.d(TAG, "error: " + e.getMessage());
        }

        //scanPiPorts();
        //Log.i(TAG, "Address: " + mcp23017.getAddress());


        try {

            mcpA5 = mcp23017.openGpio(MCP23017GPIO.A5);
            mcpA5.setDirection(Gpio.DIRECTION_IN);
            mcpA5.setActiveType(Gpio.ACTIVE_LOW);
            mcpA5.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mcpA5.registerGpioCallback(mGpioCallback);

            mcpA6 = mcp23017.openGpio(MCP23017GPIO.A6);
            mcpA7 = mcp23017.openGpio(MCP23017GPIO.A7);
            mcpB0 = mcp23017.openGpio(MCP23017GPIO.B0);
            // Initialize the pin as a high output
            mcpA6.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mcpA7.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mcpB0.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            // Low voltage is considered active
            //mcpA6.setActiveType(Gpio.ACTIVE_LOW);
            // Toggle the value to be LOW
            //mcpA6.setValue(true);
            //Log.i(TAG, "Start blinking LED GPIO pin");

            mHandler.post(mBlinkRunnable);
        } catch (IOException e) {
            // couldn't configure GPIO
            Log.d(TAG, "error: " + e.getMessage());
        }



        //performScan();

        /*if (savedInstanceState == null) {

            final Callable<Void> scan = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    while (true) {
                        TimeUnit.SECONDS.sleep(DELAY_BETWEEN_SCANS);
                        performScan();
                    }
                }
            };

            executorService.submit(scan);
        }*/
    }

    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            // Read the active low pin state
            try {
                mcpB0.setValue(mcpA5.getValue());
                /*if (mcpA5.getValue()) {
                    // Pin is HIGH
                    //Log.d(TAG, "pressed");
                    mcpB0.setValue(true);
                } else {
                    // Pin is LOW
                    //Log.d(TAG, "released");
                    mcpB0.setValue(false);
                }*/
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    };

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            /*if (mcpA6 == null) {
                return;
            }*/
            try {
                // Toggle the GPIO state
                mLedState = !mLedState;
                mcpA6.setValue(mLedState);
                mcpA7.setValue(!mLedState);
                //Log.d(TAG, "State set to " + mLedState);

                // Reschedule the same runnable in {#INTERVAL_BETWEEN_BLINKS_MS} milliseconds
                mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (Exception e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private void scanPiPorts() {
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getGpioList();
        if (portList.isEmpty()) {
            Log.i(TAG, "No GPIO port available on this device.");
        } else {
            Log.i(TAG, "List of available ports: " + portList);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //executorService.shutdownNow();
        mHandler.removeCallbacks(mBlinkRunnable);
        try {
            mcp23017.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void performScan() {

        List<String> deviceList = peripheralManager.getI2cBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        for (int address = 0; address < 256; address++) {

            //address 0x20 MCP23017
            //auto-close the devices
            try (final I2cDevice device = peripheralManager.openI2cDevice(I2C_DEVICE_NAME, address)) {

                try {
                    device.readRegByte(TEST_REGISTER);
                    Log.i(TAG, String.format(Locale.US, "Trying: 0x%02X - SUCCESS", address));
                } catch (final IOException e) {
                    Log.i(TAG, String.format(Locale.US, "Trying: 0x%02X - FAIL", address));
                }

            } catch (final IOException e) {
                //in case the openI2cDevice(name, address) fails
            }
        }
    }
}
