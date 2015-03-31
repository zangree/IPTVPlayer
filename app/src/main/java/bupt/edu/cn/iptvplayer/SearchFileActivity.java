package bupt.edu.cn.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bupt.edu.cn.utils.Global;


public class SearchFileActivity extends Activity {
    private static final String[] ACCEPTED_SUFFIX = {".mp4", ".mkv", ".flv", ".mov", ".ts", ".rmvb", ".rm"};

    private ListView listView;
    private TextView textView;
    private File currentParent;
    private File[] currentFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_file);

        listView = (ListView) findViewById(R.id.fileList);
        textView = (TextView) findViewById(R.id.filePath);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File root = Environment.getExternalStorageDirectory();
            currentParent = root;
            currentFiles = root.listFiles(new FileFilterImpl());
            inflateListView(currentFiles);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentFiles[position].isFile()) {
                    Intent intent = new Intent(SearchFileActivity.this, IPTVPlayerActivity.class);
                    intent.putExtra(Global.MEDIA, Global.LOCAL_VIDEO);
                    try {
                        TextView f = (TextView) view.findViewById(R.id.file_name);
                        String filepath = currentParent.getCanonicalPath() + "/" + f.getText();
                        intent.putExtra(Global.EXTRA_LINK, filepath);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    File[] tmp = currentFiles[position].listFiles(new FileFilterImpl());
                    currentParent = currentFiles[position];
                    currentFiles = tmp;
                    inflateListView(currentFiles);
                }

            }
        });
        Button parent = (Button) findViewById(R.id.parent);
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View source) {
                try {
                    if (!currentParent.getCanonicalPath().equals("/mnt/sdcard")) {
                        currentParent = currentParent.getParentFile();
                        currentFiles = currentParent.listFiles();
                        inflateListView(currentFiles);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void inflateListView(File[] files)
    {
        List<Map<String, Object>> listItems = new ArrayList<>();
        for (File file : files) {
            Map<String, Object> listItem = new HashMap<>();
            if (file.isDirectory()) {
                listItem.put("icon", R.drawable.folder);
            } else {
                listItem.put("icon", R.drawable.file);
            }
            listItem.put("fileName", file.getName());
            listItems.add(listItem);
        }

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItems, R.layout.view_iptv_line
                , new String[]{"icon", "fileName"}, new int[]{R.id.icon, R.id.file_name});
        listView.setAdapter(simpleAdapter);
        try {
            textView.setText("Current Path：" + currentParent.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_file, menu);
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

    private class FileFilterImpl implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return true;
            }
            for (String accepted : ACCEPTED_SUFFIX) {
                if (pathname.getName().endsWith(accepted)) {
                    return true;
                }
            }
            return false;
        }
    }
}
