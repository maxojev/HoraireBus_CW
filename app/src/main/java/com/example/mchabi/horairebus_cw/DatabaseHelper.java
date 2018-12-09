package com.example.mchabi.horairebus_cw;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;

import com.example.mchabi.horairebus_cw.BusRoute;
import com.example.mchabi.horairebus_cw.Constants;
import com.example.mchabi.horairebus_cw.StarContract;
import com.example.mchabi.horairebus_cw.Trip;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



import static android.os.Environment.getExternalStorageDirectory;

public class DatabaseHelper extends SQLiteOpenHelper implements StarContract {

    private static File DEVICE_ROOT_FOLDER = getExternalStorageDirectory();
    public SQLiteDatabase database;
    private static String DATA_NAME = "starBusDb";
    private static final int DATA_BASE_VERSION = 1;
    /**
     * Paths used to load csv files
     */
    public static String INIT_FOLDER_PATH = "star1dm/"; // From the root folder of the device
    public static String DOWNLOAD_PATH = "GTFS_2017.4.0.3_2017-12-25_2018-01-07";

    private static String CALENDAR_CSV_FILE = "calendar.txt";
    private static String BUS_ROUTES_CSV_FILE = "routes.txt";
    private static String STOPS_CSV_FILE = "stops.txt";
    private static String STOP_TIMES_CSV_FILE = "stop_times.txt";
    private static final String STOP_TIMES_SPLIT_CSV_FILE = "stop_times_";
    private static String TRIPS_CSV_FILE = "trips.txt";

