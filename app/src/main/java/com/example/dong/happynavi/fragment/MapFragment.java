package com.example.dong.happynavi.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.amap.api.maps.model.TileProvider;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.example.dong.happynavi.R;
import com.example.dong.happynavi.activity.LoginActivity;
import com.example.dong.happynavi.activity.MainActivity;
import com.example.dong.happynavi.db.PointOfInterestDBHelper;
import com.example.dong.happynavi.db.TraceDBHelper;
import com.example.dong.happynavi.entity.GpsData;
import com.example.dong.happynavi.entity.PointOfInterestData;
import com.example.dong.happynavi.entity.StepData;
import com.example.dong.happynavi.entity.TraceData;
import com.example.dong.happynavi.helper.Common;
import com.example.dong.happynavi.helper.ShareToWeChat;
import com.example.dong.happynavi.service.DownloadService;
import com.example.dong.happynavi.service.GpsTrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static android.content.Context.MODE_PRIVATE;

/**
 * 地图
 */

public class MapFragment extends Fragment implements View.OnClickListener, LocationSource, AMapLocationListener,
        AMap.OnMapClickListener, AMap.OnMarkerClickListener, GeocodeSearch.OnGeocodeSearchListener {

    private View mappageLayout;

    private TextView text_step, text_location, text_calculate;
    private ImageButton    layer;
    private ImageButton    startTrail;
    private ImageButton    pauseTrail;
    private ImageButton    endTrail;
    private ImageButton    takephoto;
    private ImageButton    toolbox;
    private ImageButton    returnToMap;
    private ProgressDialog proDialog = null;

    private AMap            aMap;
    private MapView         mapView;
    private Marker          interestpoint;          //响应用户点击的位置
    private Marker          startMarker = null;            //起点标记
    private Marker          endMarker   = null;                //终点标记
    private Polyline        polyline    = null;                //轨迹连线
    private PolylineOptions options;        //轨迹线属性

    private Polyline        polyline2 = null;        //手动画线对象（2017-7-16）
    private PolylineOptions options2;        //手动画线设置对象

    private Polygon        polyGon = null;            //手动画多边形对象（2017-7-18）
    private PolygonOptions polyGonOptions;    //手动画多边形设置对象


    private OnLocationChangedListener mListener;

    private AMapLocationClient       mlocationClient;
    private AMapLocationClientOption mLocationOption;

    private GeocodeSearch geocoderSearch;
    private LatLonPoint   latLonPoint;             //用户点击的位置的经纬度
    private LatLng        finalLatLng = new LatLng(0, 0);  //轨迹终点
    private String        addressName;
    private LatLng        currentLatLng;                 //定位到的当前位置
    private AMapLocation  currentAlocation;
    private List<LatLng>  points;                 //轨迹点的成员

    private boolean isstart         = false;
    private boolean ispause         = false;
    private boolean isend           = true;
    private boolean bound_trace     = false;

    private boolean istimeset       = false;
    private boolean iscountstep     = false;
    private boolean isShowNonLocDlg = false; //无法定位对话框是否正在显示

    public static final int                      MENU_ROUTE         = 0;
    public static final int                      MENU_NAVI          = 1;
    private             int                      maptype            = 0;

    private             long                     traceNo            = 0;
    private             int                      total_step         = 0;   //走的总步数
    private             TraceData                tracedata          = new TraceData();
    private             StepData                 stepdata           = new StepData();
    private             List<GpsData>            tracegps           = new ArrayList<GpsData>();
    private             MyBroadcastReciver       myreveiver         = null;//用于接收后台发送的定位广播
    private             AccuracyBroadcastReciver accuracyReciver    = null;
    private             BroadcastReceiver        connectionReceiver = null; // 用于监听网络状态变化的广播

    private GpsTrace          traceService;
    //private CommentUploadService commentUploadService;
    private Intent            locService;
    private Intent            commentService;
    private Intent            stepService;
    private Intent            updateService;
    private Thread            stepThread;
    private TraceDBHelper     helper;
    private SharedPreferences sp;  //存储基本配置信息 如账号、密码
    private SharedPreferences uploadCache;//存储待上传的评论信息
    //private FileInfo fileInfo = null;//新版apk文件

    private final String              MY_ACTION          = "android.intent.action.LOCATION_RECEIVER";
    private final String              PULLREFRESH_ACTION = "android.intent.action.PULLREFRESH_RECEIVER";
    private final String              ACCURACY_ACTION    = "android.intent.action.ACCURACY_RECEIVER";
    //private static final String URL_STARTTRAIL=Common.url+"reqTraceNo.aspx";
    private       String              URL_ENDTRAIL       = null;
    //private static final String URL_GET4TIME=Common.url+"request.aspx";
    private       String              URL_CHECKUPDATE    = null;
    private       String              URL_GETPOI         = null;
    public final  int                 REQUSET_COMMENT    = 1;
    private       PointOfInterestData behaviourData, durationData, partnerNumData, relationData;
    private PointOfInterestDBHelper helper2     = null;
    private Cursor                  cursor;
    private int                     checkedItem = 0;
    private double                  currentLongitude, currentLatitude, currentAltitude, currentLongitude1, currentLatitude1, currentAltitude1;
    private String[] degreeLngArr, minuteLngArr, secondLngArr, degreeLatArr, minuteLatArr, secondLatArr;
    private String degreeLngStr, minuteLngStr, secondLngStr, degreeLatStr, minuteLatStr, secondLatStr, currentAltitudeStr;
    private double minuteLng1, secondLng1, minuteLat1, secondLat1;
    private PopupWindow mPopupWindow;

    private Marker         marker;                    //手动画线的标注点对象（2017-7-16）
    private MarkerOptions  markerOption;        //手动画线的标注点设置对象
    private List<Marker>   markList;            //手动画线的标注点对象集合（在onCreat方法中初始化）
    private List<LatLng>   points2;            //手动画线的经纬度点集合（在onCreat方法中初始化）
    private List<Polyline> polylineList;    //手动画线的画线对象集合（在onCreat方法中初始化）
    private List<Polygon>  polyGonList;        //手动画多边形的对象集合（在onCreat方法中初始化）（2017-7-18）
    private boolean        distanceMode = false;    //测距离模式
    private boolean        areaMode     = false;        //测面积模式
    private float          distance;
    private float          totalDistance;            //总距离
    private float          area;
    private float          totalArea;                //总面积
    //private ImageButton deletePoint;		//删除当前测量点

    private RelativeLayout     showDistance;    //测量模式界面
    private RelativeLayout     showTips;
    private TileProvider       tileProvider;        //瓦片提供者，用于转换地图图层
    private TileOverlayOptions tileOverlayOptions;
    private TileOverlay        tileOverlay;

    private ListView                           dialogList;
    private ArrayList<HashMap<String, Object>> dialogListItem;
    //private LinearLayout dialogLayout;
    private View                               view;
    private AlertDialog                        dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mappageLayout = inflater.inflate(R.layout.fragment_map, null);

        ShareToWeChat.registToWeChat(getContext().getApplicationContext());

        text_step = (TextView) mappageLayout.findViewById(R.id.tv_step);
        text_step.setVisibility(View.INVISIBLE);

        text_location = (TextView) mappageLayout.findViewById(R.id.tv_location);
        text_calculate = (TextView) mappageLayout.findViewById(R.id.tv_distance);
        layer = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_layer);
        toolbox = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_toolbox);
        returnToMap = (ImageButton) mappageLayout.findViewById(R.id.iv_return);

        showDistance = (RelativeLayout) mappageLayout.findViewById(R.id.calculate_distance);
        showTips = (RelativeLayout) mappageLayout.findViewById(R.id.calculate_tips);
        startTrail = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_starttrail);
        pauseTrail = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_pausetrail);
        endTrail = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_endtrail);
        takephoto = (ImageButton) mappageLayout.findViewById(R.id.imgbtn_takephoto);
        showDistance.setVisibility(View.INVISIBLE);
        showTips.setVisibility(View.INVISIBLE);
        startTrail.setVisibility(View.VISIBLE);
        pauseTrail.setVisibility(View.INVISIBLE);
        endTrail.setVisibility(View.INVISIBLE);

        layer.setOnClickListener(this);
        toolbox.setOnClickListener(this);
        startTrail.setOnClickListener(this);
        pauseTrail.setOnClickListener(this);
        endTrail.setOnClickListener(this);
        takephoto.setOnClickListener(this);
        returnToMap.setOnClickListener(this);


        mapView = (MapView) mappageLayout.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);           // 此方法必须重写

        points2 = new ArrayList<LatLng>();            //手动画线的经纬度点的集合初始化（2017-7-16）
        markList = new ArrayList<Marker>();            //手动画线的标注点的集合初始化
        polylineList = new ArrayList<Polyline>();    //手动画线的对象的集合初始化
        polyGonList = new ArrayList<Polygon>();        //手动画多边形的对象集合初始化

        helper = new TraceDBHelper(getContext());
        helper2 = new PointOfInterestDBHelper(getContext());//创建POI数据库
        // sp 初始化
        sp = getActivity().getSharedPreferences("config", MODE_PRIVATE);//私有参数
        initAmap();

        //initBDMap();
        if (Common.url != null && !Common.url.equals("")) {

        } else {
            Common.url = getResources().getString(R.string.url);
        }
        URL_ENDTRAIL = Common.url + "reqTraceNo.aspx";
        URL_CHECKUPDATE = Common.url + "request.aspx";
        URL_GETPOI = Common.url + "requestInfo.aspx";
