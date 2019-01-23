package com.example.dong.happynavi.httpconnection;

import android.os.Handler;
import android.os.Message;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 提交登录信息
 */
public class PostLoginData extends Thread {

    private Handler handler_login;
    private String login_url, loginData, deviceId;
    private String uid, pwd;
    private HttpPost            httpRequest;
    private List<NameValuePair> mNameValuePairs = new ArrayList<NameValuePair>();


    public PostLoginData(Handler handler_login, String login_url, String loginData, String deviceId) {
        this.handler_login = handler_login;
        this.login_url = login_url;
        this.loginData = loginData;
        this.deviceId = deviceId;
    }

    @Override
    public void run() {
        //把登录信息分开
        String uid_pwd[] = loginData.split("!");
        mNameValuePairs.add(new BasicNameValuePair("userID", uid_pwd[0]));
        mNameValuePairs.add(new BasicNameValuePair("password", uid_pwd[1]));
        mNameValuePairs.add(new BasicNameValuePair("deviceId", deviceId));

        uid = uid_pwd[0];
        pwd = uid_pwd[1];

        Post();
    }

    private void Post() {

        //获取Message对象的时候是不能用 "new Message" 的方式来获取，而必须使用 Obtain()的方式来获取Message对象
        //只有在spool = null 的情况下才会new出一个Message(),返回一个Message对象,如果在不为空的情况下,
        // Message的对象都是从Message对象池里面拿的实例从而重复使用的
        final Message message = Message.obtain();
        OkHttpClient client = new OkHttpClient();
        //构建FormBody，传入要提交的参数
        RequestBody requestBody = new FormBody
                .Builder()
                .add("username", uid)
                .add("password", pwd)
                .build();
        final Request request = new Request.Builder()
                .url(login_url)
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                message.what = 10;
                message.obj = "提交失败!";
                handler_login.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.message();
                boolean responseSuc = response.isSuccessful();
                String tempResponse =  response.body().string();

                if (responseSuc) {
                    message.what = 0;
                } else {
                    message.what = 1;
                }
                handler_login.sendMessage(message);
            }
        });
    }

}
