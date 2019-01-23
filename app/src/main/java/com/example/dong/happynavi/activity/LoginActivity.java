package com.example.dong.happynavi.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dong.happynavi.R;
import com.example.dong.happynavi.helper.Common;
import com.example.dong.happynavi.helper.MD5Util;
import com.example.dong.happynavi.helper.ToastUtil;
import com.example.dong.happynavi.httpconnection.PostLoginData;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    public boolean isRemember;

    public EditText et_uid;
    public EditText et_pwd;
    public CheckBox cb_remember_pwd;
    public CheckBox cb_agree_protocol;
    public TextView tv_appVersion;
    public TextView tv_protocol;
    public Button   btn_register;
    public Button   btn_login;
    public Button   btn_visit_login;

    //暂时保存用户名和密码
    public String s_uid;
    public String s_pwd;
    public String s_pwd_md5;

    public String login_url = null;
    public String LoginData;

    private SharedPreferences mSharedPreferences;   // android系统下用于数据存贮的一个方便的API

    /**
     * 提交登录信息返回的message
     */
    @SuppressLint("HandlerLeak")
    private Handler handler_login = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:     //登录成功
                    SharedPreferences.Editor editor = mSharedPreferences.edit();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);// 登陆成功跳转
                    startActivity(intent);
                    break;
                case 1:
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.tips_postfail), 0).show();

                    break;
                case 10:
                    ToastUtil.show(LoginActivity.this, getResources().getString(R.string.tips_netdisconnect));
                    break;

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
        initData();
    }

    private void initView() {
        et_uid = (EditText) findViewById(R.id.et_userid);
        et_pwd = (EditText) findViewById(R.id.et_password);
        cb_remember_pwd = (CheckBox) findViewById(R.id.remberidpwd);
        cb_agree_protocol = (CheckBox) findViewById(R.id.agree);
        tv_appVersion = (TextView) findViewById(R.id.app_version);
        tv_protocol = (TextView) findViewById(R.id.protocal);
        tv_protocol.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);     //添加下划线
        btn_register = (Button) findViewById(R.id.btn_register);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_visit_login = (Button) findViewById(R.id.btn_visiter_login);

        et_uid.setOnClickListener(this);
        et_pwd.setOnClickListener(this);
        cb_remember_pwd.setOnClickListener(this);
        cb_agree_protocol.setOnClickListener(this);
        tv_appVersion.setOnClickListener(this);
        tv_protocol.setOnClickListener(this);
        btn_register.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        btn_visit_login.setOnClickListener(this);
    }

    private void initData() {
        // SharedPreferences 初始化
        mSharedPreferences = getSharedPreferences("config", MODE_PRIVATE);// 私有参数
        isRemember = mSharedPreferences.getBoolean("remberidpwd", false);
        if (isRemember) {
            //把账号密码都设置到文本框中
            String saveuid = mSharedPreferences.getString("uid", "");
            String savepwd = mSharedPreferences.getString("pwd", "");

            et_uid.setText(saveuid);
            et_pwd.setText(savepwd);
            cb_remember_pwd.setChecked(true);
        }


        if (Common.version == null || Common.version.equals("")) {
            tv_appVersion.setText("V" + Common.getAppVersionName(getApplicationContext()));
        } else {
            tv_appVersion.setText("V" + Common.version);
        }

        if (Common.url == null || !Common.url.equals("")) {
            Common.url = getResources().getString(R.string.url);
        }
        login_url = Common.url + "";
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                onLogin();
                break;
            case R.id.btn_register:
                break;
            case R.id.protocal:
                //显示protocol信息
                AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.tips_dlgtle_protocol));
                alertDialog.setMessage(getResources().getString(R.string.tips_dlgmsg_protocol1) + "\n"
                        + getResources().getString(R.string.tips_dlgmsg_protocol2) + "\n"
                        + getResources().getString(R.string.tips_dlgmsg_protocol3) + "\n"
                        + getResources().getString(R.string.tips_dlgmsg_protocol4) + "\n"
                        + getResources().getString(R.string.tips_dlgmsg_protocol5));
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.close),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                alertDialog.show();

        }
    }

    /**
     * 登录
     */
    private void onLogin() {
        s_uid = et_uid.getText().toString();
        s_pwd = et_pwd.getText().toString();
        s_pwd_md5 = MD5Util.string2MD5(s_pwd);     //使用MD5加密密码
        if (s_uid.equals("") || s_pwd.equals("")) {     // 判断账号密码不能为空
            Toast.makeText(this, getResources().getString(R.string.idpwdcannotnull), Toast.LENGTH_LONG).show();
        } else {
            // 勾选 记录用户名和密码
            // 获取到一个参数文件编辑器
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            if (cb_remember_pwd.isChecked()) {
                editor.putString("uid", s_uid);
                editor.putString("pwd", s_pwd);
                editor.putBoolean("remberidpwd", true);
                editor.commit();
            }
            //检查是否同意protocol
            if (cb_agree_protocol.isChecked()) {
                // 判断是否进行MD5
                // 若版本号小于"1.2.5"则不加密
                boolean encryption = (Common.version.compareTo("1.2.5") >= 0);
                if (encryption) {
                    LoginData = s_uid + "!" + s_pwd_md5;
                } else {
                    LoginData = s_uid + "!" + s_pwd;
                }


                Toast.makeText(this, getResources().getString(R.string.tips_dlgmsg_login), Toast.LENGTH_LONG).show();
                PostLoginData pld = new PostLoginData(handler_login, login_url, LoginData,
                        Common.getDeviceId(getApplicationContext()));                     //登录api
                Log.i("LoginMsg", LoginData);
                Log.i("LoginMsg", "onLogin: " + Common.getDeviceId(getApplicationContext()));
                pld.start();    //开启线程
            } else {
                Toast.makeText(this, getResources().getString(R.string.tips_agreeprotocol), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