//        int l = TabHost_Main.l;
       /* if (l == 0) {
            initPOI();//下载添加兴趣点下拉列表选项内容
        }
        if (l == 1) {
            initPOIEN();//英文版
        }
        if (l == 2) {
            initPOICZ();// 捷克语版
        }*/

        initPOI();          //下载添加兴趣点下拉列表选项内容 中文版

        return mappageLayout;
    }

    /**
     * 初始化AMap对象
     */
    private void initAmap() {
        //获取屏幕分辨率
        //SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        Common.winWidth = sp.getInt(LoginActivity.winWidth, 720);
        Common.winHeight = sp.getInt(LoginActivity.winHeight, 1280);
        Common.ppiScale = sp.getFloat(LoginActivity.PPISCALE, 1.5f);
        //创建相册文件夹
        Common.createFileDir();
        // 定义网络广播监听
        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectMgr = (ConnectivityManager) getActivity().getSystemService(context.CONNECTIVITY_SERVICE);

                NetworkInfo netInfo = connectMgr.getActiveNetworkInfo();
                if(netInfo!=null && netInfo.isConnected()){
                    //有网络连接
                    if(netInfo.getType()==ConnectivityManager.TYPE_WIFI){
                        //wifi连接
                        /**后期设置中加入“wifi状态下自动更新”选项，此处加入bool量判断是否自动检查
                         **/
                        Log.i("phonelog", "当前WiFi连接");
                        Common.isWiFiConnected=true;
                        if(Common.isUpdationg&&Common.fileInfo!=null){
                            Log.i("phonelog", "WiFi下继续下载");
                            // 通知Service继续下载
                            updateService = new Intent(getContext(), DownloadService.class);
                            updateService.setAction(DownloadService.ACTION_START);
                            updateService.putExtra("fileInfo", Common.fileInfo);
                            getActivity().startService(updateService);
                        }else{
                            if(Common.isAutoUpdate(getActivity().getApplicationContext())){
                                Log.i("phonelog", "wifi下检查更新");
                                String version = null;
                                if(Common.version == null ||Common.version.equals("")){
                                    version = Common.getAppVersionName(getActivity().getApplicationContext());
                                }else{
                                    version = Common.version;
                                }
                                if(version !=null && !version.equals("")){
                                    PostCheckVersion checkversion=new PostCheckVersion(updatehandler, URL_CHECKUPDATE,
                                            Common.getDeviceId(getApplicationContext()),version);
                                    checkversion.start();
                                }
                            }
                            else{
                                Log.i("phonelog", "自动检查更新关闭");
                            }
                        }


                    }
                    else if(netInfo.getType()==ConnectivityManager.TYPE_MOBILE){
                        // connect network,读取本地sharedPreferences文件，上传之前未完成上传的部分
                        Log.i("phonelog", "当前GPRS数据连接");
                        Common.isWiFiConnected=false;
                        Log.i("phonelog", "WiFi连接断开");


                        if (Common.isUpdationg&&Common.fileInfo!=null) {
                            // 通知Service暂停下载
                            updateService = new Intent(MainActivity.this, DownloadService.class);
                            updateService.setAction(DownloadService.ACTION_STOP);
                            updateService.putExtra("fileInfo", Common.fileInfo);
                            startService(updateService);
                            Log.i("phonelog", "发送暂停命令");
                        }
                    }else{
                        Common.isWiFiConnected=false;
                        Log.i("phonelog", "WiFi连接断开");
                    }
                }else{
                    //无网络连接
                    Log.i("phonelog", "Main，当前无网络");
                    Common.isWiFiConnected=false;

                    if(Common.isUpdationg&&Common.fileInfo!=null){
                        // 通知Service暂停下载
                        updateService = new Intent(MainActivity.this, DownloadService.class);
                        updateService.setAction(DownloadService.ACTION_STOP);
                        updateService.putExtra("fileInfo", Common.fileInfo);
                        startService(updateService);
                        Log.i("phonelog", "发送暂停命令");
                    }
                    if(!Common.checkGPS(getApplicationContext())){
                        //网络没开，gps没开，无法定位，提示
                        if(!isShowNonLocDlg){
                            isShowNonLocDlg=true;
                            boolean isShowBadLoc = sp.getBoolean("isShowBadLoc", true);
                            if(isShowBadLoc){
                                Log.i("phonelog", "网络没开，gps没开，无法定位，提示");

                                showDlg_badloc();
                            }
                        }
                    }
                }

            }
        };

        //注册监听网络连接状态广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        MainActivity.this.registerReceiver(connectionReceiver, intentFilter);
        uploadCache = getSharedPreferences("uploadCache", Activity.MODE_PRIVATE);


        if (aMap == null) {
            aMap = mapView.getMap();
            geocoderSearch = new GeocodeSearch(this);
            geocoderSearch.setOnGeocodeSearchListener(this);
            registerListener();
            setUpMap();
            if(!Common.isVisiter()){
                setUpService();
            }
        }
        //		Spinner spinner = (Spinner) findViewById(R.id.layers_spinner);
        //		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        //				this, R.array.layers_array,
        //				android.R.layout.simple_spinner_item);
        //		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //		spinner.setAdapter(adapter);
        //		spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }
}
