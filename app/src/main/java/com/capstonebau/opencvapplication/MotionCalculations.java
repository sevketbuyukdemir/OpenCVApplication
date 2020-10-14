package com.capstonebau.opencvapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;

public class MotionCalculations {
    // Direction constants
    public final int ROBOT_DIRECTION_NORTH = 0;
    public final int ROBOT_DIRECTION_SOUTH = 1;
    public final int ROBOT_DIRECTION_WEST = 2;
    public final int ROBOT_DIRECTION_EAST = 3;
    public final int ROBOT_DIRECTION_NORTHWEST = 4;
    public final int ROBOT_DIRECTION_NORTHEAST = 5;
    public final int ROBOT_DIRECTION_SOUTHWEST = 6;
    public final int ROBOT_DIRECTION_SOUTHEAST = 7;

    // temporary direction variable
    public int robot_direction = ROBOT_DIRECTION_NORTH;

    // whiteboard's dimensions as centimeter
    public int whiteboard_height;
    public int whiteboard_width;

    // robot's dimensions as centimeter
    public final int robot_width = 10; // todo enter actual value with robot
    public final int robot_height = 20; // todo enter actual value with robot

    // whiteboard edges locations which detected with OpenCV
    public int whiteboard_top;
    public int whiteboard_bottom;
    public int whiteboard_left;
    public int whiteboard_right;

    // robot's position on whiteboard
    public int current_x = robot_width/2;
    public int current_y = robot_height/2;

    // circles locations and centers which detected with OpenCV
    public ArrayList<double[]> circles_double;
    public ArrayList<int[]> circles;

    // JSON processes objects
    /**
     * Use this LinkedList as FIFO data structure with add() and removeFirst() function because,
     * application have to send location continuously.
     * preparedJSONS.add(jsonObject);
     * preparedJSONS.removeFirst();
     */
    public LinkedList<JSONObject> preparedJSONS = new LinkedList<>();
    public LinkedList<JSONObject> reversePreparedJSONS = new LinkedList<>();

    JSONObject jsonObject;

    /**
     * Constructor method for create MotionCalculations object in mainActivity
     */
    public MotionCalculations (double top, double bottom, double left, double right,
                               ArrayList<double[]> circles_t) {
        whiteboard_height = MainActivity.whiteboard_height;
        whiteboard_width = MainActivity.whiteboard_width;
        whiteboard_top = (int) top;
        whiteboard_bottom = (int) bottom;
        whiteboard_left = (int) left;
        whiteboard_right = (int) right;
        circles_double = circles_t;
        double_to_int();
    }

    /**
     * This function is our main motion function which call in mainActivity. This
     * function moves the robot from the top left of the board to the bottom right
     * and returns it to the starting point.
     */

    public LinkedList<JSONObject> calculate_motion () {
        while (!(current_y == whiteboard_height)) {
            if (robot_direction == ROBOT_DIRECTION_NORTH) {
                // starting location
                if (control_horizontal_circle()) {
                    int[] closest_circle = min_x_circle();
                    adjust_data_to_north(0, 0, closest_circle[0] - (closest_circle[2] + current_x), 0);
                    current_x = closest_circle[0];
                    int m = whiteboard_top - closest_circle[1];
                    if (m > robot_height) {
                        top_circulation(closest_circle);
                    } else {
                        bottom_circulation(closest_circle);
                    }
                } else {
                    adjust_data_to_north(0,0, whiteboard_left - current_x, 0);
                    make_u_turn_to_bottom();
                }
            } else if (robot_direction == ROBOT_DIRECTION_EAST) {
                if (control_horizontal_circle()) {
                    int[] closest_circle = min_x_circle();
                    adjust_data_to_north(0, 0, closest_circle[0] - (closest_circle[2] + current_x), 0);
                    current_x = closest_circle[0];
                    int m = whiteboard_top - closest_circle[1];
                    if (m > robot_height) {
                        top_circulation(closest_circle);
                    } else {
                        bottom_circulation(closest_circle);
                    }
                } else {
                    adjust_data_to_north(0,0, whiteboard_left - current_x, 0);
                    make_u_turn_to_bottom();
                }
            } else if (robot_direction == ROBOT_DIRECTION_WEST) {
                // starting location
                if (control_horizontal_circle()) {
                    int[] closest_circle = min_x_circle();
                    adjust_data_to_north(0, 0, closest_circle[0] - (closest_circle[2] + current_x), 0);
                    current_x = closest_circle[0];
                    int m = whiteboard_top - closest_circle[1];
                    if (m > robot_height) {
                        top_circulation(closest_circle);
                    } else {
                        bottom_circulation(closest_circle);
                    }
                } else {
                    adjust_data_to_north(0,0, current_x - whiteboard_right, 0);
                    make_u_turn_to_bottom();
                }
            }
        }
        combine_json_files();
        return preparedJSONS;
    }

