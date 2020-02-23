package com.appdevgenie.expanderscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private static final String I2C_DEVICE_NAME = "I2C1";
    private final static String TAG = "I2C address scanner";
    private final static long DELAY_BETWEEN_SCANS = 5; //in seconds
    private final static int TEST_REGISTER = 0x0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final PeripheralManager peripheralManager = PeripheralManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //performScan();

        if (savedInstanceState == null) {

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
        }
    }

    @Override
    protected void onDestroy() {
        //executorService.shutdownNow();
        super.onDestroy();
    }

    private void performScan() {

        /*List<String> deviceList = peripheralManager.getI2cBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }*/

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
