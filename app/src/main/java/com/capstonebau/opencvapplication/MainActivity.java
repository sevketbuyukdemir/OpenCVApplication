package com.capstonebau.opencvapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    Context context = this;
    // For motion calculations
    public LinkedList<JSONObject> preparedJSONS;
    // For motion calculations

    // For openCV
    public static final String TAG = "src";
    // For openCV

    // For Bluetooth Connection
    public static int REQUEST_BLUETOOTH = 1;
    public boolean deviceBluetoothSupport = false;
    public BluetoothAdapter bluetoothAdapter;
    public ArrayList<BluetoothEraserDevice> bluetoothDevices;
    public BluetoothConnectionEraser bluetoothConnectionEraser;
    public Handler bluetoothHandler;
    // For Bluetooth Connection

    // For bottom settings bar
    private LinearLayout bottom_settings_bar;
    private LinearLayout settings_swipe_layout;
    private BottomSheetBehavior<LinearLayout> bottom_sheet_behavior;
    private ImageView settings_swipe_layout_arrow;
    public static boolean isBluetoothConnected = false;
    public static boolean isWhiteBoardSetted = false;
    private Button setup_bluetooth_button;
    private Button set_white_board_dimension_button;
    public static boolean isErasingContinue = false;
    public static int whiteboard_width;
    public static int whiteboard_height;
    public double whiteboard_top_edge;
    public double whiteboard_right_edge;
    public double whiteboard_bottom_edge;
    public double whiteboard_left_edge;
    // For bottom settings bar

    public void init() {
        // For bottom settings bar init
        bottom_settings_bar = findViewById(R.id.bottom_settings_bar);
        settings_swipe_layout = findViewById(R.id.settings_swipe_layout);
        bottom_sheet_behavior = BottomSheetBehavior.from(bottom_settings_bar);
        settings_swipe_layout_arrow = findViewById(R.id.settings_swipe_layout_arrow);
        setup_bluetooth_button = findViewById(R.id.setup_bluetooth_button);
        set_white_board_dimension_button = findViewById(R.id.setup_white_board_dimension_button);
        // For bottom settings bar init
        cameraView = (JavaCamera2View) findViewById(R.id.cameraview);

    }

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.wtf(TAG, "OpenCV failed to load!");
        }
    }

    private JavaCamera2View cameraView;

    /**
     * this function control opencv manager application and opencv library load process
     */
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.e("bluetoothAdapter", "Device hasn't got bluetooth support.");
        } else {
            deviceBluetoothSupport = true;
        }

        // Our icon toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Our icon toolbar

        // For bottom settings bar
        ViewTreeObserver vto = settings_swipe_layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            settings_swipe_layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            settings_swipe_layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        // int width = bottomSheetLayout.getMeasuredWidth();
                        int height = settings_swipe_layout.getMeasuredHeight();

                        bottom_sheet_behavior.setPeekHeight(height);
                    }
                });
        bottom_sheet_behavior.setHideable(false);
        bottom_sheet_behavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED:
                            {
                                settings_swipe_layout_arrow.setImageResource(R.drawable.bottom_settings_down_arrow);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED:
                            {
                                settings_swipe_layout_arrow.setImageResource(R.drawable.bottom_settings_up_arrow);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                settings_swipe_layout_arrow.setImageResource(R.drawable.bottom_settings_up_arrow);
                                break;
                        }
                    }
                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
                });
        // For bottom settings bar

        /**
         * This button set bluetooth connection with robot by using BluetoothConnectionEraser class
         */
        setup_bluetooth_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceBluetoothSupport){
                    setBluetoothFromUser();
                }
            }
        });
        /**
         * This button take whiteboard dimensions with AlertDialog from user.
         */
        set_white_board_dimension_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle("Set whiteboard dimensions");

                final EditText input_width = new EditText(context);
                input_width.setHint("Enter width");
                final EditText input_height = new EditText(context);
                input_height.setHint("Enter height");
                final LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.WRAP_CONTENT));
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.addView(input_width);
                linearLayout.addView(input_height);

                alert.setView(linearLayout);

                alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value_width = input_width.getText().toString();
                        String value_height = input_height.getText().toString();
                        whiteboard_width = Integer.parseInt(value_width);
                        whiteboard_height = Integer.parseInt(value_height);
                        isWhiteBoardSetted = true;
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            }
        });
        // These lines get android screen display metrics for best camera preview size.
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        cameraView.setCvCameraViewListener(this);
        cameraView.setMaxFrameSize(width, height);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        /*
        Mat input = inputFrame.rgba();
        if(isWhiteBoardSetted && isBluetoothConnected) {
            if (!isErasingContinue) {
                // Detection process starting
                Imgproc.cvtColor(input, input, Imgproc.COLOR_RGBA2RGB);
                Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2HSV_FULL);
                // Definition of image Mat object which predefined by OpenCV
                Mat line_detection_image = input;
                Mat circle_detection_image = input;

                Mat lines_vertical = new Mat();
                Mat lines_horizontal = new Mat();
                Mat circles = new Mat();
                // Image Blurring (Image Smoothing) Image blurring is achieved by convolving the image with a low-pass filter kernel.
                Imgproc.blur(line_detection_image, line_detection_image, new Size(7, 7), new Point(2, 2)); // todo enter actual value with whiteboard and robot
                // This line only determines the color to detect gray lines
                Core.inRange(line_detection_image, new Scalar(40, 40, 40), new Scalar(70, 255, 255), circle_detection_image); // todo enter actual value with whiteboard and robot
                // OpenCV function for detect lines
                // Math.PI/180 -> for detect horizontal lines
                Imgproc.HoughLines(line_detection_image, lines_horizontal, 1, Math.PI/180, 150); // todo enter actual value with whiteboard and robot
                // Math.PI/90 -> for detect vertical lines
                Imgproc.HoughLines(line_detection_image, lines_vertical, 1, Math.PI/90, 150); // todo enter actual value with whiteboard and robot

                ArrayList<double[]> circles_array_list = new ArrayList<>();

                if ((lines_horizontal.cols() + lines_vertical.cols()) >= 4) {
                    // These two for loop get whiteboard edges.
                    for (int i=0; i < Math.min(lines_horizontal.cols(), 3); i++ ) {
                        double lineVec[] = lines_horizontal.get(0, i);
                        if (lineVec == null) {
                            break;
                        }

                        whiteboard_top_edge = lineVec[0];
                        whiteboard_bottom_edge = lineVec[1];
                        if (whiteboard_top_edge > whiteboard_bottom_edge) {
                            double temp = whiteboard_top_edge;
                            whiteboard_top_edge = whiteboard_bottom_edge;
                            whiteboard_bottom_edge = temp;
                        }

                    }
                    for (int i=0; i < Math.min(lines_vertical.cols(), 3); i++ ) {
                        double[] lineVec = lines_vertical.get(0, i);
                        if (lineVec == null) {
                            break;
                        }
                        whiteboard_right_edge = lineVec[0];
                        whiteboard_left_edge = lineVec[1];
                        if (whiteboard_right_edge > whiteboard_left_edge) {
                            double temp = whiteboard_right_edge;
                            whiteboard_right_edge = whiteboard_left_edge;
                            whiteboard_left_edge = temp;
                        }

                    }
                    // Image Blurring (Image Smoothing) Image blurring is achieved by convolving the image with a low-pass filter kernel.
                    Imgproc.blur(circle_detection_image, circle_detection_image, new Size(7, 7), new Point(2, 2)); // todo enter actual value with whiteboard and robot
                    // This line only determines the color to detect green circles
                    Core.inRange(circle_detection_image, new Scalar(40, 40, 40), new Scalar(70, 255, 255), circle_detection_image); // todo enter actual value with whiteboard and robot
                    // OpenCV function for detect circles.
                    Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100, 100, 90, 0, 1000); // todo enter actual value with whiteboard and robot
                    if (circles.cols() > 0) {
                        for (int i=0; i < Math.min(circles.cols(), 5); i++ ) {
                            double[] circleVec = circles.get(0, i);
                            if (circleVec == null) {
                                break;
                            }
                            // add circles to arraylist for using in erasing process
                            circles_array_list.add(circleVec);

                            // This lines draw circle to camera preview
                            Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                            int radius = (int) circleVec[2];
                            Imgproc.circle(input, center, 1, new Scalar(255, 255, 255), 5);
                            Imgproc.circle(input, center, radius, new Scalar(255, 255, 255), 2);
                        }
                    }
                }
                // Detection process finishing
                // erasing processes starting
                MotionCalculations motionCalculations = new MotionCalculations(whiteboard_top_edge, whiteboard_bottom_edge,
                        whiteboard_left_edge, whiteboard_right_edge, circles_array_list);

                preparedJSONS = motionCalculations.calculate_motion();

                while (!preparedJSONS.isEmpty()) {
                    if (!bluetoothConnectionEraser.isOk()) {
                        bluetoothConnectionEraser.sendJsonFile(preparedJSONS.getFirst());
                    } else {
                        preparedJSONS.removeFirst();
                        bluetoothConnectionEraser.sendJsonFile(preparedJSONS.getFirst());
                    }
                }

                // erasing processes finish
                lines_horizontal.release();
                lines_vertical.release();
                circles.release();
            } else {
                Toast.makeText(context, "Please wait while deleting continues.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "Click SET BLUETOOTH and SET WHITEBOARD DIMENSION buttons.", Toast.LENGTH_LONG).show();
        }
        input.release();
        return inputFrame.rgba();
         */
        Mat input = inputFrame.gray();
        Mat circles = new Mat();
        Imgproc.blur(input, input, new Size(7, 7), new Point(2, 2));
        Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100, 100, 90, 4, 400);

        Log.i(TAG, String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));

        if (circles.cols() > 0) {
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                Imgproc.circle(input, center, 3, new Scalar(255, 255, 255), 5);
                Imgproc.circle(input, center, radius, new Scalar(255, 255, 255), 2);
            }
        }

        circles.release();
        input.release();
        return inputFrame.rgba();
    }

    /**
     * This function call in set bluetooth button click listener
     */
    public void setBluetoothFromUser() {
        enable_bluetooth_device();
        findBluetoothDevices();
        for (BluetoothEraserDevice device : bluetoothDevices) {
            if(device.getDevice_name().equals("bt_awe1")) {
                bluetoothHandler = new Handler();
                bluetoothConnectionEraser = new BluetoothConnectionEraser(context, bluetoothHandler);
                bluetoothConnectionEraser.connectDevice(device);
                isBluetoothConnected = true;
                break;
            }
        }
    }

    /**
     * This function take permission from user for using phone bluetooth.
     * This is necessary for application and robot communication
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH)
        {
            if (resultCode == 0)
            {
                Toast.makeText(this, "Give permission to Bluetooth for using app.",
                        Toast.LENGTH_LONG).show();
            }
            else {

            }
        }
    }

    /**
     * This function open bluetooth service of android phone
     */
    private void enable_bluetooth_device() {
        if (bluetoothAdapter == null)
        {
            finish(); // finish all application because this phone cannot use our application
        }
        if( !bluetoothAdapter.isEnabled())
        {
            Intent enable_bluetooth_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_bluetooth_intent, REQUEST_BLUETOOTH);
        }
    }

    /**
     * this function find enable bluetooth device in device bluetooth range
     */
    private void findBluetoothDevices() {
        bluetoothDevices = new ArrayList<BluetoothEraserDevice>();

        bluetoothAdapter.startDiscovery(); // discovery for find bluetooth devices

        final BroadcastReceiver bluetooth_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    BluetoothEraserDevice bluetoothEraserDevice = new BluetoothEraserDevice();
                    bluetoothEraserDevice.setDevice_name(device.getName());
                    bluetoothEraserDevice.setDevice_address(device.getAddress());
                    bluetoothEraserDevice.setDevice_bond_state(device.getBondState());
                    bluetoothEraserDevice.setDevice_type(device.getType());    // requires API 18 or higher
                    bluetoothEraserDevice.setDevice_uuids(device.getUuids());

                    bluetoothDevices.add(bluetoothEraserDevice);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetooth_receiver, filter);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
    }
}