    public DatabaseHelper(Context context) {
        super(context, DATA_NAME, null, DATA_BASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Constants.CREATE_BUS_ROUTE_TABLE);
        db.execSQL(Constants.CREATE_CALENDAR_TABLE);
        db.execSQL(Constants.CREATE_STOP_TIMES_TABLE);
        db.execSQL(Constants.CREATE_STOPS_TABLE);
        db.execSQL(Constants.CREATE_TRIPS_TABLE);
        db.execSQL(Constants.CREATE_VERSIONS_TABLE);
        /*
          Initialisation des versions
         */
        ContentValues values = new ContentValues();
        ContentValues values2 = new ContentValues();
        values.put(Constants.VERSIONS_FILE_NAME_COL, "file1");
        values2.put(Constants.VERSIONS_FILE_NAME_COL, "file2");
        values.put(Constants.VERSIONS_FILE_VERSION_COL, Constants.DEFAULT_FIRST_VERSION);
        values2.put(Constants.VERSIONS_FILE_VERSION_COL, Constants.DEFAULT_FIRST_VERSION);
        db.insert(Constants.VERSIONS_TABLE, null, values);
        db.insert(Constants.VERSIONS_TABLE, null, values2);
        Log.d("STARX", "db cretaed");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + BusRoutes.CONTENT_PATH);
        db.execSQL("DROP TABLE IF EXISTS " + Trips.CONTENT_PATH);
        db.execSQL("DROP TABLE IF EXISTS " + Stops.CONTENT_PATH);
        db.execSQL("DROP TABLE IF EXISTS " + StopTimes.CONTENT_PATH);
        db.execSQL("DROP TABLE IF EXISTS " + Calendar.CONTENT_PATH);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.VERSIONS_TABLE);
        onCreate(db);
    }

    public List<String> allTableNames() {
        List<String> result = new ArrayList<>();
        String selectQuery = "select name from sqlite_master where type = 'table'";
        Cursor cursor = this.getReadableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                result.add(name);
            } while (cursor.moveToNext());
        }
        return result;
    }

    /**
     * Insert toutes les données disponibles dans le dossier
     */
    public void insertAll() {
        //insertStopTimes();
        //insertCalendars();
        insertBusRoutes();
        //insertStops();
        //insertTrips();

        /**
         * Supprime les données après leurs insertion
         */
        try {
            deleteDirectory(new File(DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert a BusRoute in the db
     *
     * @param route
     */
    public void insertRoute(BusRoute route) {
        database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BusRoutes.BusRouteColumns.ROUTE_ID, route.getRoute_id());
        values.put(BusRoutes.BusRouteColumns.SHORT_NAME, route.getShortName());
        values.put(BusRoutes.BusRouteColumns.LONG_NAME, route.getLongName());
        values.put(BusRoutes.BusRouteColumns.DESCRIPTION, route.getDescription());
        values.put(BusRoutes.BusRouteColumns.TYPE, route.getType());
        values.put(BusRoutes.BusRouteColumns.COLOR, route.getColor());
        values.put(BusRoutes.BusRouteColumns.TEXT_COLOR, route.getTextColor());
        database.insert(BusRoutes.CONTENT_PATH, null, values);
    }

    /**
     * Insert all BusRoute from the csv file to the db
     */
    public void insertBusRoutes() {
        ArrayList<BusRoute> items = new ArrayList<BusRoute>();
        database = this.getWritableDatabase();
        try {
            items = loadBusRoutesData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database.beginTransaction();
            String sql = "INSERT INTO " + StarContract.BusRoutes.CONTENT_PATH + " (" +
                    BusRoutes.BusRouteColumns.ROUTE_ID + "," +
                    BusRoutes.BusRouteColumns.SHORT_NAME + "," +
                    BusRoutes.BusRouteColumns.LONG_NAME + "," +
                    BusRoutes.BusRouteColumns.DESCRIPTION + "," +
                    BusRoutes.BusRouteColumns.TYPE + "," +
                    BusRoutes.BusRouteColumns.COLOR + "," +
                    BusRoutes.BusRouteColumns.TEXT_COLOR +
                    ") VALUES (?,?,?,?,?,?,?)";
            SQLiteStatement insert = database.compileStatement(sql);
            for (int i = 0; i < items.size(); i++) {
                BusRoute item = items.get(i);
                insert.bindString(1, item.getRoute_id());
                insert.bindString(2, item.getShortName());
                insert.bindString(3, item.getLongName());
                insert.bindString(4, item.getDescription());
                insert.bindString(5, item.getType());
                insert.bindString(6, item.getColor());
                insert.bindString(7, item.getTextColor());
                Log.d("STARX", i + " -- BusRoute added");
                insert.execute();
            }
            database.setTransactionSuccessful();
            Log.d("STARX", items.size() + " BusRoute added ----------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    public void insertTrip(Trip trip) {
        database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Trips.TripColumns.TRIP_ID, trip.getTripId());
        values.put(Trips.TripColumns.ROUTE_ID, trip.getRouteId());
        values.put(Trips.TripColumns.SERVICE_ID, trip.getServiceId());
        values.put(Trips.TripColumns.HEADSIGN, trip.getHeadSign());
        values.put(Trips.TripColumns.DIRECTION_ID, trip.getDirectionId());
        values.put(Trips.TripColumns.BLOCK_ID, trip.getBlockId());
        values.put(Trips.TripColumns.WHEELCHAIR_ACCESSIBLE, trip.getWheelchairAccessible());
        database.insert(Trips.CONTENT_PATH, null, values);
    }

    /**
     * Insert all Trip from the csv file to the db
     */
    public void insertTrips() {
        ArrayList<Trip> items = new ArrayList<Trip>();
        try {
            items = loadTripsData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database = this.getWritableDatabase();
            database.beginTransaction();
            String sql = "INSERT INTO " + StarContract.Trips.CONTENT_PATH + " (" +
                    Trips.TripColumns.TRIP_ID+ "," +
                    Trips.TripColumns.ROUTE_ID+ "," +
                    Trips.TripColumns.SERVICE_ID+ "," +
                    Trips.TripColumns.HEADSIGN+ "," +
                    Trips.TripColumns.DIRECTION_ID+ "," +
                    Trips.TripColumns.BLOCK_ID+ "," +
                    Trips.TripColumns.WHEELCHAIR_ACCESSIBLE+
                    ") VALUES (?,?,?,?,?,?,?)";
            SQLiteStatement insert = database.compileStatement(sql);
            for (int i = 0; i < items.size(); i++) {
                Trip item = items.get(i);
                insert.bindLong(1, item.getTripId());
                insert.bindLong(2, item.getRouteId());
                insert.bindLong(3, item.getServiceId());
                insert.bindString(4, item.getHeadSign());
                insert.bindLong(5, item.getDirectionId());
                insert.bindString(6, item.getBlockId());
                insert.bindString(7, item.getWheelchairAccessible());
                Log.d("STARX", i + " -- Trips added");
                insert.execute();
            }
            database.setTransactionSuccessful();
            Log.d("STARX", items.size() + " Calendar added ----------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Insert a Stop in the db
     *
     * @param stop
     */
    public void insertStop(Stop stop) {
        database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Stops.StopColumns.STOP_ID, stop.getId());
        values.put(Stops.StopColumns.NAME, stop.getName());
        values.put(Stops.StopColumns.DESCRIPTION, stop.getDescription());
        values.put(Stops.StopColumns.LATITUDE, stop.getLatitude());
        values.put(Stops.StopColumns.LONGITUDE, stop.getLongitude());
        values.put(Stops.StopColumns.WHEELCHAIR_BOARDING, stop.getWheelChairBoalding());
        database.insert(Stops.CONTENT_PATH, null, values);
        Log.e("XXXX", "insertion OK");
    }

    /**
     * Insert all Stops from the csv file to the db
     */
    public void insertStops() {
        ArrayList<Stop> items = new ArrayList<Stop>();
        try {
            items = loadStopsData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database = this.getWritableDatabase();
            database.beginTransaction();
            String sql = "INSERT INTO " + StarContract.Stops.CONTENT_PATH + " (" +
                    Stops.StopColumns.STOP_ID + "," +
                    Stops.StopColumns.NAME + "," +
                    Stops.StopColumns.DESCRIPTION + "," +
                    Stops.StopColumns.LATITUDE + "," +
                    Stops.StopColumns.LONGITUDE + "," +
                    Stops.StopColumns.WHEELCHAIR_BOARDING +
                    ") VALUES (?,?,?,?,?,?)";
            SQLiteStatement insert = database.compileStatement(sql);
            for (int i = 0; i < items.size(); i++) {
                Stop item = items.get(i);
                insert.bindString(1, item.getId());
                insert.bindString(2, item.getName());
                insert.bindString(3, item.getDescription());
                insert.bindLong(4, (long) item.getLatitude());
                insert.bindLong(5, (long) item.getLongitude());
                insert.bindString(6, item.getWheelChairBoalding());
                Log.d("STARX", i + " -- Stops added");
                insert.execute();
            }
            database.setTransactionSuccessful();
            Log.d("STARX", items.size() + " Stops added ----------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Insert a StopTime in the db
     *
     * @param stopTime
     */
    public void insertStopTime(StopTime stopTime) {
        ContentValues values = new ContentValues();
        values.put(StopTimes.StopTimeColumns.TRIP_ID, stopTime.getTripId());
        values.put(StopTimes.StopTimeColumns.ARRIVAL_TIME, stopTime.getArrivalTime());
        values.put(StopTimes.StopTimeColumns.DEPARTURE_TIME, stopTime.getDepartureTme());
        values.put(StopTimes.StopTimeColumns.STOP_ID, stopTime.getStopId());
        values.put(StopTimes.StopTimeColumns.STOP_SEQUENCE, stopTime.getStopSequence());
        database.insert(StopTimes.CONTENT_PATH, null, values);
    }

    public void insertStopTimes() {
        database = this.getWritableDatabase();
        ArrayList<StopTime> stopTimes = new ArrayList<StopTime>();
        try {
            stopTimes = loadStopTimesData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database.beginTransaction();
            String sql = "INSERT INTO " + StopTimes.CONTENT_PATH + " (" +
                    StopTimes.StopTimeColumns.TRIP_ID + "," +
                    StopTimes.StopTimeColumns.ARRIVAL_TIME + "," +
                    StopTimes.StopTimeColumns.DEPARTURE_TIME + "," +
                    StopTimes.StopTimeColumns.STOP_ID + "," +
                    StopTimes.StopTimeColumns.STOP_SEQUENCE +
                    ") VALUES (?,?,?,?,?)";
            SQLiteStatement insert = database.compileStatement(sql);
            for (int i = 0; i < stopTimes.size(); i++) {
                StopTime item = stopTimes.get(i);
                insert.bindLong(1, item.getTripId());
                insert.bindString(2, item.getArrivalTime());
                insert.bindString(3, item.getDepartureTme());
                insert.bindLong(4, item.getStopId());
                insert.bindString(5, item.getStopSequence());
                Log.d("STARX", i + " -- stoptime added");
                insert.execute();
            }
            database.setTransactionSuccessful();
            Log.d("STARX", stopTimes.size() + " stoptimes added ----------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Insert a Calendar in the db
     *
     * @param calendar
     */
    /*
    public void insertCalendar(Calendar calendar) {
        database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Calendar.CalendarColumns.SERVICE_ID, calendar.getService_id());
        values.put(Calendar.CalendarColumns.MONDAY, calendar.getMonday());
        values.put(Calendar.CalendarColumns.TUESDAY, calendar.getTuesday());
        values.put(Calendar.CalendarColumns.WEDNESDAY, calendar.getWednesday());
        values.put(Calendar.CalendarColumns.THURSDAY, calendar.getThursday());
        values.put(Calendar.CalendarColumns.FRIDAY, calendar.getFriday());
        values.put(Calendar.CalendarColumns.SATURDAY, calendar.getSaturday());
        values.put(Calendar.CalendarColumns.SUNDAY, calendar.getSunday());
        values.put(Calendar.CalendarColumns.START_DATE, calendar.getStartDate());
        values.put(Calendar.CalendarColumns.END_DATE, calendar.getEndDate());
        database.insert(Calendar.CONTENT_PATH, null, values);
    }

    /**
     * Insert all Calendars from the csv file to the db
     */
    /*public void insertCalendars() {
        ArrayList<Calendar> items = new ArrayList<Calendar>();
        try {
            items = loadCalendarData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database = this.getWritableDatabase();
            database.beginTransaction();
            String sql = "INSERT INTO " + StarContract.Calendar.CONTENT_PATH + " (" +
                    Calendar.CalendarColumns.SERVICE_ID + "," +
                    Calendar.CalendarColumns.MONDAY + "," +
                    Calendar.CalendarColumns.TUESDAY + "," +
                    Calendar.CalendarColumns.WEDNESDAY + "," +
                    Calendar.CalendarColumns.THURSDAY + "," +
                    Calendar.CalendarColumns.FRIDAY + "," +
                    Calendar.CalendarColumns.SATURDAY + "," +
                    Calendar.CalendarColumns.SUNDAY + "," +
                    Calendar.CalendarColumns.START_DATE + "," +
                    Calendar.CalendarColumns.END_DATE +
                    ") VALUES (?,?,?,?,?,?,?,?,?,?)";
            SQLiteStatement insert = database.compileStatement(sql);
            for (int i = 0; i < items.size(); i++) {
                Calendar item = items.get(i);
                insert.bindLong(1, item.getService_id());
                insert.bindString(2, item.getMonday());
                insert.bindString(3, item.getTuesday());
                insert.bindString(4, item.getWednesday());
                insert.bindString(5, item.getThursday());
                insert.bindString(6, item.getFriday());
                insert.bindString(7, item.getSaturday());
                insert.bindString(8, item.getSunday());
                insert.bindString(9, item.getStartDate());
                insert.bindString(10, item.getEndDate());
                Log.d("STARX", i + " -- Calendar added");
                insert.execute();
            }
            database.setTransactionSuccessful();
            Log.d("STARX", items.size() + " Calendar added ----------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Loads Calendars from the csv file
     *
     * @return
     * @throws IOException
     */
    /*
    public static ArrayList<Calendar> loadCalendarData() throws IOException {
        Log.d("STARXC", "start loading... " + DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + CALENDAR_CSV_FILE);
        ArrayList<Calendar> calendars = new ArrayList<>();
        FileReader file = new FileReader(new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH  + CALENDAR_CSV_FILE));
        BufferedReader buffer = new BufferedReader(file);
        String line = "";
        int i = 0;
        while ((line = buffer.readLine()) != null) {
            if (i != 0) {
                String[] str = line.split(",");
                Calendar calendar = new Calendar(
                        Integer.valueOf(str[0].replaceAll("\"", "")),
                        str[1].replaceAll("\"", ""),
                        str[2].replaceAll("\"", ""),
                        str[3].replaceAll("\"", ""),
                        str[4].replaceAll("\"", ""),
                        str[5].replaceAll("\"", ""),
                        str[6].replaceAll("\"", ""),
                        str[7].replaceAll("\"", ""),
                        str[8].replaceAll("\"", ""),
                        str[9].replaceAll("\"", ""));
                calendars.add(calendar);
                Log.d("STARXC", i + "-loaded... " + calendar.toString());
            }
            i++;
        }
        buffer.close();
        file.close();
        Log.d("STARXC", i + " calendars loaded");
        return calendars;
    }
*/
    /**
     * Loads BusRoutes from the csv file
     *
     * @return
     * @throws IOException
     */
    public static ArrayList<BusRoute> loadBusRoutesData() throws IOException {
        Log.d("STARXC", "start loading... " + DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + BUS_ROUTES_CSV_FILE);
        ArrayList<BusRoute> busRoutes = new ArrayList<>();
        FileReader file = new FileReader(new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH + BUS_ROUTES_CSV_FILE));
        BufferedReader buffer = new BufferedReader(file);
        String line = "";
        int i = 0;
        while ((line = buffer.readLine()) != null) {
            if (i != 0) {
                String[] str = line.split(",");
                BusRoute br = new BusRoute(
                        str[0].replaceAll("\"", ""),
                        str[2].replaceAll("\"", ""),
                        str[3].replaceAll("\"", ""),
                        str[4].replaceAll("\"", ""),
                        str[5].replaceAll("\"", ""),
                        str[7].replaceAll("\"", ""),
                        str[8].replaceAll("\"", ""));
                busRoutes.add(br);
                Log.d("STARXBR", i + "-loaded... " + br.toString());
            }
            i++;
        }
        buffer.close();
        file.close();
        Log.d("STARXBR", i + " busRoutes loaded");
        return busRoutes;
    }

    /**
     * Loads Stops from the csv file
     *
     * @return
     * @throws IOException
     */
    public static ArrayList<Stop> loadStopsData() throws IOException {
        Log.d("STARXC", "start loading... " + DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + STOPS_CSV_FILE);
        ArrayList<Stop> stops = new ArrayList<>();
        FileReader file = new FileReader(new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH+ STOPS_CSV_FILE));
        Log.e("STARXS", " Absolut Path for file" + file.toString());
        BufferedReader buffer = new BufferedReader(file);
        String line = "";
        int i = 0;
        while ((line = buffer.readLine()) != null) {
            if (i != 0) {
                String[] str = line.split(",");
                String id = str[0].replaceAll("\"", "");
                String name = str[2].replaceAll("\"", "");
                String description = str[3].replaceAll("\"", "");
                float latitude = Float.valueOf(str[4].replace('"', ' '));
                float longitude = Float.valueOf(str[5].replace('"', ' '));
                String wheelChairBoalding = str[11].replaceAll("\"", "");
                Stop stop = new Stop(id, name, description, latitude, longitude, wheelChairBoalding);
                stops.add(stop);
                Log.d("STARXS", i + "-loaded... " + stop.toString());
            }
            i++;
        }
        buffer.close();
        file.close();
        Log.d("STARXS", i + " stops loaded");
        return stops;
    }

    /**
     * Loads StopTimes from the csv file
     *
     * @return
     * @throws IOException
     */
    public ArrayList<StopTime> loadStopTimesData() throws IOException {
        Log.d("STARXC", "start loading... " + DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + STOP_TIMES_CSV_FILE);
        ArrayList<StopTime> fileStopTimes = new ArrayList<>();
        long items = 0;
        long allStoptimes = 0;
        String currfile = INIT_FOLDER_PATH + STOP_TIMES_CSV_FILE;
        Log.d("STARX", "current file ..: " + currfile);
        FileReader file = new FileReader(new File(DEVICE_ROOT_FOLDER, currfile));
        BufferedReader buffer = new BufferedReader(file);
        String line = "";
        long nb = 0;
        while ((line = buffer.readLine()) != null) {
            if (nb != 0) {
                String[] str = line.split(",");
                int tripId = Integer.valueOf(str[0].replaceAll("\"", ""));
                int stopId = Integer.valueOf(str[3].replaceAll("\"", ""));
                StopTime stopTime = new StopTime(
                        tripId, str[1].replaceAll("\"", ""),
                        str[2].replaceAll("\"", ""),
                        stopId,
                        str[4].replaceAll("\"", ""));
                fileStopTimes.add(stopTime);
                Log.d("STARXST", items + "-loaded... " + stopTime.toString());
            }
            items++;
            nb++;
        }
        buffer.close();
        file.close();
        allStoptimes += items;
        Log.d("STARXST", items + " stopTimes loaded");
        return fileStopTimes;
    }

    public static int splitStopTimesFile() {
        int nb_of_files = 0;
        String headerLine = "";

        try {
            // Reading file and getting no. of files to be generated
//            String inputfile = "C:/test.txt"; //  Source File Name.
            double lines_per_file = 50000.0; //  No. of lines to be split and saved in each output file.
            File file = new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + STOP_TIMES_CSV_FILE);
            Scanner scanner = new Scanner(file);
            int count = 0;
            while (scanner.hasNextLine()) {
                if (count == 0) {
                    headerLine = scanner.nextLine();
                } else {
                    scanner.nextLine();
                }
                count++;
            }
            System.out.println("Lines in the StopTimes file: " + count);     // Displays no. of lines in the input file.

            double temp = (count / lines_per_file);
            int temp1 = (int) temp;
            if (temp1 == temp) {
                nb_of_files = temp1;
            } else {
                nb_of_files = temp1 + 1;
            }
            System.out.println("No. of files to be generated :" + nb_of_files); // Displays no. of files to be generated.

            //---------------------------------------------------------------------------------------------------------

            // Actual splitting of file into smaller files

//            FileInputStream fstream = new FileInputStream(inputfile);
            FileInputStream fstream = new FileInputStream(new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + STOP_TIMES_CSV_FILE));
            DataInputStream in = new DataInputStream(fstream);

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            for (int j = 1; j <= nb_of_files; j++) {
                FileWriter fstream1 = new FileWriter(DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + STOP_TIMES_SPLIT_CSV_FILE + j + ".txt");     // Destination File Location
                BufferedWriter out = new BufferedWriter(fstream1);
                if (j != 1) {
                    out.write(headerLine);
                    out.newLine();
                }
                for (int i = 1; i <= lines_per_file; i++) {
                    strLine = br.readLine();
                    if (strLine != null) {
                        out.write(strLine);
                        if (i != lines_per_file) {
                            out.newLine();
                        }
                    }
                }
                out.close();
            }

            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return nb_of_files;
    }

    /**
     * Loads Trip from the csv file
     *
     * @return
     * @throws IOException
     */
    public static ArrayList<Trip> loadTripsData() throws IOException {
        Log.d("STARXC", "start loading... " + DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH + DOWNLOAD_PATH + "/" + TRIPS_CSV_FILE);
        ArrayList<Trip> trips = new ArrayList<>();
        FileReader file = new FileReader(new File(DEVICE_ROOT_FOLDER, INIT_FOLDER_PATH +TRIPS_CSV_FILE));
        BufferedReader buffer = new BufferedReader(file);
        String line = "";
        int i = 0;
        while ((line = buffer.readLine()) != null) {
            if (i != 0) {
                String[] str = line.split(",");
                int routeId = Integer.valueOf(str[0].replaceAll("\"", ""));
                int tripId = Integer.valueOf(str[2].replaceAll("\"", ""));
                int serviceId = Integer.valueOf(str[1].replaceAll("\"", ""));
                int direction = Integer.valueOf(str[5].replaceAll("\"", ""));
                String block = str[6].replaceAll("\"", "");
                Trip trip = new Trip(tripId,
                        routeId,
                        serviceId,
                        str[3].replaceAll("\"", ""),
                        direction,
                        block,
                        str[8].replaceAll("\"", ""));
                trips.add(trip);
                Log.d("STARXT", i + "-loaded... " + trip.toString());
            }
            i++;
        }
        buffer.close();
        file.close();
        Log.d("STARXT", i + " trips loaded");
        return trips;
    }

    /**
     * Load BusRoutes from database
     *
     * @return
     */
    public ArrayList<BusRoute> getBusRoutesFromDatabase() {
        String selectQuery = "SELECT  * FROM " + StarContract.BusRoutes.CONTENT_PATH;
        ArrayList<BusRoute> data = new ArrayList<>();
        Cursor cursor = this.getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                BusRoute item = new BusRoute(
                        cursor.getString(cursor.getColumnIndex(BusRoutes.BusRouteColumns.ROUTE_ID)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.SHORT_NAME)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.LONG_NAME)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.TYPE)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.COLOR)),
                        cursor.getString(cursor.getColumnIndex(StarContract.BusRoutes.BusRouteColumns.TEXT_COLOR))
                );
                data.add(item);
                Log.d("STARX", "load from db..." + item);
            } while (cursor.moveToNext());
            Log.d("STARX", "-----   " + data.size() + " BusRoutes loaded form database ");
        }
        cursor.close();
        return data;
    }

    /**
     * Load Calendars from database
     *
     * @return
     */
    /*
    public ArrayList<Calendar> getCalendarsFromDatabase() {
        String selectQuery = "SELECT  * FROM " + StarContract.Calendar.CONTENT_PATH;
        ArrayList<Calendar> data = new ArrayList<>();
        Cursor cursor = this.getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Calendar item = new Calendar(
                        cursor.getInt(cursor.getColumnIndex(Calendar.CalendarColumns.SERVICE_ID)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.MONDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.TUESDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.WEDNESDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.THURSDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.FRIDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.SATURDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.SUNDAY)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.START_DATE)),
                        cursor.getString(cursor.getColumnIndex(StarContract.Calendar.CalendarColumns.END_DATE))
                );
                data.add(item);
                Log.d("STARX", "load from db..." + item);
            } while (cursor.moveToNext());
            Log.d("STARX", "-----   " + data.size() + " calendars loaded form database");
        }
        cursor.close();
        return data;
    }
*/
    /**
     * Load Stops from database
     *
     * @return
     */
    public ArrayList<Stop> getStopsFromDatabase() {
        String selectQuery = "SELECT  * FROM " + StarContract.Stops.CONTENT_PATH;
        ArrayList<Stop> data = new ArrayList<>();
        Cursor cursor = this.getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Stop item = new Stop(
                        cursor.getString(cursor.getColumnIndex(Stops.StopColumns.STOP_ID)),
                        cursor.getString(cursor.getColumnIndex(Stops.StopColumns.NAME)),
                        cursor.getString(cursor.getColumnIndex(Stops.StopColumns.DESCRIPTION)),
                        cursor.getFloat(cursor.getColumnIndex(Stops.StopColumns.LATITUDE)),
                        cursor.getFloat(cursor.getColumnIndex(Stops.StopColumns.LONGITUDE)),
                        cursor.getString(cursor.getColumnIndex(Stops.StopColumns.WHEELCHAIR_BOARDING))
                );
                data.add(item);
                Log.d("STARX", "load from database..." + item);
            } while (cursor.moveToNext());
            Log.d("STARX", "-----   " + data.size() + " Stops loaded form database ");
        }
        cursor.close();
        return data;
    }

    /**
     * Load StopTimes from database
     *
     * @return
     */
    public ArrayList<StopTime> getStopTimesFromDatabase() {
        String selectQuery = "SELECT  * FROM " + StarContract.StopTimes.CONTENT_PATH;
        ArrayList<StopTime> data = new ArrayList<>();
        Cursor cursor = this.getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                StopTime item = new StopTime(
                        cursor.getInt(cursor.getColumnIndex(StarContract.StopTimes.StopTimeColumns.TRIP_ID)),
                        cursor.getString(cursor.getColumnIndex(StarContract.StopTimes.StopTimeColumns.ARRIVAL_TIME)),
                        cursor.getString(cursor.getColumnIndex(StarContract.StopTimes.StopTimeColumns.DEPARTURE_TIME)),
                        cursor.getInt(cursor.getColumnIndex(StarContract.StopTimes.StopTimeColumns.STOP_ID)),
                        cursor.getString(cursor.getColumnIndex(StarContract.StopTimes.StopTimeColumns.STOP_SEQUENCE))
                );
                data.add(item);
                Log.d("STARX", "load from database..." + item);
            } while (cursor.moveToNext());
            Log.d("STARX", "-----   " + data.size() + " StopTimes loaded form database");
        }
        cursor.close();
        return data;
    }

    /**
     * Load Trip from database
     *
     * @return
     */
    public ArrayList<Trip> getTripsFromDatabase() {
        Log.d("STARX", "loading trips from database...");
        String selectQuery = "SELECT  * FROM " + StarContract.Trips.CONTENT_PATH;
        ArrayList<Trip> data = new ArrayList<>();
        Cursor cursor = this.getWritableDatabase().rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Trip item = new Trip(
                        cursor.getInt(cursor.getColumnIndex(Trips.TripColumns.TRIP_ID)),
                        cursor.getInt(cursor.getColumnIndex(Trips.TripColumns.ROUTE_ID)),
                        cursor.getInt(cursor.getColumnIndex(Trips.TripColumns.SERVICE_ID)),
                        cursor.getString(cursor.getColumnIndex(Trips.TripColumns.HEADSIGN)),
                        cursor.getInt(cursor.getColumnIndex(Trips.TripColumns.DIRECTION_ID)),
                        cursor.getString(cursor.getColumnIndex(Trips.TripColumns.BLOCK_ID)),
                        cursor.getString(cursor.getColumnIndex(Trips.TripColumns.WHEELCHAIR_ACCESSIBLE))
                );
                data.add(item);
                Log.d("STARX", "load from database..." + item);
            } while (cursor.moveToNext());
            Log.d("STARX", "-----   " + data.size() + " Trip loaded form database ");
        }
        cursor.close();
        return data;
    }

    public static ArrayList<String> getVersions(Context context) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        String selectQuery = "SELECT  * FROM " + Constants.VERSIONS_TABLE;
        ArrayList<String> versions = new ArrayList<>();
        try {
            Cursor cursor = databaseHelper.getWritableDatabase().rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                do {
                    versions.add(cursor.getString(cursor.getColumnIndex(Constants.VERSIONS_FILE_VERSION_COL)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            versions.add(Constants.DEFAULT_FIRST_VERSION);
            e.printStackTrace();
        } finally {
            versions.add(Constants.DEFAULT_FIRST_VERSION);
        }
        return versions;
    }

    /*
    public void updateVersions(Context context, Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(context.getResources().getString(R.string.data1_url_id))
                    && bundle.containsKey(context.getResources().getString(R.string.data2_url_id))
                    && bundle.containsKey(context.getResources().getString(R.string.data1_date_id))
                    && bundle.containsKey(context.getResources().getString(R.string.data2_date_id))
                    ) {
                String msg1 = bundle.getString(context.getResources().getString(R.string.data1_url_id));
                String msg2 = bundle.getString(context.getResources().getString(R.string.data2_url_id));
                String date1 = bundle.getString(context.getResources().getString(R.string.data1_date_id));
                String date2 = bundle.getString(context.getResources().getString(R.string.data2_date_id));
                database = this.getWritableDatabase();
                database.execSQL("DROP TABLE IF EXISTS " + Constants.VERSIONS_TABLE);
                ContentValues values = new ContentValues();
                ContentValues values2 = new ContentValues();
                values.put(Constants.VERSIONS_FILE_NAME_COL, msg1);
                values2.put(Constants.VERSIONS_FILE_NAME_COL, msg2);
                values.put(Constants.VERSIONS_FILE_VERSION_COL, date1);
                values2.put(Constants.VERSIONS_FILE_VERSION_COL, date2);
                database.execSQL(Constants.CREATE_VERSIONS_TABLE);
                database.insert(Constants.VERSIONS_TABLE, null, values);
                database.insert(Constants.VERSIONS_TABLE, null, values2);
            }
        }
    }
*/

    /**
     * Supprimer un dossier
     * @param file
     * @throws IOException
     */
    private static void deleteDirectory(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                deleteDirectory(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }
}