    /**
     * This function checks whether the circle is horizontally same high with the robot.
     */
    public boolean control_horizontal_circle () {
        boolean there_is;
        for (int i = 0; i < circles.size(); i++) {
            int[] circle = circles.get(i);
            int max_height = circle[1] + circle[2]; // circle center y + circle radius
            int min_height = circle[1] - circle[2]; // circle center y - circle radius
            if (current_y >= min_height || current_y <= max_height) {
                return true;
            }
        }
        return false;
    }

    /**
     * This function will move the robot over the circle if there is no place where it
     * can pass when the robot encounters the circle.
     */
    public void top_circulation (int[] circle) {
        int up_down = circle[2] + robot_height/2;
        int horizontal = circle[2]*2 + robot_width;
        if(robot_direction  == ROBOT_DIRECTION_WEST) {
            adjust_data_to_north(up_down, 0, 0, 0);
            adjust_data_to_north(0,0,0, horizontal);
            adjust_data_to_north(0, up_down, 0,0);
            current_x -= horizontal;
        } else if (robot_direction == ROBOT_DIRECTION_EAST) {
            adjust_data_to_north(up_down, 0, 0, 0);
            adjust_data_to_north(0,0,horizontal, 0);
            adjust_data_to_north(0, up_down, 0,0);
            current_x += horizontal;
        }
    }

    /**
     * This function will move the robot under the circle if there is no place
     * where it can pass when the robot encounters the circle.
     */
    public void bottom_circulation (int[] circle) {
        int up_down = circle[2] + robot_height/2;
        int horizontal = circle[2]*2 + robot_width;
        if(robot_direction  == ROBOT_DIRECTION_WEST) {
            adjust_data_to_north(0, up_down, 0, 0);
            adjust_data_to_north(0,0,0, horizontal);
            adjust_data_to_north(up_down, 0, 0,0);
            current_x -= horizontal;
        } else if (robot_direction == ROBOT_DIRECTION_EAST) {
            adjust_data_to_north(0, up_down, 0, 0);
            adjust_data_to_north(0,0,horizontal, 0);
            adjust_data_to_north(up_down, 0, 0,0);
            current_x += horizontal;
        }
    }

    /**
     * This function turns the robot down when it reaches the left or right end of the board
     */
    public void make_u_turn_to_bottom () {
        adjust_data_to_north(0, robot_height, 0, 0);
        current_y += robot_height;
        adjust_data_to_north(0,0, 0, robot_width);
        current_x -= robot_height;
    }

    /**
     * This function :
     * 1- Convert to cm from x and y coordinates
     * 2- Code that flips directions as if it's just north to avoid confusion
     */
    public void adjust_data_to_north (int forward, int backward, int right, int left) {
        /**
         * Convert to cm from x and y coordinates
         */
        int digital_white_board_width = whiteboard_right - whiteboard_left;
        int digital_white_board_height = whiteboard_bottom - whiteboard_top;
        if (forward != 0) {
            forward = (forward * whiteboard_height) / digital_white_board_height;
        }
        if (backward != 0) {
            backward = (backward * whiteboard_height) / digital_white_board_height;
        }
        if (right != 0) {
            right = (right * whiteboard_width) / digital_white_board_width;
        }
        if (left != 0) {
            left = (left * whiteboard_width) / digital_white_board_width;
        }
        /**
         * Code that flips directions as if it's just north to avoid confusion
         */
        int temp_forward = forward;
        int temp_backward = backward;
        int temp_right = right;
        int temp_left = left;
        switch (robot_direction) {
            case ROBOT_DIRECTION_NORTH:
                set_robot_direction (temp_forward, temp_backward, temp_right, temp_left);
                prepare_json_file(temp_forward, temp_backward, temp_right, temp_left);
                break;
            case ROBOT_DIRECTION_SOUTH:
                set_robot_direction (temp_backward, temp_forward, temp_right, temp_left);
                prepare_json_file(temp_backward, temp_forward, temp_right, temp_left);
                break;
            case ROBOT_DIRECTION_WEST:
                set_robot_direction (temp_right, temp_left, temp_forward, temp_backward);
                prepare_json_file(temp_right, temp_left, temp_forward, temp_backward);
                break;
            case ROBOT_DIRECTION_EAST:
                set_robot_direction (temp_left, temp_right, temp_backward, temp_forward);
                prepare_json_file(temp_left, temp_right, temp_backward, temp_forward);
                break;
            case ROBOT_DIRECTION_NORTHWEST:
                set_robot_direction (0, 0, -1, 0);
                prepare_json_file(0, 0, -1, 0);
                set_robot_direction (temp_forward, temp_backward, temp_right, temp_left);
                prepare_json_file(temp_forward, temp_backward, temp_right, temp_left);
                break;
            case ROBOT_DIRECTION_NORTHEAST:
                set_robot_direction (0, 0, 0, -1);
                prepare_json_file(0, 0, 0, -1);
                set_robot_direction (temp_forward, temp_backward, temp_right, temp_left);
                prepare_json_file(temp_forward, temp_backward, temp_right, temp_left);
                break;
            case ROBOT_DIRECTION_SOUTHWEST:
                set_robot_direction (0, 0, 0, -1);
                prepare_json_file(0, 0, 0, -1);
                set_robot_direction (temp_backward, temp_forward, temp_right, temp_left);
                prepare_json_file(temp_backward, temp_forward, temp_right, temp_left);
                break;
            case ROBOT_DIRECTION_SOUTHEAST:
                set_robot_direction (0, 0, -1, 0);
                prepare_json_file(0, 0, -1, 0);
                set_robot_direction (temp_backward, temp_forward, temp_right, temp_left);
                prepare_json_file(temp_backward, temp_forward, temp_right, temp_left);
                break;

        }
    }

