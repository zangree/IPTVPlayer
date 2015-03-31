package bupt.edu.cn.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bupt.edu.cn.net.HttpParser;
import bupt.edu.cn.utils.Global;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();
    private ListView tvListView;
    Map<String, String> iptvLists;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x123) {
                List<String> listTitles = new ArrayList<>(iptvLists.keySet());
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(MainActivity.this, R.layout.array_item, listTitles);
                tvListView.setAdapter(adapter1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpParser httpParser = new HttpParser("http://tv.byr.cn");
                iptvLists = httpParser.parseByrTv();
                handler.sendEmptyMessage(0x123);
            }
        }).start();

        tvListView = (ListView) findViewById(R.id.tvListView);
        tvListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, IPTVPlayerActivity.class);
                intent.putExtra(Global.MEDIA, Global.STREAM_VIDEO);
                String channels = ((TextView) view).getText().toString();
                intent.putExtra(Global.EXTRA_LINK, iptvLists.get(channels));
                startActivity(intent);
            }
        });

    }

    public void refresh(View v){
        Log.v(TAG, "show called");

    }

    public void searchFile(View v){
        Intent intent = new Intent(MainActivity.this, SearchFileActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_iptvplayer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
