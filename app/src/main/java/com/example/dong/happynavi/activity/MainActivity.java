package com.example.dong.happynavi.activity;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.dong.happynavi.R;
import com.example.dong.happynavi.fragment.MapFragment;
import com.example.dong.happynavi.helper.ShareToWeChat;
import com.next.easynavigation.view.EasyNavigationBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EasyNavigationBar mNavigationBar;

    private String[] tabText = {"地图", "轨迹", "问卷", "我"};
    //未选中icon
    private int[] normalIcon=new int[]{R.drawable.map_dark,R.drawable.history_dark,R.drawable.document_dark,R.drawable.user_dark};
    //选中时icon
    private int[] selectIcon=new int[]{R.drawable.map_light,R.drawable.history_light,R.drawable.doc_light,R.drawable.user_light};

    private List<Fragment> fragments = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ShareToWeChat.registToWeChat(getApplicationContext());

        mNavigationBar = findViewById(R.id.navigationBar);

        fragments.add(new MapFragment());
        fragments.add(new MapFragment());
        fragments.add(new MapFragment());
        fragments.add(new MapFragment());

        mNavigationBar.titleItems(tabText)
                .normalIconItems(normalIcon)
                .selectIconItems(selectIcon)
                .fragmentList(fragments)
                .fragmentManager(getSupportFragmentManager())
                .canScroll(true)
                .build();
    }

    public EasyNavigationBar getNavigationBar() {
        return mNavigationBar;
    }
}