    /**
     * This function saves the robot's motion to the json file and adds this json file to
     * the preparedJSONS linkedList
     */
    public void prepare_json_file(int forward, int backward, int right, int left) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject.put("F", forward);
            jsonObject.put("B", backward);
            jsonObject.put("R", right);
            jsonObject.put("L", left);
            if (forward > 0) {
                current_y += forward;
            } else if (backward > 0) {
                current_y -= backward;
            } else if (right > 0) {
                current_x += right;
            } else if (left > 0) {
                current_x -= left;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        preparedJSONS.add(jsonObject);
        prepare_reverse_json_file(forward, backward, right, left);
    }

    /**
     * This function saves the robot's motion REVERSE to the json file and adds this json file to
     * the reversePreparedJSONS linkedList  for return to starting point.
     * -> call in prepare_json_file function
     */
    public void prepare_reverse_json_file(int forward, int backward, int right, int left) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject.put("F", backward);
            jsonObject.put("B", forward);
            jsonObject.put("R", left);
            jsonObject.put("L", right);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        reversePreparedJSONS.add(jsonObject);
    }

    /**
     * This function combine preparedJSONS and reversePreparedJSONS for send robot with bluetooth
     */
    public void combine_json_files() {
        while (!reversePreparedJSONS.isEmpty()) {
            preparedJSONS.add(reversePreparedJSONS.getLast());
            reversePreparedJSONS.removeLast();
        }
    }

    /**
     * This function change robot direction on each motion.
     */
    public void set_robot_direction (int f, int b, int r, int l) {
        switch (robot_direction) {
            case ROBOT_DIRECTION_NORTH:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                }
                break;
            case ROBOT_DIRECTION_SOUTH:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                }
                break;
            case ROBOT_DIRECTION_WEST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                }
                break;
            case ROBOT_DIRECTION_EAST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                }
                break;
            case ROBOT_DIRECTION_NORTHWEST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                }
                break;
            case ROBOT_DIRECTION_NORTHEAST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_NORTH;
                }
                break;
            case ROBOT_DIRECTION_SOUTHWEST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHWEST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_WEST;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                }
                break;
            case ROBOT_DIRECTION_SOUTHEAST:
                if (f > 0 && r == 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f == 0 && r == 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHEAST;
                } else if (f == 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTHWEST;
                } else if (f == 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_NORTHEAST;
                } else if (f > 0 && r > 0 && l == 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f > 0 && r == 0 && l > 0 && b == 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                } else if (f == 0 && r > 0 && l == 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_SOUTH;
                } else if (f == 0 && r == 0 && l > 0 && b > 0) {
                    robot_direction = ROBOT_DIRECTION_EAST;
                }
                break;
        }
    }

    public void double_to_int () {
        for (int i = 0; i < circles_double.size(); i++) {
            double[] temp = circles_double.get(i);
            int first = (int) temp[0];
            int second = (int) temp[1];
            int third = (int) temp[2];
            int[] temp_int = new int[3];
            temp_int[0] = first - whiteboard_left; // center point x of circle
            temp_int[1] = second - whiteboard_top; // center point y of circle
            temp_int[2] = third; // radius of circle
            circles.add(temp_int);
        }
    }

    /**
     * This function Find closest circle to robot.
     */
    public int[] min_x_circle (){
        int temp_min_index = 0;
        int temp_min_x = circles.get(0)[0];
        for (int i = 0; i < circles.size(); i++) {
            if(circles.get(0)[i] < temp_min_x) {
                temp_min_index = i;
                temp_min_x = circles.get(0)[i];
            }
        }
        return circles.get(temp_min_index);
    }
}
